# Research: Spring Boot 4.1.0 Upgrade

**Date**: 2026-06-22
**Feature**: Spring Boot 3.5.15 → 4.1.0 backend upgrade

---

## 1. Spring Boot 4 Prerequisites

**Decision**: Confirm Java 21+ is required and the project already meets it.

**Rationale**: Spring Boot 4 requires Java 21 as the minimum JVM version (aligned with
Spring Framework 7's baseline). This project already uses Java 25, so no JDK change
is needed.

**Alternatives considered**: Downgrading to Spring Boot 3.x to avoid prerequisite
issues — rejected because the goal is Spring Boot 4.1.0.

---

## 2. Spring Framework 7 Breaking Changes Relevant to This Codebase

**Decision**: The following areas require code review and possible changes at
implementation time.

### 2a. Jakarta EE namespace

**Status**: Already migrated. The codebase uses `jakarta.*` imports (`jakarta.annotation.PostConstruct`). Spring Framework 7 continues on the `jakarta.*` namespace. No changes needed.

### 2b. `CorsFilter` and CORS configuration

**Status**: Review required. The current `CorsConfig.java` registers a `CorsFilter`
bean using `org.springframework.web.filter.CorsFilter` and
`org.springframework.web.cors.CorsConfiguration` / `UrlBasedCorsConfigurationSource`.
These classes exist in Spring Framework 6.x and are expected to remain in Spring
Framework 7, but their configuration semantics and auto-configuration interaction may
change. The `CorsFilter` bean registration approach (vs. `WebMvcConfigurer.addCorsMappings`)
should be re-validated after bumping the version.

**Alternatives considered**: Switching to `WebMvcConfigurer` CORS registration — deferred
unless `CorsFilter` approach causes issues.

### 2c. Multipart configuration properties

**Status**: Verify at implementation time. The current `application.yml` uses:
```yaml
spring.servlet.multipart.max-file-size: 2MB
spring.servlet.multipart.max-request-size: 3MB
```
Spring Boot 4 may rename or reorganize servlet-related properties. The Tomcat-specific
properties (`server.tomcat.max-http-form-post-size`, `server.tomcat.max-swallow-size`)
should also be verified.

### 2d. Exception handling classes

**Status**: Low risk; verify at implementation time.
`MissingServletRequestPartException` (from `org.springframework.web.multipart.support`)
and `MultipartException` are standard Spring MVC classes. Their package locations should
be unchanged but must be confirmed after the version bump.

### 2e. `RestClient` API

**Status**: No change expected. `RestClient` was introduced in Spring 6.1 and is
forward-compatible. The `DeeplTranslatorServiceImpl` usage should compile without changes.

### 2f. Spring AI `ChatClient` API

**Status**: This is the highest-risk dependency. See section 3 below.

---

## 3. Spring AI Compatibility

**Decision**: A compatible Spring AI release for Spring Boot 4.1.0 MUST be confirmed
before any implementation work begins. If absent, the upgrade is a hard blocker.

**Rationale**: The codebase uses Spring AI `ChatClient.Builder` and related OpenAI
starter classes. Spring AI 1.x targets Spring Boot 3.x. Spring Boot 4 (Spring Framework 7)
will require a new Spring AI major version (expected: Spring AI 2.x). The Spring AI BOM
artifact ID and version in `pom.xml` must be updated to the compatible release once
confirmed available.

**Research task at implementation time**:
1. Check [Spring AI releases](https://github.com/spring-projects/spring-ai/releases) for
   a version declaring Spring Boot 4 / Spring Framework 7 compatibility.
2. Update `spring-ai.version` property and the `spring-ai-bom` coordinates accordingly.
3. Verify `spring-ai-starter-model-openai` artifact ID has not changed.
4. Re-test `SrtTranslatorServiceImpl` — the `ChatClient` API surface may have minor changes
   in Spring AI 2.x (e.g., builder method renames, response type changes).

**Alternatives considered**: Pinning Spring AI 1.x with explicit Spring Framework 7
exclusions — rejected as fragile and unsupported.

---

## 4. Lombok Compatibility

**Decision**: Verify Lombok works with the Java 25 + Spring Boot 4.1.0 + Spring
Framework 7 annotation processor toolchain. Upgrade Lombok if needed.

**Current version**: `1.18.42`

**Rationale**: Lombok uses annotation processing that can be sensitive to Java version
changes. Lombok 1.18.36+ added support for Java 25 features. Since 1.18.42 is already
above that, it is likely compatible, but a build-time check is required after the Spring
Boot version bump.

**Research task at implementation time**: Run `./mvnw clean package` after bumping
Spring Boot — if Lombok annotation processor emits errors or warnings about unsupported
Java version, upgrade to the latest stable Lombok release.

---

## 5. Dockerfile Base Image

**Decision**: The current `eclipse-temurin:25-jre` base image is already Java 25 and
therefore compatible with Spring Boot 4's Java 21+ requirement. No change is required
unless `eclipse-temurin:25-jre` is deprecated at implementation time.

**Rationale**: Spring Boot 4 does not prescribe a specific Docker base image. The
`eclipse-temurin` family is the standard successor to `adoptopenjdk` and is actively
maintained for all LTS and current Java versions.

**Research task at implementation time**: Verify `eclipse-temurin:25-jre` is still
available on Docker Hub and has no CVE advisories that would require a tag update.

---

## 6. Smoke Test Strategy

**Decision**: Two new test classes:

1. `SubtitleTranslatorAppSmokeTest` — `@SpringBootTest` with Spring AI
   auto-configuration mocked out (to avoid requiring a live OpenAI key) and
   `OPENAI_API_KEY` supplied as a placeholder property. Asserts the application context
   loads successfully.

2. `TranslationJobControllerSmokeTest` — `@WebMvcTest(TranslationJobController.class)`
   with mocked service layer. Asserts:
   - `POST /api/translation-jobs` without a file returns HTTP 400 (not 500).
   - `GET /api/translation-jobs/{id}` with unknown ID returns HTTP 404.
   - `GET /api/reference/countries` smoke path (via `ReferenceDataController` smoke test
     or a separate class) returns HTTP 200 or proxied response.

**Rationale**: These tests confirm the Spring Boot 4 application context wires correctly
and the MVC layer dispatches requests as expected, without requiring external services.

**Alternatives considered**: Full integration tests with `TestRestTemplate` — deferred
to a later feature; too complex for the scope of a version upgrade.

---

## 7. Property Namespace Verification Checklist

Verify the following `application.yml` keys remain valid under Spring Boot 4.1.0:

| Property | Risk | Action |
|----------|------|--------|
| `spring.ai.openai.api-key` | Medium — Spring AI 2.x may rename | Confirm against new Spring AI docs |
| `spring.ai.openai.chat.options.model` | Medium | Confirm against new Spring AI docs |
| `spring.servlet.multipart.max-file-size` | Low | Verify unchanged |
| `spring.servlet.multipart.max-request-size` | Low | Verify unchanged |
| `server.tomcat.max-http-form-post-size` | Low | Verify unchanged |
| `server.tomcat.max-swallow-size` | Low | Verify unchanged |
| `deepl.base-url` / `deepl.auth-key` | None — custom properties | No change |
| `translation.*` | None — custom properties | No change |
