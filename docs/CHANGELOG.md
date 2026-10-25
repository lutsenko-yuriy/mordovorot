# Changelog

A record of all versioned releases. For planned work and known issues, see @docs/BACKLOG.md.

---

<!-- This file is maintained by the Product Owner agent.
     New sections are prepended after each merged PR in the format:

## [X.Y.Z] — YYYY-MM-DD (PR #N merged)

### Added / Changed / Fixed
- ...
-->

## [0.21.1] — 2026-10-25 (PR #172 merged)

### Added
- [test] (PR #172) GH-162: a guard test keeps the web's reduced-motion rule in place, so every web animation and transition still stops when the system asks for reduced motion, and no stylesheet can outrank that rule. The game itself is unchanged.

## [0.21.0] — 2026-10-25 (PR #161 merged)

### Added
- [user] (PR #161) GH-160: 6x6 boards in the terminal game and the browser. Mordie sits 6x6 out for now ("6x6 is too much for me, you're on your own!") and Give Up isn't shown there. Letters labels only go up to 5x5, so a 6x6 board shows numbers instead and keeps your choice for smaller boards; a 6x6 save with Letters asks you to pick other labels.
- [non-user] serve_lan.sh rebuilds the web bundle when it isn't from the current commit; /plan now checks touched files against the size guideline.

## [0.20.8] — 2026-10-25 (PR #159 merged)

### Added
- [test] (PR #159) GH-156: `scripts/web/harness/styles_diff.sh` compares every element's computed styles in the branch's web build against a master build, case by case, and lists each element whose styles differ, with old and new values; repeated changes are folded and the entries numbered. It builds both bundles itself (the master bundle is rebuilt when master moves), and exits 0 when nothing differs, 1 when something does, 2 on error.
- [non-user] The phone-fit harness folder is renamed to `scripts/web/harness/`; `measure.sh` shares its code through `lib.sh`, and `harness.html` gains `report=styles`.
- [ci] The tests workflow gets a `web_harness` job.
- [meta] The workflow docs point to the harness, and FEATURE.md gains a "Sizing the choice" rule (GH-156 debrief).

## [0.20.7] — 2026-10-25 (PR #158 merged)

### Changed
- [user] (PR #158) GH-149: on phones and tablets, pinching no longer zooms the web game's page, and tapping into the save name no longer zooms in either; desktop zoom still works.
- [meta] The plan skill shows a plan summary and each decision in the session before asking (GH-149 debrief).

## [0.20.6] — 2026-10-24 (PR #157 merged)

### Fixed
- [user] (PR #157) GH-148: on phones held sideways, the web game now fits the screen with no scrolling; the board shrinks to fit beside the controls, and small phones get a tighter layout.
- [non-user] The phone-fit rules move to their own sheet, fit.css; the phone-fit harness checks every edge and has a landscape preset; serve_lan.sh runs under macOS's bash 3.2 and prints a QR code when qrencode is installed.

## [0.20.5] — 2026-10-24 (PR #155 merged)

### Changed
- [app] (PR #155) GH-153: the web client's styles.css is split into styles.css, board.css, dialogs.css and solution.css (linked in that order), every rule moved verbatim; the game looks and behaves the same.

## [0.20.4] — 2026-10-24 (PR #154 merged)

### Fixed
- [app] (PR #154) GH-152: the Kotlin/JS build tools' lockfile now locks patched body-parser 2.4.0 (webpack dev server) and fast-uri 3.1.9 (webpack's ajv) (Dependabot alerts #5–#9). Build tooling only; the game and web.js are unchanged.

## [0.20.3] — 2026-10-24 (PR #151 merged)

### Fixed
- [user] (PR #151) GH-147: on phones held upright the web game now fits the screen with no scrolling — the tiles shrink to fit, the toolbar stays on one row and the footer is one short line.

## [0.20.2] — 2026-10-24 (PR #146 merged)

### Fixed
- [user] (PR #146) GH-145: on touch screens a double tap no longer zooms the page and text no longer gets selected or shows the iOS long-press callout on the board; pinch-zoom still works, and the save name field stays editable.

## [0.20.1] — 2026-10-24 (PR #144 merged)

### Fixed
- [app] (PR #144) GH-137: the Kotlin/JS test runner's mocha now gets patched serialize-javascript 7.1.4 and diff 8.0.4 through yarn resolutions in the root build script (Dependabot alerts #1–#3); the withdrawn braces advisory (#4) is dismissed. Build tooling only; the game and web.js are unchanged.

## [0.20.0] — 2026-10-24 (PR #143 merged)

### Added
- [user] (PR #143) GH-139 (WU3/3): choose how tiles are labelled — numbers, numbers from 0, pairs, pairs from 0, letters or letter pairs — in a Tile labels dialog, in the terminal (`[ Labels ]`, F4) and now in the browser (`[ Labels ]`, with a live preview); the choice is remembered, and a save keeps its own labels (WU1–WU2).

## [0.19.2] — 2026-10-24 (PR #142 merged)

### Added
- [wip] (PR #142) GH-139 (WU2/3): tile labels in the terminal: `[ Labels ]` / `[Labels F4]` on the second row, F4, the Tile labels dialog, and the board drawn in the chosen labels.

## [0.19.1] — 2026-10-24 (PR #141 merged)

### Added
- [wip] (PR #141) GH-139 (WU1/3): a save keeps its tile labels (line 8) and the chosen labels are remembered as a preference (`saves/preferences` in the terminal, `localStorage` in the browser); the viewmodel, analytics and both clients' wiring are in place, with nothing drawn yet.

## [0.19.0] — 2026-10-24 (PR #138 merged)

### Added
- [user] (PR #138) GH-111 (WU3/3): play the browser version at https://lutsenko-yuriy.github.io/mordovorot/ — every green merge to `master` publishes it; a footer shows the version and credits Loopover's creator, and on touch screens the board drops the arrows for dragging (WU1–WU2).
- [ci] (PR #138) GH-111: `build.yml` builds the production web bundle on every run and, after a green `master` build, its `deploy` job publishes it to GitHub Pages.

## [0.18.3] — 2026-10-24 (PR #136 merged)

### Changed
- [wip] (PR #136) GH-111: the README's Credits also link carykh's video introducing Loopover and credit Janis Pritzkau, who made loopover.xyz.

## [0.18.2] — 2026-10-19 (PR #135 merged)

### Changed
- [wip] (PR #135) GH-111 (WU2/3): on touch screens the arrows are hidden (screen readers still reach them) and the board grows into their room.

## [0.18.1] — 2026-10-19 (PR #134 merged)

### Changed
- [wip] (PR #134) GH-111 (WU1/2): a footer at the bottom of the web page with the app version (stamped from build.gradle.kts) and the Loopover credit (Cary Huang, links to the original and loopover.xyz, in a new tab); app_launched on web gains app_version and host (public_site/localhost/other); the page sizes to the dynamic viewport on phones. Publishing on GitHub Pages follows in WU2.

## [0.18.0] — 2026-10-18 (PR #132 merged)

### Added
- [user] (PR #132) GH-126 (WU5/5): Give Up and Mordie's solution on all three clients — `[ Give Up ]` (F8 in keyboard mode) dimmed until Mordie finds its solution, then a Yes / No confirmation; `[ Mordie's solution ]` after a win; the solution screen on the original arrangement with a numbered step list, stepped by hand with Prev / Next / Restart; saves reopen the screen. WU5 adds the web's wide/tall solution layouts, the current-step highlight and auto-scroll, and fixes a blank page on a fresh browser from WU4.

## [0.17.5] — 2026-10-18 (PR #131 merged)

### Changed
- [wip] (PR #131) GH-126 (WU4/5): Give Up and the solution screen in the web (the Give Up button, disabled until Found it!, then the Yes / No confirmation, or Mordie's solution after a win; Prev / Next / Restart slide one tile per shift; the step list marks the current step; arrows, drags and Moves are off on the screen); the web's look follows in WU5.

## [0.17.4] — 2026-10-18 (PR #130 merged)

### Changed
- [wip] (PR #130) GH-126 (WU3/5): Give Up and the solution screen in the terminal (the Give Up row and F8, the confirmation, the step list with wheel and W/S scrolling); Mordie's solution is now stepped by hand with Prev / Next / Restart instead of a timed replay, and Give Up is hidden on the solution screen; the web follows in WU4-5.

## [0.17.3] — 2026-10-18 (PR #129 merged)

### Changed
- [wip] (PR #129) GH-126 (WU2/5): Give Up and the solution screen in the shared viewModel (the button states, the screen on the original arrangement, the step list and replay, analytics, Save/Load of the screen); the terminal and web views follow in WU3-5.

## [0.17.2] — 2026-10-18 (PR #128 merged)

### Changed
- [wip] (PR #128) GH-126 (WU1/5): Mordie keeps the moves of the solution it found, and saves store them (line 6) plus a solution-screen flag (line 7); old saves load as before, with no solution.

## [0.17.1] — 2026-10-18 (PR #125 merged)

### Changed
- [app] (PR #125) GH-124: `./gradlew :web:serve -t` rebuilds the browser game on change and can run alongside `check`/`build` (serve's outputs go to `build-serve/`; the Kotlin daemon gets 2 GB); a bare `serve` fails fast with the `-t` hint.

## [0.17.0] — 2026-10-18 (PR #123 merged)

### Added
- [user] (PR #123) GH-110 (WU7/7): Mordie's lines appear and disappear like chat messages in the browser; the browser client is now complete and playable on localhost:8080 with tiles, drag interactions, saves, and Mordie's responses.

## [0.16.13] — 2026-10-18 (PR #122 merged)

### Changed
- [wip] (PR #122) GH-110 (WU6/7): drag tiles in the browser - press a tile and drag; the row or column locks, follows the pointer with wrap-around, and snaps to the nearest tile on release as one move.

## [0.16.12] — 2026-10-18 (PR #121 merged)

### Changed
- [wip] (PR #121) GH-110 (WU5/7): saves in the browser - Save, Load and the startup restore over localStorage (the terminal's save format and name rules), the can't-be-solved dialog without Exit, status messages, and the browser's leave-site prompt while progress is unsaved.

## [0.16.11] — 2026-10-18 (PR #120 merged)

### Changed
- [wip] (PR #120) GH-110 (WU4/7): playable web board on localhost:8080 - WebGame over the shared viewmodel, arrow shifts, New with the size picker, Moves, Mordie's lines and the solved state, plain DOM renderer, web analytics wiring (platform: web, input_method: rich_ui, app_launched {mode, layout}).

## [0.16.10] — 2026-10-18 (PR #119 merged)

### Changed
- [wip] (PR #119) GH-110 (WU3/7): shared hooks for the web client. Every event gets a `platform` tag (`terminal` on the terminal). Shift sources: `PointerShift` and `GameShiftSources`, which add `board_solved.shift_input` on the web. Multi-step `shiftRow`/`shiftColumn` count as one move and check for solved once. `UnsavedProgress`/`hasUnsavedProgress` for the leave-site prompt. The terminal behaves the same apart from the new property.

## [0.16.9] — 2026-10-18 (PR #118 merged)

### Changed
- [wip] (PR #118) GH-110 (WU2/7): the save text format (SaveFileCodec), the save-name rules (SaveNames) and SaveFileFormatException move into :shared:storage so the browser client can share the terminal's saves (behaviour unchanged); the web dev server no longer opens a browser tab by itself.

## [0.16.8] — 2026-10-18 (PR #117 merged)

### Changed
- [test] (PR #117) GH-110 (WU0/7): 31 browser scenario stubs (startup, play, save/load, unsaved progress, drag, analytics) in `web/src/commonTest/kotlin/web/scenario/`, made green by WU4–WU6; `docs/ANALYTICS_EVENTS.md` records the approved web analytics (`platform`, `input_method: rich_ui`, `app_launched.layout`, `board_solved.shift_input`).

## [0.16.7] — 2026-10-18 (PR #115 merged)

### Changed
- [wip] (PR #115) GH-110 (WU1/7): the `:web` Kotlin Multiplatform module is added with `jvm()` + `js { browser(); nodejs() }` targets, webpack dev server on port 8080, and a placeholder page; no behaviour change.

## [0.16.6] — 2026-10-17 (PR #114 merged)

### Changed
- [test] (PR #114) GH-103 (WU2/2): the shared modules' tests moved from `jvmTest` to `commonTest` and now run on Node.js too (186 tests on JS; `runBlocking` → `runTest`), with one JVM-only cross-thread test kept in mordie's `jvmTest`.
- [non-user] GH-103: the Node.js test runner's per-test timeout is raised from Mocha's 2 s default to 30 s; the 3x3 solver test went past 2 s on a CI runner.
- [non-user] Debrief workflow changes: bounded CI wait in ship (poll checks every 30s, max 30 times), checking on long-running subagents, `/note` front matter, debrief opens with the ticket's notes. No behaviour change.

## [0.16.5] — 2026-10-17 (PR #113 merged)

### Changed
- [wip] (PR #113) GH-103 (WU1/2): the shared modules get a real `js { nodejs() }` target (Node.js test task, `kotlin-js-store/yarn.lock`), `:shared:testing` becomes multiplatform, and `kotlinx-coroutines-test` joins the version catalog; no behaviour change.

## [0.16.4] — 2026-10-17 (PR #112 merged)

### Changed
- [app] GH-109: Kotlin 2.4.21 (from 1.9.24), kotlinx-coroutines 1.11.0 and Gradle 9.7.1, with versions now in `gradle/libs.versions.toml`; no behaviour change.

## [0.16.3] — 2026-10-17 (PR #108 merged)

### Changed
- [meta] (PR #108) GH-107: Alert when the session is waiting on the user — desktop + spoken alert (Notification/Stop hook with toggles in .claude/alert.env).

## [0.16.3] — 2026-10-17 (PR #106 merged)

### Changed
- [app] (PR #106) GH-41 (WU3/3): the shared viewmodel moves into :shared:viewmodel - the game's platform-independent code (board, analytics, save contract, Mordie, viewmodel minus Exit and session control) now lives in five Kotlin Multiplatform modules; no behaviour change.

## [0.16.2] — 2026-10-17 (PR #105 merged)

### Changed
- [wip] (PR #105) GH-41 (WU2/3): board_model, analytics, the storage contract and mordie move into Kotlin Multiplatform modules (:shared:core/analytics/storage/mordie, jvm() + compile-only js(IR)) with shared test doubles in :shared:testing; the documented test command is now ./gradlew check; no behaviour change.

## [0.16.1] — 2026-10-17 (PR #104 merged)

### Changed
- [wip] (PR #104) GH-41 (WU1/3): split Exit and session control out of the shared viewmodel (GameViewModel + UnsolvableSaveExit hook, catchingNonCancellation); no behaviour change.

## [0.16.0] — 2026-10-17 (PR #102 merged)

### Added
- [user] GH-101: Keyboard mode now selects a row or column with WASD and shifts it with HJKL or the arrow keys; the arrows are gone and the selected line is highlighted, with key hints on either side of the board.

## [0.15.0] — 2026-10-17 (PR #100 merged)

### Added
- [user] GH-99: The Save dialog now opens with the name of the game you last loaded or saved already filled in, so saving your progress is just Enter.

## [0.14.8] — 2026-10-17 (PR #98 merged)

### Changed
- [app] (PR #98) GH-78 (WU8/8): the size and unsolvable-save dialogs get their own controllers, and two routers take over answering viewmodel requests and toolbar buttons, leaving `TuiView` with just its event loop and shifts. This completes 0.14.1–0.14.7: `TuiView` (460 → 102 lines) and `ViewModelImpl` (377 → 83 lines) are split by feature into use cases and dialog controllers, and `AppWiring.kt` is the one composition root, with no production class defaulting to a real implementation. No behaviour change.
- [meta] (PR #98) GH-78 debrief: new `docs/workflows/REFACTOR.md` for behaviour-preserving multi-WU tickets (no user questions between plan approval and debrief); `plan` proposes it and checks the changelog tag list; `ship` merges only on fully passed checks; `audit` never checks out in the shared working tree.

## [0.14.7] — 2026-10-17 (PR #97 merged)

### Changed
- [wip] (PR #97) GH-78 (WU7/8): the Save, Exit, Load and startup Restore dialogs move out of `TuiView` into their own controllers (`view.tui.dialog`); `TuiView` routes toolbar clicks and viewmodel requests to them (339 lines down to 194). No behaviour change; the size and unsolvable dialogs and the routers follow in WU8.

## [0.14.6] — 2026-10-17 (PR #96 merged)

### Changed
- [wip] (PR #96) GH-78 (WU6/8): first TUI split - the TUI's shared drawing state (last-drawn layouts, the pending status message, the solved flag), repaint, and the one dialog loop move from `TuiView` into a new `TuiScreen`; the dialog loop now resets keyboard focus itself. No behaviour change; the dialog controllers follow in WU7.

## [0.14.5] — 2026-10-17 (PR #95 merged)

### Changed
- [wip] (PR #95) GH-78 (WU5/8): the startup sequence (restore prompt or size prompt, then Mordie) moves into `StartupRestoreUseCase`, which completes the viewmodel split. `ViewModelImpl` is now a thin facade that keeps the shifts and the queries and hands every other flow to its use case (368 lines down to 83). No behaviour change; the TUI split starts in WU6.

## [0.14.4] — 2026-10-17 (PR #94 merged)

### Changed
- [wip] (PR #94) GH-78 (WU4/8): the load, unsolvable-save (repair / new game / exit) and exit flows move out of `ViewModelImpl` into `LoadGameUseCase`, `ResolveUnsolvableSaveUseCase` and `ExitGameUseCase`; `ViewModelImpl` no longer touches the saves directly. No behaviour change; the startup sequence follows in WU5.

## [0.14.3] — 2026-10-17 (PR #93 merged)

### Changed
- [wip] (PR #93) GH-78 (WU3/8): first viewmodel split - the request channel (`UiRequestChannel`), the save listings (`SaveCatalog`) and the new-game and save flows (`NewGameUseCase`, `SaveGameUseCase`) move out of `ViewModelImpl` into their own classes, wired once in `buildViewModel`. No behaviour change; load, unsolvable-save and exit follow in WU4.

## [0.14.2] — 2026-10-17 (PR #92 merged)

### Changed
- [wip] (PR #92) GH-78 (WU2/8): one composition root - `AppWiring.kt` is now the only production code that constructs collaborators; `ViewModelImpl` gets `MordieHooks` injected, `TuiView` takes its view model, mode switcher and renderer as constructor parameters (`TuiView.create` is gone), `GameSession` narrows to the mode, Mordie and a view builder, and no production class defaults to a real implementation any more. No behaviour change; the viewmodel split starts in WU3.

## [0.14.1] — 2026-10-17 (PR #91 merged)

### Changed
- [wip] (PR #91) GH-78 (WU1/8): tests now build `ViewModelImpl`, `TuiView` and `GameSession` only through `testViewModel`/`testTuiView`/`testGameSession` builders with fake defaults, and `FakeViewModelUi.drive` times out after 10s instead of hanging. Test-only - no production change; WU2 moves all wiring into one composition root.

## [0.14.0] — 2026-10-17 (PR #89 merged)

### Added
- [user] (PR #89) GH-83 (WU4/4): Mordie, a computer opponent, races you to the shortest solution of the board you started with. A magenta `Mordie:` line under `Moves:` greets you, reports how far its search has got every 20 shifts (skipped when nothing has changed), and announces when it has found the shortest solution. The solved title then says how you did: you beat it, matched it, lost to it, or (hand-edited saves only) "Are you a machine?". Saves keep Mordie's progress; older saves still load. This completes 0.13.1–0.13.4.

## [0.13.4] — 2026-10-17 (PR #88 merged)

### Added
- [wip] (PR #88) GH-83 (WU3/4): Mordie wired into the game - one search per session that survives mode switches and stops on exit; it starts on new game, Repair and startup, resumes from or restores a save, and sits out a loaded board that already has moves. Saves store its state; new `mordie_solution_found` event and `board_solved` gains `mordie_result`/`mordie_moves`. Nothing on screen yet - the TUI lands in WU4.

## [0.13.3] — 2026-10-16 (PR #87 merged)

### Added
- [wip] (PR #87) GH-83 (WU2/4): Mordie's opponent state machine (shift-driven line: opening, progress every 20 shifts, "hard" from 60, found revealed at the next 5-shift check; live-state outcome incl. the "Are you a machine?" case), its text with singular "1 move", and the save format - lines 4-5 store the original arrangement and `<N> found|searching`; older saves still load. Not wired into the game yet - that lands in WU3.

## [0.13.2] — 2026-10-16 (PR #86 merged)

### Added
- [wip] (PR #86) GH-83 (WU1/4): Mordie's search engine - an admissible move-metric heuristic, a steppable IDA* solver (optimal against a full 3x3 BFS; O(depth) memory, resumable above a saved N) and a throttled background search (~half a core, cancellable). Not wired into the game yet - Mordie's state, text and save format land in WU2.

## [0.13.1] — 2026-10-16 (PR #84 merged)

### Changed
- [meta] (PR #84) GH-82: credited Loopover and its creator Cary Huang in README; described Mordovorot as "a terminal version of Loopover" in README, AGENTS.md, PRODUCT_SPEC.md, and skill_router.toml.

## [0.13.0] — 2026-10-15 (PR #80 merged)

### Removed
- [user] (PR #80) GH-79: console mode removed - the game only runs as a terminal UI. A launch with no terminal prints "No interactive terminal found - Mordovorot only runs as a terminal UI." and exits; --console prints a notice and starts the TUI; the TUI's Console button and F8 are gone; a failed mode switch stays in the current mode. Saves are unaffected.

## [0.12.0] — 2026-10-15 (PR #77 merged)

### Added
- [user] (PR #77) GH-74: loading a save that can't be solved (3x3/5x5) now offers repair (two tiles swapped, save rewritten), a new game, or exit - console prompt and a TUI "Can't be solved" dialog; new games are always solvable (since 0.11.1).

## [0.11.1] — 2026-10-15 (PR #76 merged)

### Fixed
- [wip] (PR #76) GH-74 (WU1/2): new games on 3x3/5x5 boards are always solvable (parity-fixed shuffle); saves that can't be solved are marked `unsolvable` in every save listing, and `load` accepts the marked form. Loading such a save still restores it as-is - the repair / new game / exit choice lands in WU2.

## [0.11.0] — 2026-10-15

### Added
- [user] (PR #73) GH-67: console `help` command lists all commands or shows one command's usage; `help` hint printed at console start; specific error messages with usage lines for rejected commands.

## [0.10.0] — 2026-10-15

### Added
- [user] (PR #72) GH-66: console shows solved-state message and final board; congratulations prompt and play-again option follow. Board state is reset between games.

## [0.9.2] — 2026-10-14

### Changed
- [app] (PR #71) GH-64: extract shared runModal dialog-loop helper in TuiView; Save/Load/Exit/Size dialogs route Escape/Cancel/end-of-input through it so dialog_cancelled is tracked exactly once. No user-visible behaviour change.

## [0.9.1] — 2026-10-14

### Changed
- [app] (PR #70) GH-65: add catchingNonControl exception-catch helper; route five non-control catch sites through it for consistency. No user-visible behaviour change.

## [0.9.0] — 2026-10-14

### Added
- [user] (PR #69) GH-62: move counter — a move is a run of consecutive shifts on one row/column; shown as `Moves: N` (console: after the board; TUI: under the title), kept across mode switches, saved as the save file's third line (older saves load as 0), and a `board_solved` analytics event.
- [meta] (PR #61) GH-60: add `bookmarks:` frontmatter to GH notes; align README test comments; run notes index check in CI
- [wip] HAB-278 WU22: port YAB onboarding remote/README/CHANGELOG handling and skill_router fixes; session start reads [providers].pm (PR #59)
- [meta] (PR #58) HAB-278 WU11: README refresh — installDist launcher for the TUIs, --mouse/--keyboard/--console/--size flags, onboarding test command, /onboard prerequisites (python3 vs 3.11+), launch-from-root and config-push hint.
- [meta] (PR #57) HAB-278 WU10: ran /onboard — filled skill_router.toml [project]/pm=github, TECH_STACK/CODE_STYLE/CONSTRAINTS/MODEL_TIERS docs, removed unused .mcp.json, synced build.gradle.kts version to 0.8.1.

## [0.8.1] — 2026-10-05 (PR #56 merged)

### Added
- [wip] HAB-278 (WU9, Habit Loop ticket): port the `/onboard` skill and onboarding gate from yuriys-agentic-boyz (`scripts/onboard/`, `skills/configure/onboard/`, committed `.claude/settings.json` hooks, empty `[project]` config shape) and delete `setup.sh`. Until `/onboard` runs (WU10) the gate keeps the clone in onboarding mode. CI runs the onboard test suite via `tests.yml` (template-state tests skip once the sentinel is gone).

## [0.8.0] — 2026-09-16 (PR #55 merged)

### Added
- [user] GH-44 (WU5/5): load auto-resizes to match a save's size instead of rejecting a mismatch — `BoardModel.restoreState` now derives `squareSide` from the restored state's own length, so loading a differently-sized save resizes the board and restores it, both at startup and via the mid-game `load`/Load dialog. This completes GH-44: the board is adjustable 3x3-5x5 (default 4x4) via `--size=N`, the startup prompt, `size <N>`/`[ New ]` mid-session; save-listing surfaces show each save's size; and a mismatched load resizes rather than rejects.

## [0.7.8] — 2026-09-16 (PR #54 merged)

### Added
- [wip] GH-44 (WU4/5): save size visibility — every save-listing surface (startup restore prompts, the console `load` not-found message, the TUI Load dialog) now shows each save's board size alongside its name, e.g. `foo (5x5)`, so a mismatch is visible before picking a save. `ViewModel.listSaves()` returns `SaveInfo` (name + nullable size) instead of bare names. A size-mismatched load still gets rejected rather than auto-resized - that lands in WU5, the final WU.

## [0.7.7] — 2026-09-16 (PR #53 merged)

### Added
- [wip] GH-44 (WU3/5): `size <N>` console command and the TUI `[ New ]` toolbar button/F9 shortcut - both start a fresh game at a chosen size (3-5) mid-session, opening the same size picker the startup prompt uses. Keyboard-mode arrow cursor now resets when the board's side changes mid-session. Still no save-size visibility in the restore/Load prompts, and a size-mismatched load still gets rejected rather than auto-resized - those land in WU4/WU5.

## [0.7.6] — 2026-09-15 (PR #52 merged)

### Added
- [wip] GH-44 (WU2/4): startup size prompt — `ViewModelImpl.restoreOnStartup` raises `UiRequest.ChooseBoardSize` once it's established nothing was restored (no saves, declined, blank/EOF, failed load); console answers with a text prompt, the TUI with a new `Dialog.Kind.SIZE` picker (buttons-only, like `EXIT`). `--size=N` at launch now bypasses the restore prompt entirely too, not just the size prompt - the flag is the answer no matter what saves exist. Still no `size <N>`/`[ New ]` mid-session entry points - those land in WU3.

## [0.7.5] — 2026-09-15 (PR #51 merged)

### Added
- [wip] GH-44 (WU1/3): board-size domain layer — `board_model.BoardSize` (valid range 3-5, default 4), `BoardModel.newGame(side)` (the only way the board's side changes; `resetGame()` keeps reshuffling at the current size), and a `--size=N` launch flag (`Main.kt`'s `resolveBoardSize`) that's tracked on `app_launched.board_size`. `BoardModel.SQUARE_SIDE` renamed to `squareSide` since it's no longer constructor-fixed. `ViewModel.newGame(size, trigger)` delegates to the board and tracks `new_game_size_selected`. No player-visible entry point yet — the startup size prompt (WU2) and `size <N>`/`[ New ]` surfaces (WU3) land next.

## [0.7.4] — 2026-09-15 (PR #47 merged)

### Changed
- [app] GH-42 (WU4/4): mechanical rename closing the Presenter→ViewModel redesign: `presenter.Presenter`/`presenter.PresenterImpl` → `viewmodel.ViewModel`/`viewmodel.ViewModelImpl`, package rename `presenter` → `viewmodel`, and all references (classes, variables, KDoc, test doubles, test files, docs: AGENTS.md, README.md, docs/ARCHITECTURE.md) to match. Also replaced `MVP (Model-View-Presenter)` with `MVVM (Model-View-ViewModel)` in documentation. No user-visible behaviour change.

## [0.7.3] — 2026-09-15 (PR #46 merged)

### Changed
- [wip] GH-42 (WU3/4): collapse `Presenter`/`ConsolePresenter`/`TuiPresenter` into one `Presenter`, and `BasePresenter`/`ConsolePresenterImpl`/`TuiPresenterImpl` into one `PresenterImpl` — it holds no `View` reference at all now. The console session loop (`displayBoard`/`processCommand`/the solved check) moved from the old `ConsolePresenterImpl.play()` into `ViewImpl.play()`, alongside its own `UiRequest`-draining handler coroutine, matching the shape `TuiView.play()` already had. No user-visible behaviour change.

## [0.7.2] — 2026-09-15 (PR #45 merged)

### Changed
- [wip] GH-42 (WU2/4): `UiRequest`/`ask()` request-response channel — `Presenter` no longer calls `showMessage`/`confirmRestore`/`chooseSaveToRestore`/`confirmSaveBeforeExit`/`promptSaveName` directly on its `View`; it raises a `UiRequest<R>` on a `Channel.RENDEZVOUS` (`Presenter.uiRequests`) and suspends until the View answers. `ViewImpl`/`TuiView` each gain a request-handler coroutine draining that channel alongside their own event loop. `saveGame`/`loadGame`/`exitGame`/`offerStartupRestore` gain a `CancellationException`-before-catch-all guard so a cancelled coroutine isn't misread as a failed save/load. No user-visible behaviour change.

## [0.7.1] — 2026-09-15 (PR #43 merged)

### Changed
- [wip] GH-42 (WU1/4): coroutines dependency + suspend propagation, no design change — adds `kotlinx-coroutines-core` (the project's first third-party runtime dependency) and `kotlinx-coroutines-test`; `main()` wraps in `runBlocking`; `GameSession.run()`, `View`'s prompt methods, and `Presenter.saveGame()`/`loadGame()`/`exitGame()` become `suspend`, ahead of WU2's request channel. `BasePresenter` still holds its `View` reference at this point. No user-visible behaviour change.

## [0.7.0] — 2026-09-15 (PR #40 merged)

### Added
- [app] GH-30 (WU4/4): keyboard mode's F7/F8 function keys request a runtime switch to mouse/console mode, mirroring the mouse toolbar's `[Mouse F7]`/`[Console F8]` row (added in WU3) and reaching the same injected `ModeSwitcher`. Both stay live even when solved, matching F5/F6/Escape. `TerminalInputParser` decodes `ESC[19~` as the new `FunctionKey(8)`. This completes GH-30: from console, mouse, or keyboard mode, the player can switch to any other mode mid-game — via console `mouse`/`keyboard` commands (WU2), the mouse toolbar's mode row (WU3), or keyboard F7/F8 (WU4) — without losing the board.

## [0.6.3] — 2026-09-15 (PR #38 merged)

### Changed
- [wip] GH-30 (WU3/4): TUI toolbar mode buttons — mouse mode's toolbar gains `[ Keyboard ]`/`[ Console ]` buttons on a row below `[ Save ] [ Load ] [ Exit ]`, switching mode without losing the current game (the TUI counterpart of WU2's console `mouse`/`keyboard` commands). Reached via an injected `ModeSwitcher`, mirroring `ViewImpl`'s WU2 wiring. `BoardLayout`'s Save/Load/Exit button positions now look themselves up by `HitTarget` instead of a fixed index.

## [0.6.2] — 2026-09-15 (PR #37 merged)

### Changed
- [wip] GH-30 (WU2/4): console `mouse`/`keyboard` commands — lets the user switch from the line-based console UI to the mouse-driven or keyboard-driven TUI mid-game without losing the board, via `ViewImpl`'s new `mouse`/`keyboard` commands reaching an injected `ModeSwitcher`. If there's no interactive terminal to switch into, the command is rejected with an on-screen message and play continues in console mode. Also hardens `GameSession`'s mode-switch loop: a rebuild that fails outright (e.g. a missing `stty`) now falls back to console instead of crashing and losing the in-progress game.

## [0.6.1] — 2026-09-14 (PR #36 merged)

### Changed
- [wip] GH-30 (WU1/4): session loop + mode-switch plumbing — extracts `GameSession` out of `main()` so a running session can rebuild its View/presenter/analytics stack around a different `InputMode` mid-session instead of the process just ending; adds `ModeSwitchRequestedException`/`ModeSwitcher`/`ModeSwitcherImpl` (the single place that checks for an interactive terminal and tracks `input_mode_switched`), a shared `SessionControlException` marker base, and renames `LaunchMode` to `InputMode`. Not yet reachable from any UI — no `mouse`/`keyboard` console command (WU2), no TUI toolbar buttons (WU3), no F7/F8 shortcuts (WU4) call `ModeSwitcher.switchTo` yet.

## [0.6.0] — 2026-09-14 (PR #34 merged)

### Added
- [wip] GH-18 (WU1/5): decode arrow/Tab/BackTab/function keys in the TUI input parser — extends `TerminalInputParser` to recognize ESC sequences for arrow keys (`→`/`←`/`↑`/`↓`), Tab/BackTab, and function keys (F1–F12), converting them to `TerminalEvent.KeyPress` — not yet wired to the UI, still unreachable until WU2/3 routes these events through the board screen.
- [wip] GH-18 (WU2/5): cursor ring + dialog focus geometry and rendering in the TUI view layer
- [wip] GH-18 (WU3/5): TuiInput strategy extraction in the TUI view layer (no behaviour change)
- [wip] GH-18 (WU4/5): `--keyboard` launch mode + board navigation — `KeyboardInput` driver wires arrow keys to `ArrowCursor`, Enter/Space activates shifts, F5/F6/Esc open dialogs; `LaunchMode` gains `KEYBOARD` with `--keyboard`/`--mouse` flags, fixes missing `input_method` property on view events, dialog focus driven by Tab/arrow keys
- [app] GH-18 (WU5/5): keyboard dialog navigation test coverage, docs, and audit fixes — completes the keyboard-only TUI: Tab/arrow-key focus cycling and typing work across Save/Load/Exit dialogs and the startup restore prompt, with the yellow cursor and light-blue toolbar shortcuts (F5 Save, F6 Load, Esc Exit) documented in `docs/PRODUCT_SPEC.md` as Feature 7. This completes GH-18: `--keyboard` is now a fully playable alternative to the default mouse-driven TUI, with `--console` and a non-interactive terminal both still falling back to the line-based console UI.

## [0.5.1] — 2026-09-11 (PR #27 merged)

### Changed
- [app] GH-23: splits `Presenter` into a shared core plus `ConsolePresenter`/`TuiPresenter` sub-interfaces, with `BasePresenter` (shared domain-mutation flows) and `ConsolePresenterImpl`/`TuiPresenterImpl` replacing the single `PresenterImpl` — `ViewImpl` now depends only on `ConsolePresenter`, `TuiView` only on `TuiPresenter`. Test doubles and presenter test files restructured to match (`FakePresenter` split into `FakeConsolePresenter`/`FakeTuiPresenter`; `PresenterImpl*Test.kt` renamed/split per production class). Pure internal refactor - no user-visible behavior change.

## [0.5.0] — 2026-09-11 (PR #25 merged)

### Added
- [wip] GH-3 (WU1/5): launch-mode resolution (`--console` opt-in, default mouse TUI once it exists, automatic fallback to console with no interactive terminal) and its `app_launched`/`input_method` analytics plumbing — no user-visible behavior change yet, still runs the console UI either way until WU3.
- [wip] GH-3 (WU2/5): terminal layer (`Terminal`, `AnsiTerminal`, `TerminalEvent`, `TerminalInputParser`, `FakeTerminal`) — decodes raw terminal bytes (SGR-1006 and legacy X10 mouse reports, single-byte keys) into events; not wired into the app yet, still unreachable until WU3 builds the board screen on top of it.
- [wip] GH-3 (WU3/5): board screen (`BoardLayout`, `HitTarget`, `ScreenState`, `ScreenRenderer`, `TuiView`) — a mouse-playable 4x4 board with the 16 shift arrows, now wired as the default `LaunchMode.MOUSE` UI. Save/Load/Exit toolbar buttons render but are inert (no dialogs yet), and there's no Congratulations screen — both land in WU4/WU5.
- [wip] GH-3 (WU4/5): Save/Load/Exit dialogs and the startup restore prompt (`Dialog`, `DialogLayout`, `TuiView`'s modal loops) — the mouse-driven TUI now has full feature parity with the console UI (save, load, exit with save-before-quitting, restoring a save at launch), plus their `dialog_cancelled`/`screen_*_dialog` analytics. Still no Congratulations screen — a solved board disables the arrows with nothing to hand off to yet; that lands in WU5.
- [app] GH-3 (WU5/5): Congratulations screen — once the board is solved, the title switches to `Congratulations ✓`, the shift arrows dim and stop responding to clicks, and the toolbar (Save/Load/Exit) stays fully live, with `screen_congratulations` firing once on the transition. Loading a different, unsolved save from this screen re-enables the arrows and title immediately. This completes GH-3: the mouse-driven TUI is now the default launch experience, with `--console` opting back into the line-based console UI.

## [0.4.0] — 2026-09-10 (PR #15 merged)

### Added
- [app] GH-12: `exit`/`quit` command, with a save-before-quitting prompt (`save_command_used`/`exit_command_used` analytics) that re-prompts on an unusable name and aborts the quit — rather than losing the session — if the save attempt itself fails.

## [0.3.0] — 2026-09-10 (PR #14 merged)

### Added
- [app] GH-6 (WU1/4): line-based console input reader with EOF termination for play mode.
- [app] GH-6 (WU2/4): storage package (SaveRepository/SavedBoard/FileSaveRepository/SaveFileFormatException) + BoardModel.restoreState — persistence layer, not yet wired to the UI.
- [app] GH-6 (WU3/4): `save <file-name>` / `load <file-name>` console commands, wired view → presenter → storage, with `save_command_used`/`load_command_used` analytics events.
- [app] GH-6 (WU4/4): startup restore prompt — on launch, offers to resume an existing save (a yes/no question for exactly one save, a name prompt for two or more, re-prompting on an unrecognized name), with `startup_restore_prompt_shown`/`startup_restore_decision` analytics events.

## [0.2.0] — 2026-09-10 (PR #11 merged)

### Changed
- [app] GH-10: 1-based tile values and row/column indices

## [0.1.1] — 2026-09-10 (PR #5 merged)

### Added
- [test] GH-4: automated test coverage for `board_model` and `presenter` (37 tests), independent of any view/UI implementation.
- [ci] GH-4: GitHub Actions workflow running `./gradlew build` on push/PR.

### Fixed
- [app] GH-4: fix an off-by-one in the board shift bounds check (`shiftLeft`/`shiftRight`/`shiftUp`/`shiftDown`) that let an out-of-range index through and crashed instead of raising a clear error.
- [app] GH-4: fix `./gradlew run` not receiving keyboard input (the `application` plugin doesn't wire stdin by default).

### Changed
- [meta] GH-4: adopt Gradle as the project's build tool; replace the hand-rolled board shuffle with Kotlin's stdlib `IntArray.shuffle()`.

## [0.1.0] — 2026-09-10 (PR #1 merged)

### Added
- [meta] GH-2: adopt the yuriys-agentic-boyz multi-skill AI workflow (`.claude/commands`, `skills/`, `docs/`, `scripts/skill_router`, `styles/`), adapted to this project's Kotlin stack and GitHub Issues, with a calibrated model-tier mapping in `docs/MODEL_TIERS.md`.
