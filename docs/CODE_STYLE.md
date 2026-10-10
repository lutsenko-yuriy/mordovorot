# Code Style

## Base standard

Kotlin: [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
Python: [PEP 8](https://peps.python.org/pep-0008/)
Shell: [Google Shell Style Guide](https://google.github.io/styleguide/shellguide.html)

## Formatting

No formatter configured; CI enforces none. Use the IntelliJ Kotlin official style (`kotlin.code.style=official`) when reformatting, and commit formatting separately with a `style:` prefix.

## Linting

None configured for any language (no ktlint/detekt, ruff, or shellcheck).

## Naming and structure

- Packages follow MVVM layers: `board_model`, `viewmodel`, `view` (+ `view/tui`), plus `storage` and `analytics`
- Interface + `Impl` pairs (`BoardModel`/`BoardImpl`, `ViewModel`/`ViewModelImpl`, `View`/`ViewImpl`)
- Tests mirror the main package path, named class + aspect under test (e.g. `BoardImplShiftTest.kt`); shared fakes go in `src/test/kotlin/testing/` (`Fake*`, `Recording*`)

## Comments

Explain why, not what. Keep new comments to one concise line, even though some older code uses denser comments.
