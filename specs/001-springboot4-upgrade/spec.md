# Feature Specification: Spring Boot 4.1.0 Upgrade

**Feature Branch**: `001-springboot4-upgrade`

**Created**: 2026-06-22

**Status**: Draft

**Input**: User description: "Upgrade the backend to Spring Boot 4.1.0 version"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Backend Compiles and Starts on Spring Boot 4.1.0 (Priority: P1)

A developer upgrades the root `pom.xml` parent to Spring Boot 4.1.0, updates all
dependent library versions to compatible releases, resolves any breaking-change
compilation errors, and confirms the backend starts cleanly with no startup errors.

**Why this priority**: Without a clean build and startup, no other aspect of the upgrade
can be validated. This is the foundational gate for all remaining stories.

**Independent Test**: Run `./mvnw clean package` from the repository root and then start
the backend with `./mvnw -pl backend spring-boot:run`. A clean build log and a
"Started … in … seconds" log line confirm success.

**Acceptance Scenarios**:

1. **Given** the pom.xml has been updated to declare Spring Boot 4.1.0 as the parent,
   **When** `./mvnw clean package` is executed,
   **Then** the build completes with exit code 0 and zero compilation errors.

2. **Given** a successful build artifact,
   **When** the backend is started locally,
   **Then** the application starts without any `ERROR` or `WARN` lines related to
   misconfigured beans, missing auto-configurations, or deprecated removed APIs.

---

### User Story 2 - All Existing REST API Contracts Remain Intact (Priority: P2)

After the upgrade, every existing API endpoint (`GET /api/reference/countries` and
`POST /api/translation-jobs`) continues to accept the same request format and return
the same `ApiResponse<T>` envelope structure as before the upgrade.

**Why this priority**: The UI and any external consumers depend on stable API contracts.
A regression here directly breaks the end-to-end user workflow.

**Independent Test**: Send the two `curl` commands documented in the README against a
locally running upgraded backend and verify the response structure and HTTP status codes
are unchanged.

**Acceptance Scenarios**:

1. **Given** the upgraded backend is running,
   **When** `GET /api/reference/countries` is called,
   **Then** the response is HTTP 200 with body `{ "result": "SUCCESS", "data": [...] }`
   where each item contains `code` and `name` fields.

2. **Given** the upgraded backend is running and a valid `.srt` file is available,
   **When** `POST /api/translation-jobs` is called with a valid `.srt` file and a
   `targetLanguage` field,
   **Then** the response is HTTP 200 with body `{ "result": "SUCCESS", "data": { ... } }`
   containing the translated content encoded in base64.

3. **Given** the upgraded backend is running,
   **When** an invalid request is submitted to any endpoint,
   **Then** the response is an appropriate 4xx status with
   `{ "result": "ERROR", "message": "..." }` and no internal stack trace is exposed.

---

### User Story 3 - Docker Image Builds and Deploys Successfully (Priority: P3)

The GitHub Actions CI/CD pipeline builds a Docker image from the upgraded backend and
deploys it to AWS Elastic Beanstalk without errors.

**Why this priority**: Deployment is required for production use. However, it depends on
Stories 1 and 2 being complete, so it is the last validation gate.

**Independent Test**: Trigger the `deploy-backend-eb.yml` workflow manually via
`workflow_dispatch` and confirm the workflow completes with a green status.

**Acceptance Scenarios**:

1. **Given** the upgraded source code is pushed to `main`,
   **When** the `deploy-backend-eb.yml` GitHub Actions workflow runs,
   **Then** the Docker image build step completes without errors.

2. **Given** a successfully built Docker image,
   **When** the deployment step pushes the image to Elastic Beanstalk,
   **Then** the environment health status returns to "Ok" within a reasonable time.

---

### Edge Cases

- What happens if Spring AI does not yet publish a BOM version compatible with
  Spring Boot 4.1.0? The upgrade is a hard blocker and MUST be paused entirely
  until Spring AI publishes a compatible release. No workarounds are in scope.
- What happens if Spring Boot 4.1.0 has removed auto-configuration classes that
  the current code relies on (e.g., for web MVC, error handling, or actuator)?
  Each removed API MUST be replaced with the Spring Boot 4 equivalent.
