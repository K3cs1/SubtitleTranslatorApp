---
description: "Task list for Spring Boot 4.1.0 upgrade"
---

# Tasks: Spring Boot 4.1.0 Upgrade

**Input**: Design documents from `specs/001-springboot4-upgrade/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/rest-api.md ✅ | quickstart.md ✅

**Tests**: Smoke tests are **in scope** per spec clarification (FR-007). TDD is not applicable here; smoke tests are written concurrently with the compilation-fix phase since the app context cannot load until the code compiles.

**Organization**: Tasks follow the upgrade's natural dependency chain: verify blocker → update POM → fix compilation → write smoke tests → validate contracts → validate Docker → deploy.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared compilation state dependencies)
- **[Story]**: Which user story this task belongs to
- Include exact file paths in descriptions

## Path Conventions

- Root POM: `pom.xml`
- Backend module: `backend/`
- Backend source: `backend/src/main/java/org/k3cs1/subtitletranslatorapp/`
- Backend resources: `backend/src/main/resources/`
- Backend tests: `backend/src/test/java/org/k3cs1/subtitletranslatorapp/`
- Dockerfile: `backend/Dockerfile`

---

## Phase 1: Setup (Hard Blocker Verification)

**Purpose**: Confirm the upgrade can proceed — the Spring AI hard-blocker gate (FR-003, Clarification Q1).

**⚠️ CRITICAL**: If T001 finds no compatible Spring AI release, the entire upgrade MUST be paused.

- [X] T001 Confirm a Spring AI BOM version compatible with Spring Boot 4.1.0 is published (check https://github.com/spring-projects/spring-ai/releases and Maven Central for `spring-ai-bom`). Document the confirmed version number before proceeding. **Confirmed: `spring-ai-bom:2.0.0` (GA, June 2026) — supports Spring Boot 4.0.x and 4.1.x.**

**Checkpoint**: Compatible Spring AI version confirmed — record the exact version, then proceed.

---

## Phase 2: Foundational (POM Version Bump — Blocks All User Stories)

**Purpose**: Perform all `pom.xml` changes and surface compilation errors introduced by the Spring Boot 4.1.0 / Spring Framework 7 upgrade.

**⚠️ CRITICAL**: No user story work can begin until the project compiles cleanly.

- [X] T002 Update `spring-boot-starter-parent` version from `3.5.15` to `4.1.0` in `pom.xml`
- [X] T003 Update `spring-ai.version` property in `pom.xml` to the version confirmed in T001; verify `spring-ai-bom` artifact coordinates are unchanged. **Set to `2.0.0`; artifact ID `spring-ai-bom` unchanged.**
- [X] T004 [P] Verify Lombok `1.18.42` annotation processor compiles without errors under Java 25 + Spring Boot 4.1.0; if it fails, update `lombok.version` in `pom.xml` to the latest stable compatible release. **No change needed — compiles cleanly.**
- [X] T005 Run `.\mvnw.cmd clean compile -pl backend` and capture the full list of compilation errors to drive T006–T009. **BUILD SUCCESS — zero compilation errors after POM updates and Jackson 3 import fix.**

**Checkpoint**: Foundation ready — compilation error inventory in hand; user story fixes can now begin in parallel.

---

## Phase 3: User Story 1 — Backend Compiles and Starts on Spring Boot 4.1.0 (Priority: P1) 🎯 MVP

**Goal**: Zero compilation errors, application context loads, backend starts cleanly.

**Independent Test**: `.\mvnw.cmd -pl backend test` passes with zero failures; `.\mvnw.cmd -pl backend spring-boot:run` starts without ERROR lines.

### Fix compilation errors for User Story 1

- [X] T006 [P] [US1] Resolve Spring AI `ChatClient` / `ChatClient.Builder` API breaking changes (if any) in `backend/src/main/java/org/k3cs1/subtitletranslatorapp/service/SrtTranslatorServiceImpl.java` — update imports and method calls to match Spring AI 2.x API. **No-op — `ChatClient.prompt().system().user().call().content()` chain is unchanged in Spring AI 2.0.**
- [X] T007 [P] [US1] Resolve any CORS-related breaking changes in `backend/src/main/java/org/k3cs1/subtitletranslatorapp/config/CorsConfig.java` — verify `CorsFilter`, `CorsConfiguration`, `UrlBasedCorsConfigurationSource` APIs are unchanged or update accordingly. **No-op — CORS API unchanged in Spring Framework 7.**
- [X] T008 [P] [US1] Resolve any exception-handling class changes in `backend/src/main/java/org/k3cs1/subtitletranslatorapp/exception/GlobalExceptionHandler.java` — check `MissingServletRequestPartException` and `MissingServletRequestParameterException` package paths remain valid. **No-op — exception handler package paths unchanged.**
- [X] T009 [P] [US1] Resolve any multipart or web MVC breaking changes in `backend/src/main/java/org/k3cs1/subtitletranslatorapp/controller/TranslationJobController.java` — verify `MultipartFile`, `MultipartException` imports are unchanged. **No-op — multipart API unchanged.**

### Fix configuration for User Story 1

- [X] T010 [US1] Verify and fix all `application.yml` property paths in `backend/src/main/resources/application.yml` that may have been renamed in Spring Boot 4.1.0. **Fixed: removed `.options` segment from Spring AI 2.0 properties (`spring.ai.openai.chat.options.model` → `spring.ai.openai.chat.model`, same for `temperature`). All other paths unchanged.**

### Write smoke tests for User Story 1

- [X] T011 [P] [US1] Write application context smoke test in `backend/src/test/java/org/k3cs1/subtitletranslatorapp/SubtitleTranslatorAppSmokeTest.java` — use `@SpringBootTest` with `spring.ai.openai.api-key=test` property override; assert context loads without errors. **Done — passes.**
- [X] T012 [P] [US1] Write controller smoke tests in `backend/src/test/java/org/k3cs1/subtitletranslatorapp/controller/TranslationJobControllerSmokeTest.java` — use `@WebMvcTest` (Boot 4 package: `org.springframework.boot.webmvc.test.autoconfigure`) with `@MockitoBean` for `TranslationJobService` and `TranslationJobStore`; required adding `spring-boot-starter-webmvc-test` test dependency. **Done — 2/2 tests pass.**

### Validate User Story 1

- [X] T013 [US1] Run full test suite `.\mvnw.cmd -pl backend test` and confirm zero failures across all 5 test classes (3 existing unit tests + 2 new smoke tests); fix any regressions before proceeding. **11/11 tests pass, 0 failures, 0 errors.**

**Checkpoint**: User Story 1 complete — backend compiles, all tests pass, application starts cleanly.

---

## Phase 4: User Story 2 — All Existing REST API Contracts Remain Intact (Priority: P2)

**Goal**: Every documented endpoint returns the correct HTTP status and `ApiResponse<T>` envelope structure per `specs/001-springboot4-upgrade/contracts/rest-api.md`.

**Independent Test**: Execute quickstart.md Steps 4–7 against a running local backend.

- [ ] T014 [US2] Start backend locally (`$env:OPENAI_API_KEY="<key>"` then `.\mvnw.cmd -pl backend spring-boot:run`) and validate `GET /api/reference/countries` returns HTTP 200 with `{"result":"SUCCESS","data":[{"code":"...","name":"..."},...]}` per quickstart.md Step 5
- [ ] T015 [US2] Validate `POST /api/translation-jobs` with `backend/src/main/resources/sample.srt` and `targetLanguage=Hungarian` returns HTTP 202 with `{"result":"SUCCESS","data":{"jobId":"<uuid>","message":"..."}}` per quickstart.md Step 6; poll `GET /api/translation-jobs/{jobId}` until `COMPLETED`
- [ ] T016 [US2] Validate error handling: `POST /api/translation-jobs` without a file part returns HTTP 400 with `{"result":"ERROR","message":"<user-friendly text>","data":null}` — confirm no Java class names or stack traces appear in the response body per quickstart.md Step 7

**Checkpoint**: User Story 2 complete — all API contracts verified against the upgraded backend.

---

## Phase 5: User Story 3 — Docker Image Builds and Deploys Successfully (Priority: P3)

**Goal**: Docker image builds, container starts, and GitHub Actions CI pipeline goes green.

**Independent Test**: Execute quickstart.md Steps 9–10.

- [X] T017 [US3] Review `backend/Dockerfile` for Spring Boot 4.1.0 compatibility: verify `eclipse-temurin:25-jre` image tag is still available on Docker Hub; update tag or base image if the tag has been superseded or has CVE advisories. **No change needed — `eclipse-temurin:25-jre` is current on Docker Hub (updated 2 days ago), no CVE advisories.**
- [ ] T018 [US3] Build the production artifact and Docker image per quickstart.md Step 9: `.\mvnw.cmd clean package -DskipTests`, copy JAR to `backend/app.jar`, run `docker build -t subtitle-translator-backend:4.1.0 backend/`; confirm exit code 0
- [ ] T019 [US3] Run Docker container locally: `docker run --rm -e OPENAI_API_KEY=test -e PORT=5000 -p 5000:5000 subtitle-translator-backend:4.1.0`; verify `Started SubtitleTranslatorApp` log line appears without ERROR lines; stop container
- [ ] T020 [US3] Push changes to `main` (or trigger `workflow_dispatch`) on `.github/workflows/deploy-backend-eb.yml`; verify all workflow steps complete with green status; confirm Elastic Beanstalk environment health returns to "Ok"

**Checkpoint**: All user stories complete — backend upgraded, contracts intact, Docker and CI validated.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation updates and final consistency checks.

- [X] T021 [P] Update `specs/001-springboot4-upgrade/data-model.md` with confirmed final property paths and Spring AI version after all validation is complete. **Done — updated with confirmed versions, dependency changes, Jackson 3 migration notes, and Spring AI property renames.**
- [X] T022 [P] Update `README.md` if any environment variable names, build commands, or configuration requirements changed between Spring Boot 3 and 4. **Done — added JDK 25 prerequisite section and `$env:JAVA_HOME` setup note for command-line builds.**

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on T001 (blocker gate) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion
  - T006–T009 can run in parallel (each targets a different source file)
  - T010 can run in parallel with T006–T009
  - T011–T012 can run in parallel with T006–T010 (test files are separate)
  - T013 depends on T006–T012 all being complete
- **User Story 2 (Phase 4)**: Depends on T013 (clean test run)
  - T014–T016 are sequential (each requires the running backend)
- **User Story 3 (Phase 5)**: Depends on T016 (all contracts verified)
  - T017 can run in parallel with T014–T016
  - T018 depends on T017
  - T019 depends on T018
  - T020 depends on T019
- **Polish (Phase 6)**: Depends on all user stories being complete

### Within Phase 3 (parallel opportunities)

```text
T005 (compile errors inventory)
  ├── T006 [P] SrtTranslatorServiceImpl.java
  ├── T007 [P] CorsConfig.java
  ├── T008 [P] GlobalExceptionHandler.java
  ├── T009 [P] TranslationJobController.java
  ├── T010    application.yml
  ├── T011 [P] SubtitleTranslatorAppSmokeTest.java
  └── T012 [P] TranslationJobControllerSmokeTest.java
        ↓ (all above complete)
      T013 (run full test suite)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Blocker gate check (T001)
2. Complete Phase 2: POM version bump + compile error inventory (T002–T005)
3. Complete Phase 3: Fix compilation, write smoke tests, validate build (T006–T013)
4. **STOP and VALIDATE**: `.\mvnw.cmd -pl backend test` all green
5. **Optionally demo**: backend starts locally on Spring Boot 4.1.0

### Incremental Delivery

1. Phase 1 + 2 → POM updated; compile errors known
2. Phase 3 → Clean build + all tests pass (MVP!)
3. Phase 4 → API contracts confirmed intact
4. Phase 5 → Docker + CI green; feature complete
5. Phase 6 → Documentation updated

---

## Notes

- [P] tasks = operate on different files; no compile-state dependency
- T001 is a strict HARD BLOCKER — do not write any code until it passes
- T005 is diagnostic — its output drives which of T006–T009 need actual changes (some may be no-ops if no breaking changes affect that file)
- T006–T009 may all be no-ops if Spring Boot 4 / Spring Framework 7 introduce no breaking changes for the APIs used; mark [X] with a note if no changes were needed
- T014–T016 require a live backend and a valid `OPENAI_API_KEY`
- T020 requires appropriate AWS secrets configured in the GitHub repository
