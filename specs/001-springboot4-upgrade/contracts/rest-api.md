# REST API Contracts: SubtitleTranslatorApp Backend

**Date**: 2026-06-22
**Base URL**: `http://localhost:5000` (local) | Elastic Beanstalk URL (production)

> These contracts define the external interface of the backend. They are **unchanged**
> by the Spring Boot 4.1.0 upgrade. Any deviation after the upgrade constitutes a
> regression and must be fixed before merging.

All responses use the `ApiResponse<T>` envelope:

```json
{
  "result": "SUCCESS" | "ERROR",
  "message": "<human-readable string>",
  "data": <T> | null
}
```

---

## 1. List Countries (Reference Data)

**Purpose**: Provides the target-language combo options to the UI.

### Request

```
GET /api/reference/countries
Content-Type: (none required)
```

### Response — 200 OK

```json
{
  "result": "SUCCESS",
  "message": "<string>",
  "data": [
    { "code": "HU", "name": "Hungary" },
    { "code": "DE", "name": "Germany" }
  ]
}
```

`data` is a JSON array of country option objects. Each object has:

| Field | Type | Description |
|-------|------|-------------|
| `code` | string | ISO country code |
| `name` | string | Country display name |

### Error responses

| HTTP Status | When |
|-------------|------|
| 503 | World Bank API is unreachable or returns an error |
| 500 | Unexpected server error |

---

## 2. Create Translation Job

**Purpose**: Accepts a `.srt` file and a target language; returns a job ID immediately
while translation runs asynchronously in the background.

### Request

```
POST /api/translation-jobs
Content-Type: multipart/form-data

Parts:
  file          — binary (.srt file, ≤ 2 MB)
  targetLanguage — string (e.g. "Hungarian", "German")
```

### Response — 202 Accepted

```json
{
  "result": "SUCCESS",
  "message": "Translation job started.",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "Translation job created. Use GET /api/translation-jobs/{jobId} to check status."
  }
}
```

`data` fields:

| Field | Type | Description |
|-------|------|-------------|
| `jobId` | string (UUID) | Identifier to poll for status |
| `message` | string | Human-readable guidance |

### Error responses

| HTTP Status | When |
|-------------|------|
| 400 | Missing `file` part, missing `targetLanguage`, file is not `.srt`, file exceeds 2 MB, or content is not valid SRT |
| 413 | File exceeds the server-level multipart size limit (3 MB) |
| 503 | Server is busy (executor rejected the job) |
| 500 | Unexpected server error |

Error response shape:
```json
{
  "result": "ERROR",
  "message": "<user-friendly reason>",
  "data": null
}
```

---

## 3. Get Translation Job Status

**Purpose**: Polls the status of a previously created translation job.

### Request

```
GET /api/translation-jobs/{jobId}
```

### Response — 200 OK

```json
{
  "result": "SUCCESS",
  "message": "Job status retrieved.",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "originalFileName": "movie.srt",
    "status": "COMPLETED",
    "translatedEntries": 42,
    "totalEntries": 42,
    "outputFileName": "movie_hungarian.srt",
    "contentBase64": "<base64-encoded .srt content>",
    "errorMessage": null
  }
}
```

`data.status` values:

| Value | Meaning |
|-------|---------|
| `PENDING` | Job accepted; translation not yet started |
| `PROCESSING` | Translation in progress |
| `COMPLETED` | Translation done; `contentBase64` and `outputFileName` are populated |
| `FAILED` | Translation failed; `errorMessage` contains the reason |

### Error responses

| HTTP Status | When |
|-------------|------|
| 404 | `jobId` not found |
| 500 | Unexpected server error |

---

## 4. Health Check

**Purpose**: Liveness probe for the deployment platform.

### Request

```
GET /api/health
```

### Response — 200 OK

```json
{
  "result": "SUCCESS",
  "message": "OK",
  "data": null
}
```

---

## CORS Policy

The backend allows cross-origin requests from:

- `http://localhost:5173` (local Vite dev server)
- `http://127.0.0.1:5173`
- `https://*.cloudfront.net`
- `https://d1yzzvrnimz8tw.cloudfront.net`

Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
Credentials: not allowed (`allowCredentials: false`)
