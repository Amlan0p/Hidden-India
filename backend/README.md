# Hidden India: backend

Node 22.13+ / Express / TypeScript API (AI trip planner, Ask-the-Place, translator, accounts, SOS helper).

## Run locally
```bash
cd backend
cp .env.example .env      # add GROQ_API_KEY
npm install
npm run dev               # http://localhost:3001/api/health
```

## Build / start (what Render runs)
```bash
npm install --include=dev && npm run build
npm start
```

## Notes
- The SQLite database lives in `data/` (git-ignored). On Render's free plan it is not persistent.
- `GET /api/geo/nearest-emergency?lat=&lon=` finds the nearest hospital and police station through OpenStreetMap (used by the SOS screen as a fallback).
- Image URLs are no longer served by this API: the Android app reads them from its bundled image database.
