# Tech Stack

## Languages

| Language | Version | Used for |
|---|---|---|
| Kotlin | 1.9.24 (`build.gradle.kts`), JVM toolchain 17 | Game application code and tests |
| Python | 3.11+ (3.9 for `probe`; CI runs 3.9 and 3.12 in `.github/workflows/tests.yml`) | Agent workflow tooling under `scripts/` |
| Shell | bash | Onboarding gate hook (`scripts/onboard/gate.sh`) |

## Frameworks and libraries

- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) 1.8.1: ViewModel ↔ View request channel
- [kotlin.test](https://kotlinlang.org/api/latest/kotlin.test/) on JUnit 5

## Platforms

- JVM 17 desktop terminal app: an ANSI mouse/keyboard TUI that needs a real terminal (`installDist` launcher, not `./gradlew run`)

## Tooling

- **Build / package manager:** Gradle 8.7 (wrapper), Kotlin JVM + `application` plugins for the
  terminal app, Kotlin Multiplatform for the `shared/` modules (GH-41: `jvm()` + a compile-only
  `js(IR)`)
- **Test runner:** `./gradlew check` (JUnit Platform) - runs the root `test` and every shared
  module's `jvmTest`; plain `./gradlew test` skips the shared modules; Python: `python -m unittest`
- **Linter / formatter:** none configured
- **CI:** GitHub Actions: `build.yml` (`./gradlew build`), `tests.yml` (onboard Python tests)
