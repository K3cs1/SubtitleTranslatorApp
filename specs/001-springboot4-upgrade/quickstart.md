# Quickstart Validation Guide: Spring Boot 4.1.0 Upgrade

**Date**: 2026-06-22
**Purpose**: Step-by-step validation that the Spring Boot 4.1.0 upgrade is complete
and all acceptance criteria from the spec are met.

---

## Prerequisites

- Git working tree is on the `001-springboot4-upgrade` branch (or equivalent).
- `OPENAI_API_KEY` environment variable is set to a valid OpenAI API key.
- Maven wrapper (`.\mvnw.cmd` on Windows) is available at the project root.
- Docker is installed (for Story 3 validation).
- `curl` and `jq` are available (or use Postman / any HTTP client).
- The `sample.srt` file is at `backend/src/main/resources/sample.srt`.

---

## Step 1 — Verify the POM version bump

```powershell
# Check the parent version in the root pom.xml
Select-String -Path "pom.xml" -Pattern "spring-boot-starter-parent"
```

Expected: the `<version>` tag directly below shows `4.1.0`.

---

## Step 2 — Build (Story 1, SC-001)

```powershell
.\mvnw.cmd clean package -DskipTests
```

**Expected outcome**: `BUILD SUCCESS` with exit code 0, zero compilation errors.
Any `ERROR` output line from the compiler is a failure.

---

## Step 3 — Run the full test suite (Story 1, SC-001, FR-007)

```powershell
.\mvnw.cmd -pl backend test
```

**Expected outcome**: All tests pass — including the three existing unit-test classes
and the two new smoke-test classes. Output should end with:
```
Tests run: N, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

If any test fails, investigate the failure before proceeding.

---

## Step 4 — Start the backend (Story 1)

```powershell
$env:OPENAI_API_KEY="<your-key>"
.\mvnw.cmd -pl backend spring-boot:run
```

**Expected outcome**: Log line containing `Started SubtitleTranslatorApp in X.XXX seconds`
appears, with no `ERROR` lines about missing beans, auto-configuration, or deprecated
removed APIs.

Leave this process running for Steps 5–7.

---

## Step 5 — Validate country list endpoint (Story 2, SC-002)

```bash
curl -s "http://localhost:5000/api/reference/countries" | jq .
```

**Expected outcome**:
```json
{
  "result": "SUCCESS",
  "message": "...",
  "data": [
    { "code": "...", "name": "..." },
    ...
  ]
}
```

HTTP status: `200 OK`. Each item in `data` has `code` and `name` fields.
See [contracts/rest-api.md](contracts/rest-api.md) for the full contract.

---

## Step 6 — Validate translation job endpoint (Story 2, SC-002)

```bash
curl -s -X POST "http://localhost:5000/api/translation-jobs" \
  -F "file=@./backend/src/main/resources/sample.srt" \
  -F "targetLanguage=Hungarian" | jq .
```

**Expected outcome**:
```json
{
  "result": "SUCCESS",
  "message": "Translation job started.",
  "data": {
    "jobId": "<uuid>",
    "message": "Translation job created. Use GET /api/translation-jobs/{jobId} to check status."
  }
}
```

HTTP status: `202 Accepted`.

Then poll until `COMPLETED`:

```bash
curl -s "http://localhost:5000/api/translation-jobs/<jobId>" | jq .data.status
```

---

## Step 7 — Validate error response (Story 2, SC-005)

```bash
curl -s -X POST "http://localhost:5000/api/translation-jobs" \
  -F "targetLanguage=Hungarian" | jq .
```

**Expected outcome**:
```json
{
  "result": "ERROR",
  "message": "...",
  "data": null
}
```

HTTP status: `400 Bad Request`. The `message` is a human-readable string with **no stack
trace** and **no Java class names** exposed.

---

## Step 8 — Validate startup time (SC-003)

Compare the startup time from Step 4 against the Spring Boot 3.5.15 baseline.
The startup time should not have regressed significantly (no more than +20% over baseline
is a reasonable threshold).

Startup time is printed in the log as:
```
Started SubtitleTranslatorApp in X.XXX seconds (process running for Y.YYY)
```

---

## Step 9 — Docker build (Story 3, FR-008)

Stop the running backend. Then:

```powershell
.\mvnw.cmd clean package -DskipTests
Copy-Item "backend\target\subtitle-translator-backend-0.0.1-SNAPSHOT.jar" "backend\app.jar"
docker build -t subtitle-translator-backend:4.1.0 backend/
```

**Expected outcome**: `Successfully built <image-id>` or `naming to docker.io/...`
with exit code 0.

Run the image locally to verify it starts:

```powershell
docker run --rm -e OPENAI_API_KEY=test -e PORT=5000 -p 5000:5000 subtitle-translator-backend:4.1.0
```

**Expected outcome**: Application starts and logs `Started SubtitleTranslatorApp`.
Ctrl+C to stop.

---

## Step 10 — CI pipeline (Story 3, SC-004)

Push the branch to `main` (or trigger `workflow_dispatch` on
`.github/workflows/deploy-backend-eb.yml`).

**Expected outcome**: All GitHub Actions workflow steps complete with green status,
including the Docker build and Elastic Beanstalk deployment steps.

---

## Failure Triage Quick Reference

| Symptom | Likely cause | Action |
|---------|-------------|--------|
| Compilation error mentioning Spring AI class | Spring AI 2.x API change | Check Spring AI 2.x migration guide; update imports/method calls |
| `NoSuchBeanDefinitionException` on `ChatClient.Builder` | Spring AI BOM incompatibility | Verify `spring-ai-starter-model-openai` is compatible with Spring Boot 4 |
| `UnsatisfiedDependencyException` on startup | Auto-configuration change | Check Spring Boot 4 migration guide for removed auto-configurations |
| Property `spring.servlet.multipart.*` ignored | Property renamed | Check Spring Boot 4 release notes for property migration |
| `ClassNotFoundException` for `MissingServletRequestPartException` | Package moved | Update import to new package in `GlobalExceptionHandler.java` |
| Docker build fails — `eclipse-temurin:25-jre` not found | Image tag unavailable | Update to latest `eclipse-temurin:25-jre-*` digest or tag |
| `CorsFilter` not applying headers | CORS configuration change | Switch to `WebMvcConfigurer.addCorsMappings` if needed |
