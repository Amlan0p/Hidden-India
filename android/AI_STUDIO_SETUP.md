# Deploying Hidden India (Android + backend on Render + frontend on AI Studio)

This app is split into three pieces:

- **hidden-india-android** (this folder) — the native Kotlin/Compose app. Build with
  Android Studio (or import into Google AI Studio's Android platform).
- **hidden-india-backend** (sibling zip) — the Node/Express API. Deploy to **Render**.
- **hidden-india-frontend** (sibling zip) — the React web app. Import into **Google AI
  Studio**'s Web app platform.

See the deployment walkthrough provided alongside these files for exact, numbered
steps (where to paste the Groq API key, where the Render URL goes, etc).

## Where the backend URL goes in this project
Once your backend is live on Render, open `app/build.gradle` and replace:
```
buildConfigField "String", "API_BASE_URL", "\"https://YOUR-BACKEND-DOMAIN/\""
```
with your actual Render URL, e.g. `"https://hidden-india-backend.onrender.com/"`
(keep the trailing slash).
