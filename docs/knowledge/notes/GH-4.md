---
bookmarks: []
---

# GH-4: Automated test coverage for board_model and presenter

## Notes

### 2026-09-10

**Pre-existing infinite-loop-on-EOF bug in PresenterImpl.play()**

While manually verifying `./gradlew run` still worked after the Gradle migration, feeding it exhausted stdin (piped input that ran out) caused an infinite loop and eventual `OutOfMemoryError`. Root cause: `play()`'s `catch (e: Exception)` swallows *any* exception from `view.processCommand()`, including `NoSuchElementException` from `Scanner.next()` on exhausted input, and just loops again — `Scanner` doesn't block once stdin is closed, so it spins hot forever. Not introduced by GH-4 (same behavior existed before, under plain `kotlinc`); out of scope here since `view`/`ViewImpl` coverage is explicitly deferred. Worth a follow-up ticket: distinguish "no more input" from a recoverable per-command error.

## Debrief summary
