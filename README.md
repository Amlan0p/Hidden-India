# Hidden India

Android app (Kotlin / Jetpack Compose) plus its Node.js backend, in one repository.

```
hidden-india/
├── android/        Android app (open this folder in Android Studio)
├── backend/        Node 22 + Express API, deployed to Render
├── render.yaml     Render Blueprint (points at ./backend)
├── .gitignore
└── .gitattributes
```

## 1. Put this on GitHub
Create an empty repository on GitHub (no README, no .gitignore), then in this folder:
```bash
git init -b main
git add .
git commit -m "Hidden India: Android app + backend"
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```
Use git or GitHub Desktop for the first push. GitHub's web uploader accepts only 100 files at a time and this project has more than 200.

## 2. Deploy the backend on Render
1. Render dashboard -> **New -> Blueprint** -> pick this repository.
2. Render reads `render.yaml` (service `hidden-india-backend`, root directory `backend`).
3. When asked, enter `GROQ_API_KEY` (and optionally `GEMINI_API_KEY`). Leave the optional ones empty if unused.
4. After the deploy, open `https://<your-service>.onrender.com/api/health`; it should answer.

If you previously created a Render service by hand for this backend, either delete it or point it at this repo with **Root Directory = `backend`**; do not run both.

Accounts, sessions, favourites and itineraries are stored in a SQLite file. Render's free plan wipes it on every deploy and restart. For persistence use a paid plan and enable the `disk` block that is commented out in `render.yaml`.

## 3. Point the Android app at the new backend
Open `android/app/build.gradle` and set the URL Render gave you (keep the trailing slash):
```groovy
buildConfigField "String", "API_BASE_URL", "\"https://<your-service>.onrender.com/\""
```
It currently points to `https://hidden-india-backend.onrender.com/`.

## 4. Build the app
Open `android/` in Android Studio and run, or from the command line:
```bash
cd android
./gradlew assembleDebug
```

## Image data
All destination, place, food and experience images come from the bundled, read-only database
`android/app/src/main/assets/databases/hidden_india_image_database_final.sqlite`, matched by record ID.
Check it with `python3 android/tools/validate_image_database.py`.
If you replace that file, raise `IMAGE_DB_VERSION` in `ImageRepository.kt` so installed apps refresh their copy.

## Secrets
Never commit API keys. `.env` files, keystores and `local.properties` are git-ignored; set keys in the Render dashboard.
