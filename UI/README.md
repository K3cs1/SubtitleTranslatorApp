# SubtitleTranslatorApp UI (React + Vite)

This is the frontend for SubtitleTranslatorApp.

## Configure

Create `UI/.env.local`:

```
VITE_API_BASE_URL=http://localhost:5000
```

`VITE_API_BASE_URL` must point to the backend base URL (Spring Boot).

## Run locally

```bash
npm install
npm run dev
```

UI dev server: `http://localhost:5173`

## What the UI does

- Loads “Target language” options from the backend:
  - `GET /api/reference/countries`
- Uploads an `.srt` file + selected target language to translate:
  - `POST /api/translation-jobs` (multipart/form-data with `file` and `targetLanguage`)
