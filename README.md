# SubtitleTranslatorApp

## Build with Maven (whole project)

From the repository root, build both modules (backend + UI):

```bash
./mvnw clean package
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean package
```

This runs the Spring Boot build and the UI build via the `frontend-maven-plugin`.

## Start the backend locally

From the repository root:

```bash
./mvnw -pl backend spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -pl backend spring-boot:run
```

Environment variables used by the backend:

- `OPENAI_API_KEY` (required)
- `DEEPL_API_KEY` (required)
- `DEEPL_BASE_URL` (optional, defaults to `https://api-free.deepl.com`)

The backend starts on `http://localhost:8080` by default.

## Start the frontend locally

From the `UI` folder:

```bash
npm install
npm run dev
```

Configure the backend base URL for the UI:

- PowerShell (current session):
  ```powershell
  $env:VITE_API_BASE_URL="http://localhost:8080"
  ```
- Or create `UI/.env.local`:
  ```
  VITE_API_BASE_URL=http://localhost:8080
  ```

The UI runs at `http://localhost:5173`.

## Use the UI

1. Start the backend and frontend.
2. Open `http://localhost:5173` in the browser.
3. Pick a `.srt` file using the file picker.
4. Click **Start translation** and watch the status message.

The backend processes the file asynchronously and writes the translated file
next to the uploaded temp file with a `_hun.srt` suffix (in the system temp
directory on the backend machine).
