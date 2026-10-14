# Checkup ledger

Tracks due status and open findings for the two-tier periodic code-quality checkup defined in [ADR-0001](../decisions/ADR-0001-two-tier-periodic-code-quality-checkup.md). `scripts/checkup/due.py` reads the **Cadence & due status** table below to report which tier(s) are due; the `/checkup` skill updates this file after each run.

## Cadence & due status

Each tier is tracked as "not yet done this period" — a period label, not an exact-day match — so it stays flagged as overdue until actually run (see ADR-0001). `Period covered` is the period label (`YYYY-MM` for light, `YYYY-Qn` for heavy) of the most recently completed run.

| Tier | Cadence | Last run | Period covered | Next due |
|---|---|---|---|---|
| Light | 1st of every calendar month | 2026-10-14 | 2026-10 | 2026-11-01 |
| Heavy | 14th of Jan/Apr/Jul/Oct | 2026-10-14 | 2026-Q4 | 2027-01-14 |

## Open findings

Findings needing human decision or larger effort, each carrying an explicit deadline — no individual PM ticket is filed at checkup time; a scheduled finding links its ticket as `→ GH-XX` (per ADR-0001 and its 2026-10-14 amendment). Classified using [Fowler's Technical Debt Quadrant](https://martinfowler.com/bliki/TechnicalDebtQuadrant.html) before being written up.

| ID | Opened | Tier | Dimension | Debt quadrant | Summary | Deadline | Write-up |
|---|---|---|---|---|---|---|---|
| CHK-2026-10-14-light-1 | 2026-10-14 | light | Unused functionality | prudent-inadvertent | `BoardImpl.counter` is write-only (reset, never incremented/read outside 3 tests) — repurpose in GH-62 or remove → GH-62 | 2026-11-14 | [CHK-2026-10-14-light](CHK-2026-10-14-light.md) |
| CHK-2026-10-14-heavy-1 | 2026-10-14 | heavy | Readability & structural clarity | prudent-inadvertent | `TuiView`'s four modal dialog loops duplicate the repaint/readEvent/Cancel-Activate-Quit skeleton — extract a shared loop helper → GH-64 | 2027-01-14 | [CHK-2026-10-14-heavy](CHK-2026-10-14-heavy.md) |
| CHK-2026-10-14-heavy-2 | 2026-10-14 | heavy | Readability & structural clarity | prudent-inadvertent | Rethrow-SessionControl/rethrow-Cancellation/catch-Exception triplet copied in 5 places — extract one helper → GH-65 | 2027-01-14 | [CHK-2026-10-14-heavy](CHK-2026-10-14-heavy.md) |
| CHK-2026-10-14-heavy-3 | 2026-10-14 | heavy | Cross-surface UX consistency | prudent-deliberate | Console mode exits silently on solve (final board never shown) vs TUI's Congratulations screen → GH-66 | 2026-12-14 | [CHK-2026-10-14-heavy](CHK-2026-10-14-heavy.md) |
| CHK-2026-10-14-heavy-4 | 2026-10-14 | heavy | Cross-surface UX consistency | prudent-inadvertent | Console has no `help`/command list; all errors are a bare `Incorrect input` → GH-67 | 2027-01-14 | [CHK-2026-10-14-heavy](CHK-2026-10-14-heavy.md) |

## Resolved findings

Archive of findings once fixed or otherwise closed out.

| ID | Opened | Resolved | Tier | Dimension | Summary | Write-up |
|---|---|---|---|---|---|---|
| CHK-2026-10-14-light-2 | 2026-10-14 | 2026-10-14 | light | Unused functionality | `kotlinx-coroutines-test` unused since GH-42 — decide: adopt `runTest` or drop the dependency — fixed in commit `7f1da10` (dependency dropped) | [CHK-2026-10-14-light](CHK-2026-10-14-light.md) |
| CHK-2026-10-14-light-3 | 2026-10-14 | 2026-10-14 | light | Glossary/naming drift | GLOSSARY.md: stale `SQUARE_SIDE`/Save-file symbols, "solved" alias contradicts code, missing input mode/board size/dialog/toolbar/startup restore terms — fixed in commit `8986e12` | [CHK-2026-10-14-light](CHK-2026-10-14-light.md) |
| CHK-2026-10-14-heavy-5 | 2026-10-14 | 2026-10-14 | heavy | Accessibility | Keyboard-mode board cursor is colour-only (yellow) — add a non-colour cue (reverse video/brackets) — fixed in commit `00824fc` | [CHK-2026-10-14-heavy](CHK-2026-10-14-heavy.md) |
| CHK-2026-10-14-heavy-6 | 2026-10-14 | 2026-10-14 | heavy | Knowledge-corpus bookmark health | Bookmark vocabulary empty, all 14 notes `[]` — adopt proposed bookmarks and tag notes — fixed in commit `a93d932` | [CHK-2026-10-14-heavy](CHK-2026-10-14-heavy.md) |
