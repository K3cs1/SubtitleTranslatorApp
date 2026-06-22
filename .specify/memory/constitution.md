<!--
SYNC IMPACT REPORT
==================
Version change: (blank template) → 1.0.0
Added sections:
  - Core Principles (I–V)
  - Technology Stack
  - Deployment & Infrastructure
  - Governance
Modified principles: N/A (initial ratification)
Removed sections: N/A
Templates requiring updates:
  - .specify/templates/plan-template.md  ✅ reviewed — Constitution Check gate aligns with principles below
  - .specify/templates/spec-template.md  ✅ reviewed — no updates needed
  - .specify/templates/tasks-template.md ✅ reviewed — task categories align with principles
Deferred TODOs: none
-->

# SubtitleTranslatorApp Constitution

## Core Principles

### I. Strict Layered Architecture

Every feature MUST respect the three-tier request-handling contract:

- **RestController** — the sole entry point for HTTP request/response handling.
  Controllers MUST NOT contain business logic and MUST NOT autowire Repositories directly
  (except where absolutely justified and documented).
- **ServiceImpl** — owns all business and database-operation logic, always via Repository
  methods. Direct database queries inside a ServiceImpl are forbidden unless a Repository
  method is insufficient and the deviation is explicitly justified.
- **Repository** — data-access interface only; extends `JpaRepository`; uses JPQL for
  custom queries; uses `@EntityGraph` on relationship queries to prevent N+1 problems.

Rationale: explicit layer boundaries make behaviour predictable, testable in isolation,
and maintainable as the codebase grows.

### II. DTO-First Data Transfer

All data flowing between layers MUST travel as DTOs:

- DTOs MUST be Java `record` types with a compact canonical constructor that validates
  all fields (non-null, non-blank, format checks as appropriate).
- Entity classes are only permitted as data containers for the immediate output of a
  database query execution; they MUST NOT be returned from ServiceImpl methods or
  accepted/returned by RestControllers.
- Response payloads from RestControllers MUST be `ResponseEntity<ApiResponse<T>>`,
  where `T` is a DTO or a collection of DTOs.

Rationale: records enforce immutability and self-validation; the ApiResponse wrapper
gives API consumers a consistent envelope regardless of endpoint.

### III. Code Quality — SOLID, DRY, KISS, YAGNI

All production code MUST comply with the following non-negotiable quality standards:

- **SOLID**: single responsibility per class, open/closed design, Liskov-safe
  hierarchies, interface segregation, dependency inversion via Spring injection.
- **DRY**: duplicated logic MUST be extracted into a shared service, utility, or
  constant before merging.
- **KISS**: the simplest design that satisfies the requirement is the correct one;
  complexity requires justification in the Complexity Tracking table of the plan.
- **YAGNI**: no speculative abstractions; features are built only when a concrete
  requirement exists.

Rationale: consistent code quality lowers onboarding cost, reduces defect density, and
keeps the codebase evolvable.

### IV. Security First — OWASP

Every feature MUST be reviewed against the current OWASP Top 10 before merging:

- Input validation MUST be applied at the Controller layer (Bean Validation annotations
  on DTOs and `@Valid` on controller parameters).
- Secrets (API keys, credentials) MUST be supplied via environment variables and MUST
  NOT be committed to source control.
- File upload endpoints MUST validate MIME type, file extension, and maximum size.
- Error responses MUST NOT leak internal stack traces; the `GlobalExceptionHandler` is
  the single point of exception-to-response mapping.

Rationale: security concerns retrofitted after the fact are expensive; building them in
from the start is the only acceptable approach.

### V. RESTful API Design Standards

All HTTP APIs exposed by the backend MUST follow resource-based REST conventions:

- Routes are class-level via `@RequestMapping`; method mappings use `@GetMapping`,
  `@PostMapping`, `@PutMapping`, `@DeleteMapping`.
- Path segments name resources (nouns), never verbs (`/api/translation-jobs`, not
  `/api/createTranslationJob`).
- Every controller method MUST be wrapped in a `try…catch` block; caught exceptions
  are delegated to `GlobalExceptionHandler.errorResponseEntity(…)`.
- Successful responses carry `result: "SUCCESS"`; error responses carry
  `result: "ERROR"` — never mixed conventions.

Rationale: predictable, self-documenting API surface reduces integration friction and
simplifies client-side error handling.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 3 (Maven) |
| AI integration | Spring AI — OpenAI (ChatGPT API) |
| Persistence | Spring Data JPA + PostgreSQL (when DB is needed) |
| Utilities | Lombok |
| Frontend | React 18 + Vite (TypeScript) |
| Build (full) | Maven wrapper (`./mvnw clean package`) with `frontend-maven-plugin` |
| Runtime config | Environment variables (`OPENAI_API_KEY`, `PORT`, `VITE_API_BASE_URL`) |

The backend serves on port `5000` by default. The UI dev server runs on port `5173`.
The ChatGPT translation endpoint is `POST /api/translation-jobs` (multipart/form-data).
Reference data (target languages) is proxied from the World Bank Countries API via
`GET /api/reference/countries`.

## Deployment & Infrastructure

- **Backend**: deployed to AWS Elastic Beanstalk (Docker platform, Amazon Linux 2023)
  via the GitHub Actions workflow `.github/workflows/deploy-backend-eb.yml`.
  Triggered on pushes to `main` touching backend files, or manually.
- **Frontend**: deployed to AWS S3 (optionally fronted by CloudFront) via
  `.github/workflows/deploy-ui-s3.yml`. Triggered on pushes to `main` touching UI
  files, or manually.
- Required GitHub secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`,
  `EB_APP_NAME`, `EB_ENV_NAME`, `EB_S3_BUCKET`, `UI_S3_BUCKET`, `VITE_API_BASE_URL`,
  `CLOUDFRONT_DISTRIBUTION_ID` (optional).
- The `OPENAI_API_KEY` secret MUST be injected as an EB environment variable; it MUST
  NOT appear in any committed file.

## Governance

This constitution is the authoritative governance document for SubtitleTranslatorApp.
It supersedes all other implicit or informal coding conventions.

**Amendment procedure**:
1. Propose the change with a rationale explaining why the current principle is
   insufficient or incorrect.
2. Update this file, bump the version following semantic versioning rules
   (MAJOR for breaking governance changes, MINOR for additions, PATCH for
   clarifications).
3. Update any dependent templates or plan artifacts affected by the change.
4. Record the amendment in the Sync Impact Report comment at the top of this file.
5. Commit with message: `docs: amend constitution to vX.Y.Z (<summary>)`.

**Compliance review**: every pull request MUST include a Constitution Check section
in its plan (or a reviewer note confirming no constitution principles are violated).
Violations require a written justification in the plan's Complexity Tracking table.

**Runtime guidance**: see `README.md` for build, run, and deployment commands.

**Version**: 1.0.0 | **Ratified**: 2026-06-22 | **Last Amended**: 2026-06-22
