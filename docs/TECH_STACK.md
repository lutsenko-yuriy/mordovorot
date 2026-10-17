# Tech Stack

## Languages

| Language | Version | Used for |
|---|---|---|
| Kotlin | 2.4.21 (`gradle/libs.versions.toml`), JVM toolchain 17 | Game application code and tests |
| Python | 3.11+ (3.9 for `probe`; CI runs 3.9 and 3.12 in `.github/workflows/tests.yml`) | Agent workflow tooling under `scripts/` |
| Shell | bash | Claude Code hooks: onboarding gate (`scripts/onboard/gate.sh`), session-waiting alert (`scripts/alert/alert.sh`, GH-107) |

## Frameworks and libraries

- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) 1.11.0: ViewModel ↔ View request channel
- [kotlin.test](https://kotlinlang.org/api/latest/kotlin.test/) on JUnit 5

## Platforms

- JVM 17 desktop terminal app: an ANSI mouse/keyboard TUI that needs a real terminal (`installDist` launcher, not `./gradlew run`)

## Tooling

- **Build / package manager:** Gradle 9.7.1 (wrapper), Kotlin JVM + `application` plugins for the
  terminal app, Kotlin Multiplatform for the `shared/` modules (GH-41: `jvm()` + `js { nodejs() }`,
  GH-103; the Kotlin Gradle plugin downloads Node.js and yarn, `kotlin-js-store/yarn.lock` pins
  the npm packages; a Kotlin upgrade that changes them fails the build until
  `./gradlew kotlinUpgradeYarnLock` is run and the new lock committed)
- **Test runner:** `./gradlew check` (JUnit Platform) - runs the root `test` and every shared
  module's `jvmTest`; plain `./gradlew test` skips the shared modules; Python: `python -m unittest`
- **Linter / formatter:** none configured
- **Agent alert hook (GH-107):** `scripts/alert/alert.sh` runs on Claude Code's `Notification` and `Stop`
  hooks and alerts by macOS notification and `say`. Per-machine toggles go in the gitignored
  `.claude/alert.env` (`YAB_ALERT_NOTIFY`, `YAB_ALERT_SPEAK`, `YAB_ALERT_SUPPRESS_FOCUSED`, all `on` by
  default; set both channels `off` to opt out). The file is per checkout, so an opt-out does not
  carry over to worktrees or fresh clones. Needs `jq`; does nothing off macOS
- **CI:** GitHub Actions: `build.yml` (`./gradlew build`), `tests.yml` (onboard and alert Python tests)