- What happens if Lombok's annotation processor is incompatible with the Java 25 +
  Spring Boot 4.1.0 combination? A compatible Lombok version MUST be sourced before
  proceeding.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The root `pom.xml` MUST declare `spring-boot-starter-parent` version
  `4.1.0` as the parent.
- **FR-002**: All third-party dependency versions managed via the Spring Boot BOM MUST
  be verified for compatibility with Spring Boot 4.1.0 and updated if necessary.
- **FR-003**: The `spring-ai-bom` version MUST be updated to the latest release that
  is compatible with Spring Boot 4.1.0; the `spring-ai-starter-model-openai` dependency
  MUST remain functional. If no compatible Spring AI release exists at implementation
  time, the upgrade MUST be treated as a hard blocker and paused until one is published.
- **FR-004**: The Lombok version MUST be updated to a release that supports the
  Java 25 + Spring Boot 4.1.0 toolchain combination.
- **FR-005**: All compilation errors introduced by Spring Boot 4.1.0 breaking changes
  (e.g., removed or renamed classes, changed annotations) MUST be resolved.
- **FR-006**: The `GlobalExceptionHandler` and `ApiResponse` error-handling mechanism
  MUST continue to work correctly under Spring Boot 4.1.0's exception handling model.
- **FR-007**: A minimal smoke test suite MUST be written as part of this upgrade using
  `spring-boot-starter-test`. It MUST include: (a) an application context load test
  confirming all beans initialise without errors, and (b) happy-path HTTP status checks
  for `GET /api/reference/countries` and `POST /api/translation-jobs`. All tests MUST
  pass with zero failures after the upgrade.
- **FR-008**: The `Dockerfile` MUST be explicitly reviewed and updated as part of this
  upgrade to ensure compatibility with Spring Boot 4.1.0 (e.g., base JRE image version,
  JAR layering configuration). The resulting Docker image MUST build and run successfully
  with the upgraded artifact before the deployment pipeline is triggered.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The backend project builds successfully from a clean checkout with zero
  compilation errors, and the newly written smoke test suite passes with zero failures.
- **SC-002**: All documented API endpoints return correct responses with unchanged
  contract structure after the upgrade.
- **SC-003**: The backend starts and is ready to serve requests within the same time
  window as the pre-upgrade baseline (no significant startup regression).
- **SC-004**: The GitHub Actions deployment pipeline completes end-to-end with a green
  status after the upgrade is merged to `main`.
- **SC-005**: No internal error details (stack traces, class names) are exposed in any
  error response, confirming the `GlobalExceptionHandler` remains operative.

## Clarifications

### Session 2026-06-22

- Q: If Spring AI has no published version compatible with Spring Boot 4.1.0 at implementation time, what is the required course of action? → A: Hard blocker — pause the upgrade entirely until Spring AI publishes a compatible release.
- Q: Should writing a basic smoke test suite be in scope for this upgrade? → A: Yes, in scope — write minimal smoke tests (application context loads + endpoint happy-path HTTP status checks) as part of this upgrade.
- Q: Should reviewing and updating the Dockerfile be an explicit named task? → A: Yes, explicit task — add a dedicated requirement to review and update the Dockerfile for Spring Boot 4 compatibility before running the CI pipeline.

## Assumptions

- Spring Boot 4.1.0 is publicly released and available in Maven Central at the time of
  implementation.
- If no compatible Spring AI BOM version for Spring Boot 4.1.0 exists at implementation
  time, the upgrade is a hard blocker and MUST NOT proceed until one is published.
- The project's use of Java 25 meets or exceeds Spring Boot 4.1.0's minimum Java
  version requirement (expected to be Java 21+).
- The `Dockerfile` will be explicitly reviewed and updated as needed; it is not assumed
  to be compatible without verification.
- No new Spring Boot 4 features need to be adopted as part of this upgrade — the scope
  is limited to achieving a clean, fully functional upgrade with no regressions.
- The UI module and its `frontend-maven-plugin` build are unaffected by this upgrade.
