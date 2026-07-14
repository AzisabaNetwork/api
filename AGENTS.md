# Repository Guidelines

## Project Structure & Module Organization

This is a Kotlin/JVM Gradle multi-project repository targeting Java 17. `server/` contains the Ktor HTTP/WebSocket application, routes, authentication, storage, and integrations. `common/` holds shared data models, serializers, and SQL utilities. `spigot/` and `velocity/` provide Minecraft platform plugins; `spigot-common/`, `spigot-mythic4_12/`, and `spigot-mythic5_12/` isolate shared and version-specific MythicMobs support. Production code belongs under each module's `src/main/kotlin`; resources belong in `src/main/resources`. Tests mirror packages under `src/test/kotlin`. Keep the public API contract in `swagger.yaml` synchronized with route changes.

## Build, Test, and Development Commands

Use the checked-in wrapper so every contributor runs the repository's Gradle version. On Windows, replace `./gradlew` with `.\gradlew.bat`.

- `./gradlew build` — compile all modules and run verification tasks.
- `./gradlew test` — run all configured unit tests.
- `./gradlew :server:run` — start the Ktor server on port 8080.
- `./gradlew shadowJar` — create deployable shaded JARs in each module's `build/libs/`.
- `docker build -t azisaba-api .` — build the server container locally.

## Coding Style & Naming Conventions

Follow Kotlin's official style (`kotlin.code.style=official`) with four-space indentation and trailing commas in multiline declarations. Use `UpperCamelCase` for classes and objects, `lowerCamelCase` for functions and properties, and package names under `net.azisaba.api`. Match existing role-based names such as `RoutePlayers`, `DatabaseManager`, and `UploadMythicMobsTask`. Keep platform-specific behavior in its platform module and reusable contracts in `common` or `spigot-common`.

## Testing Guidelines

Tests use Kotlin Test/JUnit with Ktor's server test utilities. Name test classes `*Test` and write focused test methods describing observable behavior. Add route tests under `server/src/test/kotlin` and module tests beside the code they cover. The current sample test is disabled, so contributors must add active regression tests for new behavior and run `./gradlew test` before submitting.

## Commit & Pull Request Guidelines

Recent history generally uses Conventional Commit prefixes such as `feat:`, `fix:`, and `chore:`; write concise, imperative subjects. Keep commits scoped to one logical change. Pull requests should explain the motivation, affected modules, configuration or API-contract changes, and verification commands. Link relevant issues; include request/response examples for API changes and screenshots only for user-visible changes.

## Security & Configuration

Never commit `config.yml`, `interchat.yml`, API keys, database credentials, or webhook secrets. These runtime files are ignored. Use local placeholders, and document newly required configuration fields in the pull request.
