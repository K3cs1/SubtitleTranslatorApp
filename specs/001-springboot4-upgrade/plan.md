# Implementation Plan: Spring Boot 4.1.0 Upgrade

**Branch**: `001-springboot4-upgrade` | **Date**: 2026-06-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-springboot4-upgrade/spec.md`

## Summary

Upgrade the backend from Spring Boot `3.5.15` to `4.1.0`. The upgrade touches the root
`pom.xml` parent declaration, the Spring AI BOM version, and the Lombok version. Any
breaking-change compilation errors introduced by Spring Boot 4 (via Spring Framework 7)
must be resolved. A minimal smoke test suite must be written. The Dockerfile must be
explicitly reviewed and updated if needed. The existing REST API contracts (`GET
/api/reference/countries`, `POST /api/translation-jobs`, `GET
/api/translation-jobs/{jobId}`) must remain fully intact. The upgrade is a hard blocker
if no compatible Spring AI release exists at implementation time.

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**:
- Spring Boot 4.1.0 (target)
- Spring AI (version compatible with Spring Boot 4 — see research.md)
- Lombok 1.18.42+ (verify compatibility; upgrade if needed)
- Spring Web (spring-boot-starter-web)
- spring-boot-starter-test (JUnit 5 + Mockito + AssertJ)

**Storage**: N/A — no database; in-memory job store only

**Testing**: JUnit 5, Mockito, AssertJ via `spring-boot-starter-test`; 3 existing
unit-test classes; smoke tests added as part of this feature

**Target Platform**: Linux server (AWS Elastic Beanstalk Docker, Amazon Linux 2023);
local Windows/PowerShell for development

**Project Type**: Web service (Spring Boot REST API)

**Performance Goals**: No measurable startup-time regression vs. Spring Boot 3.5.15
baseline

**Constraints**:
- Spring AI MUST have a published release compatible with Spring Boot 4.1.0 — hard
  blocker if absent
- Java 25 is already above the Spring Boot 4 minimum (Java 21); no JDK change needed
- Dockerfile base image (`eclipse-temurin:25-jre`) already covers Java 25; verify it
  is still the recommended image for the Spring Boot 4 era

**Scale/Scope**: Single backend Maven module; UI module unaffected

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Strict Layered Architecture | ✅ Pass | Upgrade does not alter layer boundaries; controller → service → (no repo) chain unchanged |
| II. DTO-First Data Transfer | ✅ Pass | All DTOs remain `record` types; `ApiResponse<T>` envelope unchanged |
| III. Code Quality — SOLID, DRY, KISS, YAGNI | ✅ Pass | No new abstractions introduced; breaking changes resolved in-place |
| IV. Security First — OWASP | ✅ Pass | `GlobalExceptionHandler` must be verified to remain operative; no stack traces leaked |
| V. RESTful API Design Standards | ✅ Pass | All existing resource-based paths and response conventions preserved |

**Gate result**: PASS — no violations. Proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/001-springboot4-upgrade/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (configuration model)
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── rest-api.md      # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
pom.xml                                  ← spring-boot-starter-parent version bump
backend/
├── pom.xml                              ← no changes expected
├── Dockerfile                           ← review + update if needed
└── src/
    ├── main/
    │   ├── java/org/k3cs1/subtitletranslatorapp/
    │   │   ├── SubtitleTranslatorApp.java
    │   │   ├── api/ApiResponse.java
    │   │   ├── config/
    │   │   │   ├── CorsConfig.java      ← review for Spring Boot 4 CORS changes
    │   │   │   └── ExecutorsConfig.java
    │   │   ├── controller/
    │   │   │   ├── TranslationJobController.java  ← review multipart/exception handling
    │   │   │   ├── ReferenceDataController.java
    │   │   │   └── HealthController.java
    │   │   ├── dto/                     ← no changes expected
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java  ← verify exception mapping intact
    │   │   ├── model/SrtEntry.java
    │   │   ├── parser/SrtIOParser.java
    │   │   └── service/                 ← review Spring AI ChatClient usage
    │   └── resources/application.yml   ← verify multipart property paths
    └── test/
        └── java/org/k3cs1/subtitletranslatorapp/
            ├── SubtitleTranslatorAppSmokeTest.java  ← NEW: context load test
            ├── controller/
            │   └── TranslationJobControllerSmokeTest.java  ← NEW: endpoint smoke tests
            ├── parser/SrtIOParserTest.java           ← existing; must still pass
            └── service/
                ├── SrtTranslatorServiceImplTest.java  ← existing; must still pass
                └── TranslationJobServiceImplTest.java ← existing; must still pass
```

**Structure Decision**: Web application (Option 2) — `backend/` module with React/Vite
`UI/` module. The upgrade scope is limited to the `backend/` module; `UI/` is unchanged.

## Complexity Tracking

> No constitution violations detected; table not required.
