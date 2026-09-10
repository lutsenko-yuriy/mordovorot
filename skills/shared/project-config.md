# Project Config

Read this file to resolve all project-specific constants referenced in skill instructions.
When setting up the project, fill in every `{{placeholder}}`. Skills stay unchanged.

## Source control

| Setting | Value |
|---|---|
| Git host | `GitHub` (e.g. GitHub, GitLab, Bitbucket) |

## Tech stack

| Layer | Technology |
|---|---|
| Framework | None — plain Kotlin, run via `kotlinc`/IDE, no build tool checked in yet |
| State management | In-memory (`BoardImpl` holds an `IntArray` board state) |
| Local persistence | None — state lives only for the process lifetime |

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
| Integration test directory | None yet — no test suite exists in the repo |
| Test harness file | N/A |
| Harness class / entry point | N/A |
| Unit / integration test command | None yet — add one (e.g. `kotlin.test` + Gradle) before relying on automated verification |

## Version management

| Setting | Value |
|---|---|
| Version file | None yet — no manifest/version file exists; track releases via `docs/CHANGELOG.md` and git tags until one is added |
| Version field | N/A |
| Manual vs automated | Manual — no CI is configured yet |

## In QA path patterns

A merged PR moves to **In QA** (not Done directly) if it touches any of:

- `src/view/` — console I/O behavior is hard to cover with automated tests; a human should play a session before sign-off

Move straight to **Done** if the PR touches only: pure logic with no runtime platform dependency, documentation, CI config, or pure refactors where automated tests fully own correctness. When in doubt, use **In QA**.
