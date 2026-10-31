/**
 * Helpers for the Ask AI chat: keep replies as clean plain text for the Android app
 * (which does not render markdown) and ground "All India" questions in verified data.
 */

/** Turn any markdown the model emits into clean plain text with "• " bullets. */
export function cleanChatText(input: string): string {
  if (!input) return '';
  let t = String(input).replace(/\r/g, '');
  t = t.replace(/```[a-zA-Z]*\n?([\s\S]*?)```/g, '$1');         // code fences
  t = t.replace(/^\s{0,3}#{1,6}\s*/gm, '');                      // # headings
  t = t.replace(/!\[[^\]]*\]\([^)]*\)/g, '');                    // images
  t = t.replace(/\[([^\]]+)\]\((?:[^)]*)\)/g, '$1');             // [text](url) -> text
  t = t.replace(/\*\*\*([^*\n]+?)\*\*\*/g, '$1');                // ***bold italic***
  t = t.replace(/\*\*([^*\n]+?)\*\*/g, '$1');                    // **bold**
  t = t.replace(/__([^_\n]+?)__/g, '$1');                        // __bold__
  t = t.replace(/(^|[\s(\[])\*(?!\s)([^*\n]+?)\*(?=[\s).,;:!?\]]|$)/g, '$1$2'); // *italic*
  t = t.replace(/(^|[\s(\[])_(?!\s)([^_\n]+?)_(?=[\s).,;:!?\]]|$)/g, '$1$2');   // _italic_
  t = t.replace(/`([^`\n]+)`/g, '$1');                           // `code`
  t = t.replace(/^\s*[-*+•]\s+/gm, '• ');                        // list markers -> bullet
  t = t.replace(/^\s*(\d+)[.)]\s+/gm, '$1. ');                   // keep numbered lists tidy
  t = t.replace(/^\s*[-_*]{3,}\s*$/gm, '');                      // horizontal rules
  t = t.replace(/^\s*>\s?/gm, '');                               // blockquotes
  t = t.replace(/\*{2,}/g, '');                                  // stray asterisks left over
  t = t.replace(/[ \t]+\n/g, '\n').replace(/\n{3,}/g, '\n\n');
  return t.trim();
}

/** Find the destination a question is about (word-boundary match on its name). */
export function findMentionedDestination(question: string, destinations: any[]): any | null {
  const q = (question || '').toLowerCase();
  if (!q) return null;
  const cands = destinations
    .map((d) => ({ d, names: Array.from(new Set([d.name.toLowerCase(), d.name.toLowerCase().replace(/^(new|old|north|south)\s+/, '')])) }))
    .sort((a, b) => b.d.name.length - a.d.name.length);
  for (const { d, names } of cands) {
    for (const n of names) {
      if (n.length < 3) continue;
      const re = new RegExp(`(^|[^a-z])${n.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}([^a-z]|$)`, 'i');
      if (re.test(q)) return d;
    }
  }
  return null;
}

/** Compact verified context for one destination. */
export function destinationContext(dest: any): string {
  const places = (dest.places || []).map((p: any) => `- ${p.name}${p.isSecretGem ? ' (hidden gem)' : ''}: ${p.description}`).join('\n');
  const foods = (dest.localFoods || []).map((f: any) => `- ${f.name} (${f.vegetarian ? 'veg' : 'non-veg'}): ${f.description} Try at: ${f.whereToTry}. ${f.priceRange}`).join('\n');
  const exps = (dest.localExperiences || []).map((e: any) => `- ${e.title}: ${e.description} (${e.duration})`).join('\n');
  return `CURATED DATA FOR ${dest.name.toUpperCase()}, ${dest.state}\nBest time: ${dest.bestTimeToVisit}\n\nPLACES:\n${places}\n\nFOOD:\n${foods}\n\nEXPERIENCES:\n${exps}`;
}
