---
bookmarks: []
---

# GH-3: Mouse-driven TUI with directional edge arrows

## Notes

### 2026-09-11

**Research-WU: raw mode + mouse reporting are reachable with the JVM stdlib alone**

Feasibility spike for the plan's Phase 1 question — confirmed via cited precedent rather than
a live terminal test (this session's own shell isn't an interactive TTY, which is itself the
non-TTY case the ticket must handle). Findings:

- **Raw mode via `stty`, no library needed.** JLine's own historical `UnixTerminal`
  implementation shells out to `stty` against `/dev/tty` to disable echo and canonical
  (line-buffered) input — it does not implement raw mode itself in Java, because the JVM has
  no native raw-mode API. This is exactly the plan's proposed approach
  (`ProcessBuilder("stty", "raw", "-echo").redirectInput(File("/dev/tty"))`, or equivalent),
  and confirms it's a proven pattern, not a novel risk. `stty` is present on this dev machine
  (`/bin/stty`, macOS/Darwin) and is documented as available on "all known Unix systems
  including Linux and Macintosh OS X." Restoring cooked mode on exit (including abnormal exit,
  via a JVM shutdown hook) is the same pattern JLine uses for its own cleanup.
  Source: [JLine's stty-based UnixTerminal, described via search of jline.sourceforge.net/jline.github.io docs and jline troubleshooting guide]

- **SGR-1006 mouse reporting is a standard, stdlib-reachable xterm escape-sequence protocol** —
  no library needed, just writing/reading raw bytes:
  - Enable: `CSI ?1000h CSI ?1006h` (i.e. `[?1000h[?1006h`); disable with `l` in
    place of `h`, mode 1006 before 1000.
  - Event format: `CSI < Cb ; Cx ; Cy M` for a press, `CSI < Cb ; Cx ; Cy m` for a release
    (`[<Cb;Cx;CyM` / `...m`). `Cb` encodes the button (0/1/2 = left/middle/right, +32 for
    drag-motion) plus modifier-key bits; `Cx`/`Cy` are 1-based column/row.
  - Source: [xterm's own control-sequences reference, invisible-island.net/xterm/ctlseqs — DECSET modes 1000/1006 and the SGR mouse report format]

- **No robust JVM API exists to detect "is stdin a real interactive terminal."**
  `System.console() != null` is the standard idiom, but it is documented as unreliable in
  redirected/piped/containerized contexts — it can return null even when a script author
  didn't intend to disable interactivity, and there's no better native alternative
  (`Pty4j` or shelling out are the only more-precise options, both heavier than this project's
  zero-dependency stance justifies for a boolean check).
  Source: [search results on Java `System.console()` limitations, incl. the appsody/appsody
  GitHub issue "Java apps cannot detect whether console is interactive TTY"]

**Resulting decision — not-a-TTY fallback behavior:** confirmed with the user (see GH-3 ticket
body, updated 2026-09-11): when mouse mode is the effective launch choice (no `--console`
given) but `System.console() == null` at startup, the app falls back to console mode
automatically rather than failing fast. This accepts `System.console()`'s known
false-negative risk as tolerable — a false negative degrades a session to the console UI
(fully functional, if less pleasant) rather than crashing it, so the imprecision costs
UX polish, not correctness. Revisit only if false negatives turn out to be common on the
platforms this ships to.

**Conclusion:** no new dependency needed; `Terminal`/`AnsiTerminal`/`TerminalInputParser` as
scoped in the plan are buildable on the JVM stdlib alone. Proceeding to WU0 (scenario stubs).

- 2026-09-11: Add a workflow step guideline — implementation-stage comments should be kept concise (essential sense only, ~3 lines as a soft target, optional).

## Debrief summary
