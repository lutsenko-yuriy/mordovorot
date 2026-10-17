# Code Style

## Base standard

Kotlin: [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
Python: [PEP 8](https://peps.python.org/pep-0008/)
Shell: [Google Shell Style Guide](https://google.github.io/styleguide/shellguide.html)

## Formatting

No formatter configured; CI enforces none. Use IntelliJ's built-in "Kotlin coding conventions" code style when reformatting, and commit formatting separately with a `style:` prefix.

## Linting

None configured for any language (no ktlint/detekt, ruff, or shellcheck).

## Naming and structure

- Packages follow MVVM layers: `board_model`, `viewmodel`, `view` (+ `view/tui`), plus `storage` and `analytics`
- Interface + `Impl` pairs (`BoardModel`/`BoardImpl`, `ViewModel`/`ViewModelImpl`)
- Tests mirror the main package path, named class + aspect under test (e.g. `BoardImplShiftTest.kt`); fakes (`Fake*`, `Recording*`) of a shared-module type go in the `:shared:testing` module (`shared/testing/src/main/kotlin/testing/`), so every module's tests can use them; app-only fakes go in `src/test/kotlin/testing/`
- Aim to keep source files under ~300 lines. This is a guideline, not a hard limit: a file over it
  is a split candidate worth a look (split it, or note why not in a follow-up ticket), not a rule
  violation.

## Comments

Explain why, not what. If a comment is longer than the method or property it describes, try
to shorten it, but keeping its meaning matters more than brevity.
