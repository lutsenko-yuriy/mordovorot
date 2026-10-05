# Project Config

Read this file to resolve all project-specific constants referenced in skill instructions.
Project identity (name, issue prefix, git host, PM tool, test/version settings) lives in `skill_router.toml`
(`[project]`, `[providers]`). Skills stay unchanged.

## Tech stack

Languages, frameworks, platforms and tooling: `docs/TECH_STACK.md`.

## Project management

@skills/shared/pm-tool-mapping.md

## Documentation paths

| Document | Path |
|---|---|
| Product spec | `docs/PRODUCT_SPEC.md` |
| Glossary | `docs/GLOSSARY.md` |
| Backlog | `docs/BACKLOG.md` |
| Changelog | `docs/CHANGELOG.md` |
| Architecture | `docs/ARCHITECTURE.md` |
| Agent workflow | `AGENTS.md` |
| Knowledge base | `docs/knowledge/notes/` (one `GH-XX.md` file per ticket) |

## Testing

| Setting | Value |
|---|---|
| Integration test directory | `src/test/kotlin/` (unit tests only — no separate integration suite yet) |
| Test harness file | N/A — no scripted end-to-end harness; `src/test/kotlin/testing/` holds shared fakes (`FakeBoardModel`, `FakeView`) |
| Harness class / entry point | N/A |
| Unit / integration test command | `./gradlew test` (or `./gradlew build` to also compile + assemble) |

## Version management

| Setting | Value |
|---|---|
| Version file | None yet — no manifest/version file exists; track releases via `docs/CHANGELOG.md` and git tags until one is added |
| Version field | N/A |
| Manual vs automated | Manual — CI (`.github/workflows/build.yml`) runs `./gradlew build` on push/PR but does not bump versions |

## In QA path patterns

A merged PR moves to **In QA** (not Done directly) if it touches any of:

- `src/main/kotlin/view/` — console I/O behavior is hard to cover with automated tests; a human should play a session before sign-off

Move straight to **Done** if the PR touches only: pure logic with no runtime platform dependency, documentation, CI config, or pure refactors where automated tests fully own correctness. When in doubt, use **In QA**.
