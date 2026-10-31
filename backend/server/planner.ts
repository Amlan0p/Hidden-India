/**
 * Grounded trip planner.
 *
 * Decides WHAT goes on WHICH day using only the verified destination data:
 *   1. Scores every place / food / experience against the traveller's interests,
 *      budget tier and travel style.
 *   2. Picks the best-scoring items that fit the number of days.
 *   3. Groups places into days with a nearest-neighbour route so each day stays
 *      geographically compact (real haversine distances, not guesses).
 *   4. Orders each day (morning -> lunch -> afternoon -> evening) and computes real
 *      distances between consecutive stops.
 *
 * The LLM (Groq/Gemini) never chooses or reorders stops. It only writes the wording
 * (summary, day titles, activity names, descriptions) for this skeleton.
 */

export interface Coord { lat: number; lng: number }

const DEFAULT_BASE_COST: Record<string, number> = { Budget: 1800, Moderate: 3500, Luxury: 6000 };

// ---------------------------------------------------------------------------
// Geometry helpers
// ---------------------------------------------------------------------------
export function haversineKm(a: Coord, b: Coord): number {
  const toRad = (x: number) => (x * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 12742 * Math.asin(Math.min(1, Math.sqrt(h)));
}

export function formatDistance(km: number, from?: string): string {
  const suffix = from ? ` ${from}` : '';
  if (km < 0.15) return 'Right next door';
  if (km < 1) return `~${Math.round(km * 10) * 100} m walk${suffix}`;
  if (km <= 2) return `${km.toFixed(1)} km (short walk or auto)${suffix}`;
  if (km <= 60) return `${km.toFixed(1)} km by road or trail${suffix}`;
  return `${Math.round(km)} km by road${suffix}`;
}

function validCoord(c: any): c is Coord {
  return !!c && typeof c.lat === 'number' && typeof c.lng === 'number' && !(c.lat === 0 && c.lng === 0);
}

// ---------------------------------------------------------------------------
// Interest model (matches INTEREST_OPTIONS in the Android app)
// ---------------------------------------------------------------------------
interface InterestRule {
  categories: string[];       // place / experience category
  markers: string[];          // place markerCategory
  keywords: RegExp;           // matched against name + description
  food?: boolean;
}

const INTEREST_RULES: Record<string, InterestRule> = {
  Heritage: { categories: ['Heritage'], markers: [], keywords: /\b(fort|palace|temple|museum|ruins?|monument|cave|stupa|tomb|haveli|mahal|memorial|jail|ancient)\b/i },
  Culture: { categories: ['Culture', 'Cultural Immersion'], markers: ['Cultural Site', 'Local Market'], keywords: /\b(village|festival|tribal|folk|market|bazaar|dance|music|museum|monastery)\b/i },
  Food: { categories: ['Food'], markers: ['Local Market'], keywords: /\b(food|market|bazaar|street|kitchen|tasting)\b/i, food: true },
  Photography: { categories: [], markers: ['Viewpoint'], keywords: /\b(view|viewpoint|sunrise|sunset|scenic|falls?|waterfall|lake|peak|valley|panorama|backwaters?)\b/i },
  Stepwells: { categories: [], markers: [], keywords: /\b(stepwell|baori|baoli|vav|kund|talao|jhalra|tank|rani ki vav)\b/i },
  'Art & Craft': { categories: ['Cultural Immersion'], markers: [], keywords: /\b(craft|workshop|weav\w*|artisan|pottery|painting|handloom|handicraft|loom|kalamkari|puppet|carving|textile|terracotta)\b/i },
  'Nature & Trails': { categories: ['Nature', 'Wildlife', 'Mountains', 'Adventure', 'Beach', 'Beaches'], markers: [], keywords: /\b(trek|trail|waterfall|valley|lake|forest|park|sanctuary|safari|beach|hill|peak|gorge|island|river)\b/i },
  Spiritual: { categories: ['Spiritual'], markers: [], keywords: /\b(temple|shrine|monastery|gurudwara|gurdwara|ghat|church|mosque|dargah|ashram|gompa|darshan|pilgrim)\b/i },
};

export function parseCostMax(s: any): number {
  if (!s || typeof s !== 'string') return 0;
  if (/free/i.test(s) && !/\d/.test(s)) return 0;
  const nums = (s.replace(/,/g, '').match(/\d+(\.\d+)?/g) || []).map(Number);
  return nums.length ? Math.max(...nums) : 0;
}

interface Candidate {
  kind: 'place' | 'food' | 'experience';
  raw: any;
  id: string;
  name: string;
  category: string;
  coords: Coord;
  score: number;
  reasons: string[];
  isGem: boolean;
  timeHint: 'morning' | 'midday' | 'sunset';
}

function timeHintOf(bestTime: string | undefined): 'morning' | 'midday' | 'sunset' {
  const t = (bestTime || '').toLowerCase();
  if (/sunset|evening/.test(t)) return 'sunset';
  const m = t.match(/(\d{1,2})(?::\d{2})?\s*(am|pm)/);
  if (m) {
    let h = parseInt(m[1], 10) % 12;
    if (m[2] === 'pm') h += 12;
    if (h >= 16) return 'sunset';
    if (h <= 9) return 'morning';
  }
  return 'midday';
}

function scoreItem(
  kind: Candidate['kind'], name: string, desc: string, category: string, marker: string,
  isGem: boolean, costMax: number, interests: string[], budget: string, travelStyle: string
): { score: number; reasons: string[] } {
  let score = 1;
  const reasons: string[] = [];
  const text = `${name} ${desc}`;
  for (const interest of interests) {
    const rule = INTEREST_RULES[interest];
    if (!rule) continue;
    let hit = 0;
    if (rule.categories.includes(category)) hit += 3;
    if (marker && rule.markers.includes(marker)) hit += 2;
    if (rule.keywords.test(text)) hit += 2;
    if (kind === 'food' && rule.food) hit += 3;
    if (hit > 0) {
      score += Math.min(hit, 5);
      reasons.push(interest);
    }
  }
  const offbeat = /immersive|offbeat|authentic|hidden/i.test(travelStyle);
  if (isGem) score += offbeat ? 1.5 : 0.5;
  if (marker === 'Key Place' && !offbeat) score += 1;
  if (budget === 'Budget') {
    if (costMax > 500) score -= 1.5;
    else if (costMax === 0 || costMax <= 100) score += 0.5;
  } else if (budget === 'Luxury') {
    if (costMax >= 300) score += 0.5;
  }
  return { score, reasons };
}

// ---------------------------------------------------------------------------
// Route building
// ---------------------------------------------------------------------------
function nearestNeighbourOrder<T extends { coords: Coord }>(items: T[], start: T): T[] {
  const rest = items.filter((i) => i !== start);
  const route = [start];
  let cur = start;
  while (rest.length) {
    let bi = 0, bd = Infinity;
    rest.forEach((r, i) => {
      const d = haversineKm(cur.coords, r.coords);
      if (d < bd) { bd = d; bi = i; }
    });
    cur = rest.splice(bi, 1)[0];
    route.push(cur);
  }
  return route;
}

function centroid(items: { coords: Coord }[], fallback: Coord): Coord {
  if (!items.length) return fallback;
  return {
    lat: items.reduce((s, i) => s + i.coords.lat, 0) / items.length,
    lng: items.reduce((s, i) => s + i.coords.lng, 0) / items.length,
  };
}

function routeLength(items: { coords: Coord }[]): number {
  let t = 0;
  for (let i = 1; i < items.length; i++) t += haversineKm(items[i - 1].coords, items[i].coords);
  return t;
}

const THEME_BY_CATEGORY: Record<string, string> = {
  Heritage: 'Heritage & History',
  Spiritual: 'Sacred Sites & Rituals',
  Nature: 'Nature & Scenic Escapes',
  Wildlife: 'Wildlife & Wilderness',
  Mountains: 'Mountain Trails',
  Adventure: 'Adventure Day',
  Beach: 'Coast & Beaches',
  Beaches: 'Coast & Beaches',
  Culture: 'Culture & Local Life',
  'Cultural Immersion': 'Artisans & Local Life',
  Offbeat: 'Offbeat Discoveries',
  Food: 'Flavours of the Region',
};

function activityFor(c: Candidate): string {
  if (c.kind === 'food') return `Local tasting: ${c.raw.name}`;
  if (c.kind === 'experience') return `Hands-on with ${c.raw.communityHost || 'local hosts'}`;
  if (c.isGem) return 'Hidden gem discovery';
  switch (c.category) {
    case 'Heritage': return 'Heritage walk';
    case 'Spiritual': return 'Visit and quiet reflection';
    case 'Nature': case 'Mountains': return 'Scenic exploration';
    case 'Wildlife': return 'Wildlife and nature watch';
    case 'Beach': case 'Beaches': return 'Coastal time';
    case 'Culture': return 'Cultural exploration';
    case 'Adventure': return 'Active adventure';
    default: return 'Explore';
  }
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------
export interface PlannerOptions {
  days?: number;
  budgetStyle?: string;
  interests?: string[];
  travelStyle?: string;
}

export function maxFeasibleDaysFor(dest: any): number {
  const P = dest.places?.length || 0;
  const F = dest.localFoods?.length || 0;
  const E = dest.localExperiences?.length || 0;
  // Largest D where every day can still get >= 3 distinct stops
  // (at most 1 food and 1 experience per day; the rest must be places).
  let best = 1;
  for (let d = 1; d <= 14; d++) {
    if (P + Math.min(F, d) + Math.min(E, d) >= 3 * d) best = d;
  }
  return best;
}

export function buildGroundedPlan(dest: any, opts: PlannerOptions = {}) {
  const budgetStyle = opts.budgetStyle || 'Moderate';
  const interests = (opts.interests || []).filter(Boolean);
  const travelStyle = opts.travelStyle || 'Authentic & Immersive';
  const requestedDays = Math.max(1, Math.min(opts.days || 3, 14));
  const totalDays = Math.min(requestedDays, maxFeasibleDaysFor(dest));
  const wasCapped = totalDays < requestedDays;
  const centre: Coord = validCoord(dest.coordinates) ? dest.coordinates : { lat: 22.5, lng: 80 };

  const places: Candidate[] = (dest.places || []).map((p: any) => {
    const { score, reasons } = scoreItem('place', p.name, p.description || '', p.category || '', p.markerCategory || '',
      !!p.isSecretGem, parseCostMax(p.estimatedCost), interests, budgetStyle, travelStyle);
    return { kind: 'place', raw: p, id: p.id, name: p.name, category: p.category || 'Heritage',
      coords: validCoord(p.coordinates) ? p.coordinates : centre, score, reasons, isGem: !!p.isSecretGem,
      timeHint: timeHintOf(p.bestTimeOfDay) } as Candidate;
  });
  const foods: Candidate[] = (dest.localFoods || []).map((f: any) => {
    const { score, reasons } = scoreItem('food', f.name, f.description || '', 'Food', '', false,
      parseCostMax(f.priceRange), interests, budgetStyle, travelStyle);
    return { kind: 'food', raw: f, id: f.id, name: f.name, category: 'Food',
      coords: validCoord(f.coordinates) ? f.coordinates : centre, score, reasons, isGem: false, timeHint: 'midday' } as Candidate;
  });
  const exps: Candidate[] = (dest.localExperiences || []).map((e: any) => {
    const { score, reasons } = scoreItem('experience', e.title, e.description || '', e.category || '', '', false,
      parseCostMax(e.cost), interests, budgetStyle, travelStyle);
    return { kind: 'experience', raw: e, id: e.id, name: e.title, category: e.category || 'Cultural Immersion',
      coords: validCoord(e.coordinates) ? e.coordinates : centre, score, reasons, isGem: false, timeHint: 'midday' } as Candidate;
  });

  const byScore = (a: Candidate, b: Candidate) => b.score - a.score;
  places.sort(byScore); foods.sort(byScore); exps.sort(byScore);

  // --- 1. How many of each kind do we use? -------------------------------
  const foodsPerDay = interests.includes('Food') && foods.length >= totalDays * 2 ? 2 : 1;
  const foodCount = Math.min(foods.length, totalDays * foodsPerDay);
  const expCount = Math.min(exps.length, totalDays);

  // food slots per day (first pass 1 each, optional second pass)
  const foodSlots: number[] = Array(totalDays).fill(0);
  for (let left = foodCount, pass = 0; left > 0 && pass < foodsPerDay; pass++) {
    for (let d = 0; d < totalDays && left > 0; d++) { foodSlots[d]++; left--; }
  }
  // experiences go on the first expCount days (they are re-matched by proximity below)
  const expDays = new Set<number>(Array.from({ length: expCount }, (_, i) => i));

  // places wanted per day so the day totals 4 stops; minimum so it totals >= 3
  const want = (d: number) => Math.max(1, 4 - foodSlots[d] - (expDays.has(d) ? 1 : 0));
  const minimum = (d: number) => Math.max(1, 3 - foodSlots[d] - (expDays.has(d) ? 1 : 0));
  let totalWant = 0;
  for (let d = 0; d < totalDays; d++) totalWant += want(d);
  const placeCount = Math.min(places.length, totalWant);

  const chosenPlaces = places.slice(0, placeCount);
  const chosenFoods = foods.slice(0, foodCount);
  const chosenExps = exps.slice(0, expCount);

  // per-day place quota: start at the minimum, then top up towards the wanted count
  const quota: number[] = [];
  for (let d = 0; d < totalDays; d++) quota.push(minimum(d));
  let remaining = placeCount - quota.reduce((a, b) => a + b, 0);
  for (let d = 0; remaining > 0 && d < totalDays; d = (d + 1) % totalDays) {
    if (quota.every((q, i) => q >= want(i))) break;
    if (quota[d] < want(d)) { quota[d]++; remaining--; }
  }
  if (remaining < 0) {            // not enough places for the minimum: shave from the last days
    for (let d = totalDays - 1; remaining < 0 && d >= 0; d--) {
      while (quota[d] > 1 && remaining < 0) { quota[d]--; remaining++; }
    }
  }

  // --- 2. Group places into geographically compact days ------------------
  const start = chosenPlaces[0];                        // best-scoring place anchors Day 1
  const tour = start ? nearestNeighbourOrder(chosenPlaces, start) : [];
  const dayPlaces: Candidate[][] = Array.from({ length: totalDays }, () => []);
  {
    let idx = 0;
    for (let d = 0; d < totalDays; d++) {
      const n = d === totalDays - 1 ? tour.length - idx : Math.min(quota[d], tour.length - idx);
      dayPlaces[d] = tour.slice(idx, idx + n);
      idx += n;
    }
  }

  // --- 3. Attach one experience per experience-day (nearest day) ---------
  const dayExp: (Candidate | undefined)[] = Array(totalDays).fill(undefined);
  for (const e of chosenExps) {
    let best = -1, bd = Infinity;
    for (const d of expDays) {
      if (dayExp[d]) continue;
      const dist = haversineKm(e.coords, centroid(dayPlaces[d], centre));
      if (dist < bd) { bd = dist; best = d; }
    }
    if (best >= 0) dayExp[best] = e;
  }

  // --- 4. Attach foods (nearest to each day's centroid) ------------------
  const dayFoods: Candidate[][] = Array.from({ length: totalDays }, () => []);
  const foodPool = [...chosenFoods];
  for (let pass = 0; pass < foodsPerDay; pass++) {
    for (let d = 0; d < totalDays; d++) {
      if (!foodPool.length || foodSlots[d] <= pass) continue;
      const c = centroid([...dayPlaces[d], ...(dayExp[d] ? [dayExp[d]!] : [])], centre);
      let bi = 0, bd = Infinity;
      foodPool.forEach((f, i) => { const dist = haversineKm(f.coords, c); if (dist < bd) { bd = dist; bi = i; } });
      dayFoods[d].push(foodPool.splice(bi, 1)[0]);
    }
  }

  // --- 5. Order each day and compute real distances ----------------------
  const dayPlans: any[] = [];
  for (let d = 0; d < totalDays; d++) {
    let ps = dayPlaces[d];
    if (ps.length > 1) {
      // nearest-neighbour from the place best suited to the morning
      const morning = ps.find((p) => p.timeHint === 'morning') || ps[0];
      ps = nearestNeighbourOrder(ps, morning);
      // a sunset/evening place goes last
      const sunsetIdx = ps.findIndex((p) => p.timeHint === 'sunset');
      if (sunsetIdx > -1 && sunsetIdx !== ps.length - 1) ps.push(ps.splice(sunsetIdx, 1)[0]);
    }
    const exp = dayExp[d];
    const lunch = dayFoods[d][0];
    const snack = dayFoods[d][1];

    const seq: { c: Candidate; time: string; duration: string }[] = [];
    if (exp) {
      // P1 morning, lunch, experience, P2 evening
      if (ps[0]) seq.push({ c: ps[0], time: '09:30 AM', duration: '2 hours' });
      if (lunch) seq.push({ c: lunch, time: '01:00 PM', duration: '1 hour' });
      seq.push({ c: exp, time: '02:30 PM', duration: exp.raw.duration || '2 hours' });
      if (ps[1]) seq.push({ c: ps[1], time: ps[1].timeHint === 'sunset' ? '05:00 PM' : '04:45 PM', duration: '1.5 hours' });
      for (const extra of ps.slice(2)) seq.push({ c: extra, time: '06:00 PM', duration: '1 hour' });
    } else {
      // P1, P2 morning, lunch, P3 afternoon
      if (ps[0]) seq.push({ c: ps[0], time: '09:30 AM', duration: '2 hours' });
      if (ps[1]) seq.push({ c: ps[1], time: '11:45 AM', duration: '1.5 hours' });
      if (lunch) seq.push({ c: lunch, time: '01:15 PM', duration: '1 hour' });
      if (ps[2]) seq.push({ c: ps[2], time: ps[2].timeHint === 'sunset' ? '05:00 PM' : '03:30 PM', duration: '2 hours' });
      for (const extra of ps.slice(3)) seq.push({ c: extra, time: '05:30 PM', duration: '1 hour' });
    }
    if (snack) seq.push({ c: snack, time: '06:45 PM', duration: '45 minutes' });

    let prev: Coord | null = null;
    const items = seq.map((s, i) => {
      const c = s.c;
      const dist: number = prev ? haversineKm(prev, c.coords) : haversineKm(centre, c.coords);
      const travelDistance = i === 0
        ? formatDistance(dist, 'from town centre')
        : formatDistance(dist, 'from previous stop');
      prev = c.coords;
      const r = c.raw;
      const reasonText = c.reasons.length ? `Matches your interest in ${c.reasons.slice(0, 2).join(' & ')}.` : '';
      return {
        id: `${dest.id}-d${d + 1}-${i + 1}`,
        sourceId: c.id,
        time: s.time,
        placeName: c.name,
        activity: activityFor(c),
        description: c.kind === 'place'
          ? `${r.description}${c.isGem && r.insiderTip ? ` Insider tip: ${r.insiderTip}` : ''}`
          : r.description,
        estimatedCost: c.kind === 'place' ? r.estimatedCost : c.kind === 'food' ? r.priceRange : r.cost,
        duration: s.duration,
        travelDistance,
        category: c.kind === 'food' ? 'Food' : c.kind === 'experience' ? 'Art' : c.category,
        coordinates: c.coords,
        image: c.kind === 'experience' ? undefined : r.image || undefined,
        reason: reasonText || undefined,
        kind: c.kind,
      };
    });

    // theme from what the day actually contains
    const counts: Record<string, number> = {};
    seq.forEach((s) => { if (s.c.kind !== 'food') counts[s.c.category] = (counts[s.c.category] || 0) + 1; });
    const top = Object.entries(counts).sort((a, b) => b[1] - a[1]).map(([k]) => THEME_BY_CATEGORY[k] || k);
    const theme = top.length ? Array.from(new Set(top)).slice(0, 2).join(' + ') : 'Local Flavours';
    const namedPlaces = items.filter((i) => i.kind === 'place').map((i) => i.placeName);
    const farthest = items.reduce((m, i) => Math.max(m, haversineKm(centre, i.coordinates)), 0);
    const title = `Day ${d + 1}: ${namedPlaces.slice(0, 2).join(' & ') || dest.name}${farthest > 25 ? ' (day trip)' : ''}`;
    dayPlans.push({
      dayNumber: d + 1,
      title,
      theme,
      totalDistanceKm: Math.round(routeLength(items.map((i) => ({ coords: i.coordinates }))) * 10) / 10,
      items,
    });
  }

  const baseCost = DEFAULT_BASE_COST[budgetStyle] ?? DEFAULT_BASE_COST.Moderate;
  const interestText = interests.length ? ` tuned to ${interests.join(', ')}` : '';
  return {
    id: `itin_${dest.id}_${Date.now()}`,
    destinationId: dest.id,
    destinationName: dest.name,
    state: dest.state,
    days: totalDays,
    requestedDays,
    wasCapped,
    capNote: wasCapped
      ? `This destination currently has verified content for about ${totalDays} day(s) at a comfortable pace, so the itinerary was scaled down from the requested ${requestedDays} days rather than repeating places or inventing new ones.`
      : undefined,
    budgetStyle,
    interests,
    travelStyle,
    totalEstimatedCost: `₹${(baseCost * totalDays).toLocaleString('en-IN')}`,
    curatedDate: 'Verified Local Archive',
    summary: `A ${totalDays}-day route through ${dest.name}${interestText}. Stops on each day are grouped by distance so you spend less time in transit.`,
    dayPlans,
    provider: 'Grounded Local Planner',
  };
}

/**
 * Merge LLM-written wording into a grounded plan. The LLM may only change
 * summary / titles / themes / activity / description per stop id; it can never
 * add, remove or reorder stops. Anything missing or invalid keeps the template text.
 */
export function mergeLlmWording(plan: any, llm: any): any {
  if (!llm || !Array.isArray(llm.dayPlans)) return plan;
  const byDay = new Map<number, any>();
  llm.dayPlans.forEach((d: any) => { if (d && typeof d.dayNumber === 'number') byDay.set(d.dayNumber, d); });
  const clean = (s: any, max: number) => (typeof s === 'string' && s.trim() ? s.trim().slice(0, max) : undefined);
  const dayPlans = plan.dayPlans.map((dp: any) => {
    const ld = byDay.get(dp.dayNumber);
    if (!ld) return dp;
    const byItem = new Map<string, any>();
    (ld.items || []).forEach((it: any) => { if (it && typeof it.id === 'string') byItem.set(it.id, it); });
    return {
      ...dp,
      title: clean(ld.title, 90) || dp.title,
      theme: clean(ld.theme, 60) || dp.theme,
      items: dp.items.map((it: any) => {
        const li = byItem.get(it.id);
        if (!li) return it;
        return { ...it, activity: clean(li.activity, 70) || it.activity, description: clean(li.description, 320) || it.description };
      }),
    };
  });
  return {
    ...plan,
    summary: clean(llm.summary, 400) || plan.summary,
    totalEstimatedCost: clean(llm.totalEstimatedCost, 60) || plan.totalEstimatedCost,
    dayPlans,
  };
}

/** Compact skeleton sent to the LLM so it only writes wording. */
export function skeletonForPrompt(plan: any) {
  return plan.dayPlans.map((dp: any) => ({
    dayNumber: dp.dayNumber,
    items: dp.items.map((it: any) => ({
      id: it.id,
      time: it.time,
      placeName: it.placeName,
      kind: it.kind,
      category: it.category,
      travelDistance: it.travelDistance,
      factSheet: it.description,
      estimatedCost: it.estimatedCost,
    })),
  }));
}
