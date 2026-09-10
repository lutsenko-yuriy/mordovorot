---
bookmarks: []
---

# GH-4: Automated test coverage for board_model and presenter

## Notes

### 2026-09-10

**Pre-existing infinite-loop-on-EOF bug in PresenterImpl.play()**

While manually verifying `./gradlew run` still worked after the Gradle migration, feeding it exhausted stdin (piped input that ran out) caused an infinite loop and eventual `OutOfMemoryError`. Root cause: `play()`'s `catch (e: Exception)` swallows *any* exception from `view.processCommand()`, including `NoSuchElementException` from `Scanner.next()` on exhausted input, and just loops again — `Scanner` doesn't block once stdin is closed, so it spins hot forever. Not introduced by GH-4 (same behavior existed before, under plain `kotlinc`); out of scope here since `view`/`ViewImpl` coverage is explicitly deferred. Worth a follow-up ticket: distinguish "no more input" from a recoverable per-command error.

**Review + audit findings from PR #5 (test coverage)**

Full `review` (architectural) and `audit` (runtime/migration) passes ran against PR #5. Summary, with resolution:

- 🔴 *Fixed:* `./gradlew run` — the command this PR's own README newly documents as "Run the game" — never wired stdin. The `application` plugin's `run` task doesn't connect `System.in` to the forked JVM by default, so `ViewImpl`'s `Scanner` hit EOF on the first read and `PresenterImpl.play()`'s catch-all swallowed the resulting `NoSuchElementException` and spun hot into an `OutOfMemoryError` — reproducible even with valid piped input, i.e. every invocation, not just the already-known exhausted-input case above. Fixed by explicitly setting `standardInput = System.\`in\`` on the `run` task in `build.gradle.kts`.
- 🟡 *Fixed:* `docs/PRODUCT_SPEC.md` and `skills/shared/project-config.md` still referenced `src/view/`, stale after the `src/main/kotlin/` move earlier in this same ticket.
- 🟡 *Fixed:* `skills/shared/project-config.md`, `AGENTS.md`, and `docs/VERSIONING.md`'s CI/CD section still said "no CI configured" after this PR added `.github/workflows/build.yml`.
- 🟢 *Fixed:* CI workflow had no dependency caching and no explicit `permissions` block — added `cache: gradle` and `permissions: { contents: read }`.
- 🟢 *Fixed:* only `shiftLeft` had a negative-index bounds test; added the same for `shiftRight`/`shiftUp`/`shiftDown` for symmetry (the guards are textually identical, so this was cheap, not a real coverage gap).
- ✅ Architectural review found no issues: layer directions preserved, `BoardModel.boardArray` is read-only on the interface (narrows rather than widens the existing exposure), fakes correctly isolated to `src/test/kotlin/testing/`, file moves confirmed as pure renames via diff similarity.

**Process note:** the audit caught a real, immediate-on-every-invocation bug (`./gradlew run` was completely non-functional) that my own manual smoke test before opening the PR missed — I'd attributed the OOM I saw locally entirely to the *already-known* exhausted-stdin bug, when actually stdin wasn't being forwarded at all. Worth remembering: a smoke test that reproduces a known-bug symptom doesn't rule out a *second*, unrelated cause producing the same symptom — should have isolated the two before assuming which bug I was looking at.

**Unwanted LICENSE file (see also [[GH-2-adjust-1]])**

Also flagged and removed during this ticket: the `yuriys-agentic-boyz` template had shipped a `LICENSE` file at the repo root during GH-2's adoption, which wasn't something the project owner asked for. Removed directly on `master`. Full finding and the framework-setup suggestion (ask explicitly about licensing during template setup instead of silently including a default) is logged in `docs/knowledge/notes/GH-2-adjust-1.md`.

## Debrief summary
