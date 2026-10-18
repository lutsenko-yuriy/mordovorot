# Tech Stack

## Languages

| Language | Version | Used for |
|---|---|---|
| Kotlin | 2.4.21 (`gradle/libs.versions.toml`), JVM toolchain 17 | Game application code and tests |
| Python | 3.11+ (3.9 for `probe`; CI runs 3.9 and 3.12 in `.github/workflows/tests.yml`) | Agent workflow tooling under `scripts/` |
| Shell | bash | Claude Code hooks: onboarding gate (`scripts/onboard/gate.sh`), session-waiting alert (`scripts/alert/alert.sh`, GH-107) |

## Frameworks and libraries

- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) 1.11.0: ViewModel ↔ View request channel
- [kotlin.test](https://kotlinlang.org/api/latest/kotlin.test/) on JUnit 5 (JVM) and Mocha (Node.js)
- [kotlinx-coroutines-test](https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test) 1.11.0: `runTest` for the shared modules' common tests (test only, GH-103)

## Platforms

- JVM 17 desktop terminal app: an ANSI mouse/keyboard TUI that needs a real terminal (`installDist` launcher, not `./gradlew run`)
- Web browser (GH-110): the `:web` Kotlin/JS client (plain DOM and CSS, no UI framework), served on `http://localhost:8080` by `./gradlew :web:serve -t` (the Kotlin plugin's webpack dev server under continuous build, so it rebuilds on change, GH-124; the port is pinned because browser saves are per origin)

## Tooling

- **Build / package manager:** Gradle 9.7.1 (wrapper), Kotlin JVM + `application` plugins for the
  terminal app, Kotlin Multiplatform for the `shared/` modules (GH-41: `jvm()` + `js { nodejs() }`,
  GH-103; the Kotlin Gradle plugin downloads Node.js and yarn, `kotlin-js-store/yarn.lock` pins
  the npm packages; a Kotlin upgrade that changes them fails the build until
  `./gradlew kotlinUpgradeYarnLock` is run and the new lock committed); the `:web` module (GH-110) adds
  `js { browser() }`, whose webpack, webpack-cli and webpack-dev-server are build-time npm packages in
  the same lock. `serve` keeps its outputs in each project's `build-serve/` (outside `build/`, so
  `clean` leaves it), and `gradle.properties` gives the shared Kotlin compile daemon 2 GB (`kotlin.daemon.jvmargs`), so
  `check`/`build` can run alongside it (GH-124)
- **Test runner:** `./gradlew check` (JUnit Platform) - runs the root `test` and every shared
  module's common tests on both the JVM (`jvmTest`) and Node.js (`jsNodeTest`, GH-103); plain `./gradlew test` skips the shared modules; Python: `python -m unittest`
- **Linter / formatter:** none configured
- **Agent alert hook (GH-107):** `scripts/alert/alert.sh` runs on Claude Code's `Notification` and `Stop`
  hooks and alerts by macOS notification and `say`. Per-machine toggles go in the gitignored
  `.claude/alert.env` (`YAB_ALERT_NOTIFY`, `YAB_ALERT_SPEAK`, `YAB_ALERT_SUPPRESS_FOCUSED`, all `on` by
  default; set both channels `off` to opt out). The file is per checkout, so an opt-out does not
  carry over to worktrees or fresh clones. Needs `jq`; does nothing off macOS
- **CI:** GitHub Actions: `build.yml` (`./gradlew build`), `tests.yml` (onboard and alert Python tests)
