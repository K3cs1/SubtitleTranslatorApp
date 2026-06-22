# Data Model: Spring Boot 4.1.0 Upgrade

**Date**: 2026-06-22

> This feature introduces no new entities or schema changes. The backend has no database
> (Spring Data JPA is not active in this project; there is no PostgreSQL connection
> configured). The "data model" for this upgrade is the **configuration model** — the
> properties and environment variables that govern runtime behaviour.

---

## Configuration Model

### Root POM properties (`pom.xml`)

| Property | Before | After | Status |
|----------|--------|-------|--------|
| `spring-boot-starter-parent` version | `3.5.15` | `4.1.0` | ✅ Done |
| `spring-ai.version` | `1.1.2` | `2.0.0` | ✅ Done — confirmed GA, compatible with Boot 4.1.x |
| `lombok.version` | `1.18.42` | `1.18.42` | ✅ No change — annotation processor compiles cleanly |
| `java.version` | `25` | `25` | ✅ No change |

### Backend starter (`backend/pom.xml`)

| Dependency | Before | After | Status |
|------------|--------|-------|--------|
| `spring-boot-starter-web` | present | replaced by `spring-boot-starter-webmvc` | ✅ Done — `starter-web` deprecated in Boot 4 |
| `spring-boot-starter-test` | present | present | ✅ No change |
| `spring-boot-starter-webmvc-test` | absent | added (test scope) | ✅ Done — required for `@WebMvcTest` in Boot 4 |

---

### Application configuration (`backend/src/main/resources/application.yml`)

All properties listed below must be **verified to remain valid** after the Spring Boot
4.1.0 upgrade. No value changes are expected unless property keys are renamed.

#### Spring AI (OpenAI)

| Key | Value | Status |
|-----|-------|--------|
| `spring.ai.openai.api-key` | `${OPENAI_API_KEY:OPENAI_API_KEY}` | ✅ Unchanged |
| ~~`spring.ai.openai.chat.options.model`~~ → `spring.ai.openai.chat.model` | `gpt-4o-mini` | ✅ Fixed — `.options` segment removed in Spring AI 2.0 |
| ~~`spring.ai.openai.chat.options.temperature`~~ → `spring.ai.openai.chat.temperature` | `0.0` | ✅ Fixed — `.options` segment removed in Spring AI 2.0 |

#### Spring Web / Multipart

| Key | Value | Status |
|-----|-------|--------|
| `spring.servlet.multipart.max-file-size` | `2MB` | ✅ Unchanged |
| `spring.servlet.multipart.max-request-size` | `3MB` | ✅ Unchanged |

#### Tomcat

| Key | Value | Status |
|-----|-------|--------|
| `server.tomcat.max-http-form-post-size` | `3MB` | ✅ Unchanged |
| `server.tomcat.max-swallow-size` | `3MB` | ✅ Unchanged |

#### Jackson 3 migration (`WorldBankReferenceServiceImpl.java`)

Spring Boot 4 uses Jackson 3 (`tools.jackson.*` packages). The following import change was required:

| Before | After | Status |
|--------|-------|--------|
| `com.fasterxml.jackson.databind.JsonNode` | `tools.jackson.databind.JsonNode` | ✅ Fixed |
| `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` | ✅ Fixed |

> `@JsonProperty`, `@JsonIgnore`, and other annotations from `com.fasterxml.jackson.annotation` are **unchanged** (Jackson annotations remain on the original group ID in Jackson 3).

#### Custom properties (no change expected)

| Key | Current value |
|-----|--------------|
| `deepl.base-url` | `${DEEPL_BASE_URL:https://api-free.deepl.com}` |
| `deepl.auth-key` | `${DEEPL_API_KEY:DEEPL_API_KEY}` |
| `translation.batch-size` | `60` |
| `translation.max-batch-chars` | `12000` |
| `translation.max-parallel` | `5` |
| `server.port` | `${PORT:5000}` |
| `server.address` | `0.0.0.0` |
| `logging.level.root` | `warn` |

---

## Runtime Data Structures (unchanged)

These in-memory structures exist in the running application and are not affected by the
upgrade:

| Structure | Type | Location | Change |
|-----------|------|----------|--------|
| `SrtEntry` | Java record | `model/SrtEntry.java` | None |
| Translation job store | `ConcurrentHashMap` (in-memory) | `TranslationJobStore` | None |
| `TranslationJobStatusResponse` | Java record | `dto/` | None |
| `TranslationJobCreateResponse` | Java record | `dto/` | None |
| `TranslationJobRequest` | Java record | `dto/` | None |
| `CountryOptionDto` | Java record | `dto/` | None |

---

## Environment Variables

| Variable | Required | Used by | Change |
|----------|----------|---------|--------|
| `OPENAI_API_KEY` | Yes | Spring AI OpenAI auto-config | None — verify key is still consumed the same way in Spring AI 2.x |
| `DEEPL_API_KEY` | No (optional) | `DeeplTranslatorServiceImpl` | None |
| `DEEPL_BASE_URL` | No (optional) | `DeeplTranslatorServiceImpl` | None |
| `PORT` | No (defaults to 5000) | Spring Boot server config | None |
