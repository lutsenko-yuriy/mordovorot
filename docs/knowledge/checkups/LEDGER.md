# Checkup ledger

Tracks due status and open findings for the two-tier periodic code-quality checkup defined in [ADR-0001](../decisions/ADR-0001-two-tier-periodic-code-quality-checkup.md). `scripts/checkup/due.py` reads the **Cadence & due status** table below to report which tier(s) are due; the `/checkup` skill updates this file after each run.

## Cadence & due status

Each tier is tracked as "not yet done this period" — a period label, not an exact-day match — so it stays flagged as overdue until actually run (see ADR-0001). `Period covered` is the period label (`YYYY-MM` for light, `YYYY-Qn` for heavy) of the most recently completed run.

| Tier | Cadence | Last run | Period covered | Next due |
|---|---|---|---|---|
| Light | 1st of every calendar month | 2026-10-14 | 2026-10 | 2026-11-01 |
| Heavy | 14th of Jan/Apr/Jul/Oct | never | — | — |

## Open findings

Findings needing human decision or larger effort, each carrying an explicit deadline — no individual PM ticket is filed (per ADR-0001). Classified using [Fowler's Technical Debt Quadrant](https://martinfowler.com/bliki/TechnicalDebtQuadrant.html) before being written up.

| ID | Opened | Tier | Dimension | Debt quadrant | Summary | Deadline | Write-up |
|---|---|---|---|---|---|---|---|
| CHK-2026-10-14-light-1 | 2026-10-14 | light | Unused functionality | prudent-inadvertent | `BoardImpl.counter` is write-only (reset, never incremented/read outside 3 tests) — repurpose in GH-62 or remove | 2026-11-14 | [CHK-2026-10-14-light](CHK-2026-10-14-light.md) |
| CHK-2026-10-14-light-2 | 2026-10-14 | light | Unused functionality | prudent-deliberate | `kotlinx-coroutines-test` unused since GH-42 — decide: adopt `runTest` or drop the dependency | 2026-11-30 | [CHK-2026-10-14-light](CHK-2026-10-14-light.md) |
| CHK-2026-10-14-light-3 | 2026-10-14 | light | Glossary/naming drift | prudent-inadvertent | GLOSSARY.md: stale `SQUARE_SIDE`/Save-file symbols, "solved" alias contradicts code, missing input mode/board size/dialog/toolbar/startup restore terms | 2026-10-31 | [CHK-2026-10-14-light](CHK-2026-10-14-light.md) |

## Resolved findings

Archive of findings once fixed or otherwise closed out.

| ID | Opened | Resolved | Tier | Dimension | Summary | Write-up |
|---|---|---|---|---|---|---|
| _none yet_ | | | | | | |
