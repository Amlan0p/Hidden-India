#!/usr/bin/env python3
"""
Validation for the Hidden India image-database integration.
Run from the project root:   python3 tools/validate_image_database.py
Needs only the Python standard library. Exit code 0 = all checks passed.

It re-implements exactly what ImageRepository.applyImages() does (match by record ID, and only
apply when type, name, destination and state agree) and checks the result against the spec.
"""
import json, re, sqlite3, sys, hashlib, pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSET = ROOT / "app/src/main/assets/databases/hidden_india_image_database_final.sqlite"
DATA = ROOT / "app/src/main/assets/destinations.json"
SRC = ROOT / "app/src/main/java"
# Expected contents of the CURRENT bundled database (audited edition). Update when the .sqlite is replaced.
EXPECTED_VERIFIED = 2617
EXPECTED_BLANK = 0
EXPECTED_PER_TYPE = {"destination_cover": 167, "place": 779, "experience": 835, "food": 836}
results = []
def check(n, ok, detail=""):
    results.append(ok); print(f"[{'PASS' if ok else 'FAIL'}] {n}" + (f"  ({detail})" if detail else ""))

con = sqlite3.connect(f"file:{ASSET}?mode=ro", uri=True); con.row_factory = sqlite3.Row
rows = {r["record_id"]: dict(r) for r in con.execute("SELECT * FROM image_records")}
by_type = lambda t: [r for r in rows.values() if r["type"] == t]
verified = {k: r for k, r in rows.items() if r["verification_status"] == "verified" and r["image_url"]}

check("1. database has 2,617 records", len(rows) == 2617, len(rows))
check("2. 167 destination records", len(by_type("destination_cover")) == 167)
check("3. 779 place records", len(by_type("place")) == 779)
check("4. 835 experience records", len(by_type("experience")) == 835)
check("5. 836 food records", len(by_type("food")) == 836)
check("6. 2,617 non-empty image URLs, all verified", sum(1 for r in rows.values() if r["image_url"]) == EXPECTED_VERIFIED and len(verified) == EXPECTED_VERIFIED,
      len(verified))
check("   no record is left without a verified image (blank / needs_manual_review count = 0)",
      sum(1 for r in rows.values() if r["verification_status"] == "needs_manual_review" and not r["image_url"]) == EXPECTED_BLANK)

app = json.load(open(DATA, encoding="utf-8"))
recs = {}
for d in app:
    recs[d["id"]] = ("destination_cover", d["name"], d["id"], d["state"])
    for p in d["places"]: recs[p["id"]] = ("place", p["name"], d["id"], d["state"])
    for f in d["localFoods"]: recs[f["id"]] = ("food", f["name"], d["id"], d["state"])
    for e in d["localExperiences"]: recs[e["id"]] = ("experience", e["title"], d["id"], d["state"])
check("   app has 167/779/836/835/835 destinations/places/foods/experiences/hotels",
      (len(app), sum(len(d["places"]) for d in app), sum(len(d["localFoods"]) for d in app),
       sum(len(d["localExperiences"]) for d in app), sum(len(d["hotels"]) for d in app)) == (167, 779, 836, 835, 835))
check("   every app record ID exists in the database and vice versa", set(recs) == set(rows),
      f"app={len(recs)} db={len(rows)}")

applied, mismatched, placeholders = 0, [], 0
for rid, (t, n, did, st) in recs.items():
    r = rows[rid]
    ok_match = (r["type"], r["name"], r["destination_id"], r["state"]) == (t, n, did, st)
    if not ok_match: mismatched.append(rid)
    if rid in verified and ok_match: applied += 1
    else: placeholders += 1
check("7. every non-empty image URL has a matching app record", all(k in recs for k in verified))
check("8. no image is assigned to a different record (type/name/destination/state agree)", not mismatched, f"{len(mismatched)} mismatches")
check("9. empty-URL / unverified records get a placeholder (none in this edition)", placeholders == EXPECTED_BLANK and applied == EXPECTED_VERIFIED,
      f"images={applied}, placeholders={placeholders}")
exp = EXPECTED_PER_TYPE
got = {t: sum(1 for k in verified if rows[k]["type"] == t) for t in exp}
check("   per type images: covers 167, places 779, experiences 835, foods 836", got == exp, got)

# Welcome-screen / hero artwork that existed before the integration (not destination, place,
# food or experience images). Left unchanged on purpose so the UI stays exactly as it was.
HERO_ARTWORK = {"ExperienceIndiaHero.kt", "WelcomeScreen.kt"}
hard, hero = [], []
for p in SRC.rglob("*.kt"):
    for i, line in enumerate(p.read_text(encoding="utf-8").splitlines(), 1):
        if re.search(r"https?://(upload|thumb)\.wikimedia\.org", line):
            (hero if p.name in HERO_ARTWORK else hard).append(f"{p.name}:{i}")
check("   no hard-coded record image URLs in Kotlin sources", not hard, hard[:3])
print(f"[INFO]   {len(hero)} pre-existing hero/welcome artwork URLs left unchanged: {sorted(set(h.split(':')[0] for h in hero))}")
check("   no runtime Wikimedia/Wikipedia search code remains",
      not any("wikipedia.org/w/api.php" in p.read_text(encoding="utf-8") or "commons.wikimedia.org/w/api.php" in p.read_text(encoding="utf-8") for p in SRC.rglob("*.kt")))
allv = [r for r in rows.values() if r["image_url"]]
check("   every image URL is an exact https://upload.wikimedia.org URL", all(r["image_url"].startswith("https://upload.wikimedia.org/") for r in allv))
check("   bundled database contains only the image_records table",
      [t[0] for t in con.execute("SELECT name FROM sqlite_master WHERE type='table'")] == ["image_records"])
mp = [(r["image_width"] or 0) * (r["image_height"] or 0) / 1e6 for r in allv]
print(f"[INFO]   full-size originals: {sum(1 for x in mp if x > 12)} images over 12 megapixels, {sum(1 for x in mp if x > 25)} over 25 MP, largest {max(mp):.0f} MP")
srcs = {p.name: p.read_text(encoding="utf-8") for p in SRC.rglob("*.kt")}
check("   no name-based image lookup remains (findVerifiedUrl / ImageResolver / SQL on name)",
      not any(("findVerifiedUrl" in t or "ImageResolver." in t or "object ImageResolver" in t or re.search(r"WHERE\s+name\s*=", t)) for t in srcs.values()))
check("   ResolvedImages and OfflineImageCache use ImageRepository.imageUrlFor (exact record ID)",
      "imageUrlFor" in srcs["ResolvedImages.kt"] and "imageUrlFor" in srcs["OfflineImageCache.kt"])
print("\nOverall:", "ALL CHECKS PASSED" if all(results) else "FAILURES PRESENT")
sys.exit(0 if all(results) else 1)
