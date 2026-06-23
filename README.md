# SubtitleTranslatorApp

Translate `.srt` subtitles via a Spring Boot backend and a React (Vite) UI.

## How it works (high level)

- **UI**: Uploads an `.srt` file and sends the selected **Target language** to the backend.
- **Backend**:
  - Validates and parses the `.srt`
  - Calls the ChatGPT API (Spring AI OpenAI) to translate subtitle entries into the selected target language
  - Returns the translated `.srt` content (base64) so the UI can offer it for download
- **Reference data**: The “Target language” combo is populated from the World Bank Countries API via a backend proxy endpoint.

## API endpoints

- **List countries for the UI combo**
  - `GET /api/reference/countries`
  - Response: `ApiResponse<List<CountryOptionDto>>` where each item is `{ code, name }`
- **Translate an `.srt`**
  - `POST /api/translation-jobs` (multipart/form-data)
  - Fields:
    - `file`: the `.srt` file
    - `targetLanguage`: target language label (currently the selected **country name** from the combo)

Example requests:

```bash
curl -s "http://localhost:5000/api/reference/countries" | jq .
```

```bash
curl -s -X POST "http://localhost:5000/api/translation-jobs" \
  -F "file=@./backend/src/main/resources/sample.srt" \
  -F "targetLanguage=Hungarian"
```

## Prerequisites

- **JDK 25** — the project targets Java 25. Ensure `JAVA_HOME` points to your JDK 25 installation before running Maven from the command line:
  ```powershell
  $env:JAVA_HOME = "C:\Java\jdk-25"   # adjust path to your JDK 25 install
  ```
  > IntelliJ IDEA users: configure the Maven JDK in **Settings → Build → Build Tools → Maven → JDK for importer** (and the project SDK) to JDK 25.

## Build with Maven (whole project)

From the repository root, build both modules (backend + UI):

```bash
./mvnw clean package
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean package
```

This runs the Spring Boot 4.1.0 build and the UI build via the `frontend-maven-plugin`.

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

- `OPENAI_API_KEY` (required) — used by Spring AI to call the ChatGPT API
- `PORT` (optional, defaults to `5000`)

The backend starts on `http://localhost:5000` by default.

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
   - `PORT` (optional; Elastic Beanstalk commonly injects this automatically)

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

## Deploy UI to AWS (S3 + optional CloudFront)

This repository includes a GitHub Actions workflow that builds the Vite UI and deploys the generated static files (`UI/dist`) to an S3 bucket.

### One-time AWS setup

1. Create an S3 bucket to host the UI (static website hosting is optional if you front it with CloudFront).
2. (Optional, recommended) Create a CloudFront distribution with the S3 bucket as origin.

### GitHub repository secrets

Add these secrets to your GitHub repo:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `UI_S3_BUCKET` (target bucket name)
- `VITE_API_BASE_URL` (backend base URL, e.g. `http://localhost:8080` or your Elastic Beanstalk URL)
- `CLOUDFRONT_DISTRIBUTION_ID` (optional; if set, the workflow invalidates `/*`)

The workflow file is: `.github/workflows/deploy-ui-s3.yml`.
It runs on pushes to `main` that touch UI files, or manually via
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
  $env:VITE_API_BASE_URL="http://localhost:5000"
  ```
- Or create `UI/.env.local`:
  ```
  VITE_API_BASE_URL=http://localhost:5000
  ```

The UI runs at `http://localhost:5173`.

## Use the UI

1. Start the backend and frontend.
2. Open `http://localhost:5173` in the browser.
3. Pick a `.srt` file using the file picker.
4. Select a **Target language** from the combo.
5. Click **Start translation** and watch the status message.

The backend returns the translated `.srt` content to the UI (base64) and the UI offers it as a download.
On the backend machine, a translated `.srt` file is also written to the backend user’s home directory with a suffix based on the selected target language.

## GitHub Spec Kit

This repository uses [GitHub Spec Kit](https://github.com/github/spec-kit) (v0.11.3) for
[Spec-Driven Development](https://github.github.io/spec-kit/concepts/sdd.html). Features are
defined as version-controlled Markdown artifacts (spec → plan → tasks → implement) that guide
AI-assisted development in Cursor via Claude skills.

### Project layout

```text
.
├── .specify/
│   ├── memory/constitution.md   # Project governing principles
│   ├── templates/               # Spec, plan, and task templates
│   └── scripts/                 # Feature scaffolding helpers
├── .claude/skills/speckit-*/    # Cursor slash-command skills
└── specs/
    └── 001-springboot4-upgrade/ # Feature artifacts (spec, plan, tasks, …)
```

### Slash commands (Cursor / Claude)

| Command | Purpose |
| --- | --- |
| `/speckit-constitution` | Create or update project principles |
| `/speckit-specify` | Define requirements for a new feature |
| `/speckit-clarify` | Resolve ambiguities before planning |
| `/speckit-checklist` | Validate requirement quality |
| `/speckit-plan` | Create a technical implementation plan |
| `/speckit-tasks` | Generate an actionable task list |
| `/speckit-analyze` | Cross-check spec, plan, and tasks before implementation |
| `/speckit-implement` | Execute tasks from `tasks.md` |
| `/speckit-converge` | Append remaining work after a partial implement |
| `/speckit-taskstoissues` | Convert tasks to GitHub issues |

### Recommended workflow

For production features:

```text
/speckit-specify → /speckit-clarify → /speckit-checklist → /speckit-plan → /speckit-tasks → /speckit-analyze → /speckit-implement
```

For quick experiments:

```text
/speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement
```

Spec Kit tracks the active feature from the current Git branch (for example,
`001-springboot4-upgrade`). Switch branches to work on a different feature.

### Key artifacts

- **Constitution**: [.specify/memory/constitution.md](.specify/memory/constitution.md) — layered architecture, DTO rules, OWASP, and deployment constraints
- **Current feature**: [specs/001-springboot4-upgrade/](specs/001-springboot4-upgrade/) — Spring Boot 4.1.0 upgrade spec, plan, and tasks
- **Agent context**: [CLAUDE.md](CLAUDE.md) — managed plan pointer for coding agents (refreshed after `/speckit-specify` and `/speckit-plan`)

### Starting a new feature

1. Run `/speckit-specify` with a plain-language description of what to build (this creates `specs/NNN-feature-name/`).
2. Follow the workflow above through implementation.
3. See the [Spec Kit quick start](https://github.github.io/spec-kit/quickstart.html) for full documentation.
