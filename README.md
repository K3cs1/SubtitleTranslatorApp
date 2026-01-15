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

## Deploy backend to AWS Elastic Beanstalk

This repository includes a GitHub Actions workflow that builds the backend and
deploys it to Elastic Beanstalk using the Docker platform.

### One-time AWS setup

1. Create an Elastic Beanstalk application and environment:
   - Platform: **Docker** (64bit Amazon Linux 2023)
   - Instance profile should allow EB to read from the S3 bucket used for deployments.
2. Create an S3 bucket for application versions.
3. Configure backend environment variables in the EB environment:
   - `OPENAI_API_KEY` (required)
   - `DEEPL_API_KEY` (required)
   - `DEEPL_BASE_URL` (optional, defaults to `https://api-free.deepl.com`)

### GitHub repository secrets

Add these secrets to your GitHub repo:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `EB_APP_NAME`
- `EB_ENV_NAME`
- `EB_S3_BUCKET`

The workflow file is: `.github/workflows/deploy-backend-eb.yml`.
It runs on pushes to `main` that touch backend files, or manually via
`workflow_dispatch`.

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
