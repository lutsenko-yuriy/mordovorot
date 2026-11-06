# Architecture

## Related docs

- `docs/TECH_STACK.md` — languages, frameworks, tooling
- `docs/CODE_STYLE.md` — style rules
- `docs/CONSTRAINTS.md` — standing constraints that shape design trade-offs

## Overview

MVVM (Model-View-ViewModel): board_model + viewmodel + view packages, plus the `mordie`
opponent package (GH-83). The browser is the only client; the terminal client was dropped in
GH-228.

## Directory structure

```
shared/                  # GH-41: the platform-independent game as Kotlin Multiplatform modules
│                          # (`:shared:<name>`), each `jvm()` + `js { nodejs() }` (GH-103), so a
│                          # JVM-only API fails the build. Packages are unchanged; main code is in
│                          # `<module>/src/commonMain/kotlin/`, tests in `src/commonTest/kotlin/`
├── core/                # :shared:core
│   └── board_model/
│       ├── BoardModel.kt    # Board interface (domain contract)
│       ├── BoardSize.kt     # The valid board side range (MIN 3 / MAX 6 / DEFAULT 4) and its
│       │                      # validation - the one place the range lives (GH-44, GH-160); VERY_LARGE
│       │                      # 7-9 is only reachable through BoardImpl(maxSide), web only (GH-207)
│       ├── Solvability.kt   # Which boards shifts can solve (permutation parity on odd sides)
│       │                      # and the two-tile repair - the one place the rule lives (GH-74)
│       └── BoardImpl.kt     # IntArray-backed board state + shift/reset/newGame/isCorrect logic + move count.
│                              # `squareSide` is a read-only view over a private mutable field:
│                              # `newGame(side)` is the only thing that ever changes it (GH-44),
│                              # `resetGame()` reshuffles at the same size, always to a solvable board (GH-74)
├── analytics/           # :shared:analytics
│   └── analytics/
│       ├── AnalyticsService.kt             # Analytics abstraction — track(event, properties)
│       ├── NoopAnalyticsService.kt         # Default implementation; no SDK wired up yet
│       ├── InputMethodAnalyticsService.kt  # Decorator adding `input_method` to every forwarded
│       │                                     # event, without touching the viewmodel's call sites (GH-3)
│       └── PlatformAnalyticsService.kt     # Decorator adding `platform` (always `web` now)
│                                             # the same way (GH-110)
├── storage/             # :shared:storage - the save contract and the save formats; the browser client does the I/O
│   └── storage/
│       ├── SaveRepository.kt          # Persistence contract: list/exists/save/load/delete
│       ├── PreferenceRepository.kt    # Player preferences as plain strings: get/set (GH-139)
│       ├── SaveFileCodec.kt           # The save codec, the one public seam: decode reads the packed and the old text
│       │                                # format, dispatching on the first character; encode writes the packed one only (GH-237)
│       ├── PackedSaveFormat.kt        # The packed format: `~` + base64url of a bit stream (GH-237, ADR-0004)
│       ├── SaveBits.kt                # BitWriter / BitReader, varints and the Lehmer code of a permutation (GH-237)
│       ├── BoardLinkFormat.kt         # A board in a share link: side in 4 bits + Lehmer code, base64url (GH-243, ADR-0005)
│       ├── TextSaveFormat.kt          # The old nine-line text format, read-only from GH-237 on and deprecated (GH-241; `@Deprecated`, no removal date) (GH-110)
│       ├── SaveRules.kt               # The checks both formats share: arrangement, solution, ruled-out length (GH-237)
│       ├── SaveNames.kt               # Which save names are accepted, on every platform (GH-110)
│       ├── SaveFileFormatException.kt # Thrown on a malformed save (from the app, GH-110)
│       ├── SavedBoard.kt              # Data carrier: square side + tile arrangement (+ optional
│       │                                # SavedMordie, GH-83, and the labels' wire name, GH-139)
│       ├── SavedMordie.kt             # Original arrangement + Mordie's N + found flag (GH-83);
│       │                                # Mordie's solution and the solution-screen flag (GH-126),
│       │                                # whether the answer is proven shortest and R (GH-224)
│       └── SavedMove.kt               # One step of that solution; mirrors mordie.SolutionMove (GH-126)
├── mordie/              # :shared:mordie (depends on kotlinx-coroutines-core)
│   └── mordie/              # GH-83: the computer opponent - pure domain, no project imports
│       ├── MoveMetricHeuristic.kt   # Admissible lower bound in the GH-62 move metric:
│       │                              # ceil(wrong-row tiles/n) + ceil(wrong-column tiles/n)
│       ├── IdaStarSolver.kt         # Steppable IDA* (explicit stack, O(depth) memory): ruledOut,
│       │                              # solutionLength and its solution (GH-126); can resume
│       │                              # above a saved N
│       ├── SolutionMove.kt          # One move of a solution (row/column, index, amount), shared by
│       │                              # both solvers (GH-213)
│       ├── ThreeCycleSolver.kt      # Fast, non-minimal solver (GH-213): places tiles one per step
│       │                              # with row/column commutator 3-cycles; run by BestSolutionSolver (GH-222)
│       ├── BestSolutionSolver.kt    # GH-222: steps ThreeCycleSolver, then IdaStarSolver; keeps the
│       │                              # best solution so far, its source and whether it is proven shortest;
│       │                              # a kept best (GH-224) is the answer from the start and skips the fast solver
│       ├── MordieSearch.kt          # MordieSearch / SearchProgress / MordieSearchFactory (+ NONE) -
│       │                              # the seam tests replace with FakeMordieSearchFactory;
│       │                              # start(original, ruledOut, best) - best is the kept solution (GH-224)
│       ├── BackgroundMordieSearch.kt # The real search: one throttled coroutine on
│       │                               # Dispatchers.Default stepping a BestSolutionSolver until proven (GH-222), publishing a volatile snapshot;
│       │                               # onSnapshot forwards each one (the browser's worker, GH-218)
│       ├── SearchProtocol.kt        # GH-218: the page <-> Web Worker messages as plain strings
│       │                              # (start: arrangement + saved N + kept solution; progress, with the solution)
│       ├── Mordie.kt                # Opponent state: shift count, 5-shift checks, opening/resume
│       │                              # kind, found reveal, a shorter solution announced at a check
│       │                              # (MordieCheck, GH-223), frozen outcome, save snapshot;
│       │                              # plays every board size, 3x3 to 9x9 (GH-225)
│       └── MordieLine.kt            # MordieLine (sealed, incl. FoundUnproven / Shorter) /
│                                      # MordieCheck / MordieOutcome / MordieResult (incl.
│                                      # BEAT_MORDIE) / MordieSnapshot (what a save stores)
├── viewmodel/           # :shared:viewmodel - the game flows (api
│   │                      # dependency on the four modules above + coroutines)
│   └── viewmodel/
│       ├── GameViewModel.kt      # The shared viewmodel interface (GH-42 WU3) — shift/newGame/save/load plus the read-only query
│       │                           # surface (listSaves/saveExists/currentSaveName/isSolved/
│       │                           # boardState/squareSide/moveCount) and restoreOnStartup
│                           # and newGameFrom(arrangement), a fresh game on a given solvable board, and
│                           # startingBoard(), the board Mordie started from, null when it sits out (GH-243)
│       ├── GameViewModelImpl.kt  # Its one implementation (GH-42 WU3, collapsing the earlier
│       │                           # per-client viewmodels). Keeps shifts and the
│       │                           # queries; delegates every other flow to a usecase/ class. All
│       │                           # collaborators are injected, with no defaults (GH-78). Holds no
│       │                           # `View` reference — see the "viewmodel" section below
│       ├── GameViewModelFactory.kt # buildGameViewModel — builds GameViewModelImpl and its use
│       │                             # cases; the only way a platform gets one (GH-41)
│       ├── UiRequest.kt          # Sealed request/response type for the viewmodel→UI
│       │                           # interactions (ShowMessage/ConfirmRestore/ChooseSaveToRestore/
│       │                           # ChooseBoardSize (GH-44)/
│       │                           # ResolveUnsolvableSave (GH-74) + UnsolvableSaveChoice,
│       │                           # ChooseFittingLabels (GH-160)); ShowMessage.loadedSave names
│       │                           # the save a load restored (GH-166),
│       │                           # sent on
│       │                           # GameViewModel.uiRequests (GH-42 WU2)
│       ├── UiRequestChannel.kt   # The rendezvous channel behind uiRequests: ask/showMessage. One
│       │                           # per rebuild, shared by GameViewModelImpl, and every use case
│       │                           # (GH-78, GH-41)
│       ├── CurrentSaveName.kt    # The session's current save name (`var name: String?`): set on
│       │                           # save/load/repair, cleared on new game, prefills the Save dialog.
│       │                           # One per session; never persisted (GH-99)
│       ├── ShiftSource.kt        # What made a shift: ShiftKey {ARROW, HJKL} (GH-101),
│       │                           # PointerShift {ARROW, DRAG} (web, GH-110) or KeyShift (web,
│       │                           # its ShiftKey null when left-handed, GH-171); null for a click
│       ├── GameShiftSources.kt   # Which ShiftSources made this game's shifts, for board_solved.
│       │                           # shift_keys / shift_input. Reset on new game/load/repair; one
│       │                           # per session (GH-101, GH-110)
│       ├── UnsavedProgress.kt    # Whether the game has shifts no save holds: set by a shift,
│       │                           # cleared by new game/save/load/repair. Backs the web's
│       │                           # leave-site prompt; one per session (GH-110)
│       ├── SaveCatalog.kt        # Read-only SaveInfo views of the saves: infos (throws) /
│       │                           # listSaves / saveExists (swallow) / availableSavesMessage (GH-78)
│       ├── CatchingNonCancellation.kt # catchingNonCancellation — the use cases' catch-all: rethrows
│       │                                 # only CancellationException (GH-41)
│       ├── MordieHooks.kt         # GameViewModelImpl's Mordie part: binds it on new game/load/repair/
│       │                             # startup (sits out a loaded board already played on, GH-83), tracks
│       │                             # its analytics, words its line and ending (GH-83)
│       ├── MordieText.kt          # Every Mordie string (line bodies + the four endings), singular
│       │                             # `1 move` included (GH-83)
│       ├── SolutionScreen.kt      # The session's solution screen: its SolutionReplay, null off it.
│       │                             # One per session (GH-126)
│       ├── TileLabels.kt          # The six label schemes: wireName (analytics, saves, the
│       │                             # preference), title, maxSide, fits(side), label(tile, side), sampleRow.
│       │                             # NUMBERS is the default (GH-139)
│       ├── TileLabelsPreference.kt # TileLabels over a PreferenceRepository under `tile_labels`;
│       │                             # a failed read is no preference, a failed write is silent (GH-139)
│       ├── Direction.kt           # UP/DOWN/LEFT/RIGHT, what a key can mean (GH-18, shared by GH-171)
│       ├── TileSelection.kt       # Keyboard mode's selected tile (GH-170, replaced GH-101's line
│       │                             # selection): (row, col), move with wraparound, clampedTo(side)
│       ├── KeyScheme.kt           # Keyboard mode's keys (ADR-0003) per KeyboardLayout: boardKey gives
│       │                             # a BoardKey (select / shift; a left-handed shift has no ShiftKey),
│       │                             # solutionKey a SolutionKey, boardKeyLines the Controls key list's
│       │                             # first lines. The client adapts its own events to it (GH-171)
│       ├── KeyboardLayout.kt      # RIGHT_HANDED (default) / LEFT_HANDED: wireName (the preference
│       │                             # only, never analytics) and title (GH-170)
│       ├── KeyboardLayoutPreference.kt # KeyboardLayout under `keyboard_layout`, like
│       │                             # TileLabelsPreference (GH-170)
│       ├── SessionKeyboardLayout.kt # The session's layout: the preference read once; choose()
│       │                             # applies and writes it. One per session (GH-170)
│       ├── SessionTileLabels.kt   # The session's labels and TileLabelsSource: a save's labels
│       │                             # beat the preference, which beats NUMBERS. One per session,
│       │                             # so a mode switch keeps them; reads the preference once (GH-139).
│       │                             # A scheme that doesn't fit the side shows NUMBERS
│       │                             # (LETTERS_UNAVAILABLE), the preference kept (GH-160)
│       ├── SolutionReplay.kt      # Mordie's steps on their own copy of the original arrangement,
│       │                             # stepped one shift at a time (next/previous/restart); the
│       │                             # game's board, moves and Mordie never see it (GH-126)
│       ├── SolutionSteps.kt       # SolutionStep (one step, the shortest way round its line) and
│       │                             # the step list's text, `03. Col 3 ↑ ×1` (GH-126)
│       ├── SolutionState.kt       # What a view draws on the solution screen, plus GiveUpButton
│       │                             # (DIMMED/GIVE_UP/MORDIES_SOLUTION/HIDDEN) (GH-126); its moves,
│       │                             # the steps unformatted (GH-166)
│       └── usecase/               # One class per viewmodel flow, delegated to by GameViewModelImpl
│                                     # (GH-78): NewGameUseCase, SaveGameUseCase, LoadGameUseCase ->
│                                     # ResolveUnsolvableSaveUseCase (repair / new game, GH-74;
│                                     # both return LoadResult, GH-41), DeleteSaveUseCase (deletes a save and
│                                     # tracks save_deleted; not suspend, raises no request, GH-251),
│                                     # StartupRestoreUseCase (restore or
│                                     # size prompt, then Mordie; runs once per session), SolutionUseCase (the Give Up row,
│                                     # entering the solution screen, the replay and its
│                                     # analytics, GH-126), TileLabelsUseCase (the labels dialog's
│                                     # screen view and OK, GH-139), ControlsDialogUseCase (the
│                                     # Controls dialog's screen view, GH-170); both take `screen`
│                                     # from ScreenName.kt's one rule
└── testing/             # :shared:testing - plain JVM module of test doubles shared by every
                           # module's tests: FakeBoardModel / FakeSaveRepository / FakePreferenceRepository /
                           # RecordingAnalyticsService / FakeMordieSearch + FakeMordieSearchFactory /
                           # FakeViewModelUi (scripted UiRequest responder, GH-42 WU2)

web/                     # :web - the browser client (GH-110): `jvm()` + `js { browser(); nodejs() }`.
│                          # DOM-free code in src/commonMain (tested on the JVM and Node.js), the
│                          # page in src/jsMain. `./gradlew :web:serve -t` serves it on localhost:8080;
│                          # `build.yml`'s `deploy` job publishes it to GitHub Pages (GH-111)
├── src/commonMain/kotlin/web/
│   ├── WebGame.kt       # The browser's View: page intents (onArrow/onDrag*/onNew/onSave/onLoad/dialog answers,
│   │                    # GH-139's onLabels*, GH-163's onEscape, GH-171's onKey and onTilePressed) ->
│   │                    # GameViewModel, a WebState to the WebRenderer after every change; drains
│   │                    # uiRequests; tracks the view-layer analytics (screen views, dialog_cancelled);
│   │                    # sets each frame's announcement (GH-166)
│   ├── WebDialogFlows.kt # WebGame's Save, Load, size and can't-be-solved dialogs over its modal (GH-166)
│   ├── WebSolutionFlows.kt # WebGame's Give Up and the solution screen's replay (GH-126, split out in GH-171)
│   ├── WebControlsDialog.kt # The Controls dialog's flow over WebGame's modal and the session's mode (GH-171)
│   ├── PlayMode.kt / PlayModePreference.kt # Mouse or keyboard, kept under `input_mode` (GH-171)
│   ├── PressedWith.kt   # The session's last input, read per event as input_method (GH-171)
│   ├── ShareLink.kt     # GH-243 (WU1): `<page address>#b=<board>` built and read; playable, or rejected
│   │                      # as invalid / unsupported_size / unsolvable. storage.BoardLinkFormat (ADR-0005) holds the board's bits
│   ├── qr/              # GH-243 (WU1): a DOM-free QR encoder (byte mode, ECC M, versions 1-10: QrCode, ReedSolomon, QrTables, QrMasks) and its SVG path (QrSvg);
│   │                      # imports only the standard library, a boundary test checks it (as web.a11y)
│   ├── Clipboard.kt     # GH-243 (WU3): copy(text, done), which may be refused; DomClipboard (jsMain) tries the async API, then the old copy command on the selected link field
│   ├── PageAddress.kt   # GH-243 (WU2): the address bar's base, fragment and clearFragment (DomPageAddress in jsMain; a fake in the harness)
│   ├── WebShareFlows.kt # GH-243 (WU2): opening a link at startup and on hashchange, the Can't open and Open the shared board dialogs; (WU3) the Share dialog and Copy link, and a Clipboard seam (DomClipboard in jsMain; a fake in the harness)
│   ├── WebKeys.kt       # A key outside a dialog to a PageKey: ToolbarButton's digits, 0, or a shared
│   │                    # BoardKey or SolutionKey while the board has focus (GH-171); a non-Latin
│   │                    # key counts as the Latin letter at its position (GH-200)
│   ├── WebKeyPlay.kt    # WebGame's keys outside dialogs, tile presses and the TileSelection (GH-171)
│   ├── KeyHints.kt      # The web's Controls key list and the hints under the board, in play and on
│   │                    # the solution screen (GH-171)
│   ├── WebState.kt      # One frame: tiles (1-based), Moves, Mordie's spot, solved, status, WebDialog,
│   │                    # the dragged line's live offset; the Give Up row, the solution screen and
│   │                    # the replay shift to slide (GH-126); the tile labels and tileTexts (GH-139);
│   │                    # LabelOption.available and Labels.message (GH-160); the one-time focus (GH-163)
│   │                    # and announcement (GH-166); which tiles are home and Read board's words (GH-167)
│   ├── WebRenderer.kt   # fun interface render(WebState), and scrollSteps (GH-171) - the DOM on the page, a recorder in tests
│   ├── WebRequestRouter.kt # Answers each UiRequest (exhaustive `when`)
│   ├── WebLabelsDialog.kt # The Tile labels dialog's intents and flows over WebGame's modal (GH-160; the intents moved here in GH-243)
│   ├── WebModal.kt      # WebGame's dialog machinery: open/answer a WebDialog, the button that
│   │                    # opened it, the cancel's `via` (GH-163)
│   ├── DialogFocus.kt   # GH-163: Focus (a FocusTarget: the game's controls by data-focus key), each
│   │                    # dialog's default focus and title
│   ├── PageFocus.kt     # GH-164: fallbackFocus - where focus goes when its control is gone or disabled
│   ├── Arrow.kt         # Which arrow was clicked, with its viewmodel Direction (GH-201)
│   ├── Layout.kt        # layoutOf(width, height): WIDE (wider than tall) or TALL
│   ├── Slides.kt        # The eased slide after an arrow shift, a drag release or a replay shift
│   │                    # (GH-126), drawn by WrapPositions
│   ├── MessageFeed.kt   # Chat-like timing for Mordie's lines: the old one rises out, then the new rises in
│   ├── Launch.kt        # startWebGame: the page's wiring (PlatformAnalyticsService "web" wrapped
│   │                    # by InputMethodAnalyticsService "rich_ui"), app_launched {mode, layout,
│   │                    # app_version, host} (GH-111) and tile_labels (GH-139); webBoard(): the
│   │                    # page's BoardImpl(maxSide = 9), shared with WebHarness (GH-207)
│   ├── TextScale.kt     # GH-168: textScaleOf(root, appleBody): the system text size as a scale, never below 1;
│   │                    # rootFontPx: the root's size for it, null when the default already is
│   ├── Host.kt          # hostOf(hostname): PUBLIC_SITE, LOCALHOST or OTHER - only the bucket is sent (GH-111)
│   ├── Footer.kt        # footer(version): the footer line as Text/Link parts, worded as the README's Credits (GH-111)
│   ├── a11y/            # GH-179: web.a11y, screen readers and focus (also in jsMain below). web
│   │   │                # depends on it, not it on web: it imports only the standard library, the DOM
│   │   │                # and a few :shared:viewmodel types, pinned by A11yBoundaryTest (GH-201). The
│   │   │                # game's ARIA moving in is GH-201's next step; a Gradle module is future work
│   │   ├── Announcements.kt # GH-166: what a screen reader hears - shifts, the win, new game, load,
│   │   │                # solution steps - and the arrows' accessible names; Read board's
│   │   │                # row-by-row words, letters in the phonetic alphabet (GH-167)
│   │   └── FocusTarget.kt # GH-201: a control's data-focus key, and whether focus falls back from it
│   ├── drag/            # LineDrag: one drag in pixels - axis lock past a threshold, live offset,
│   │                    # snap to whole tiles mod the side; WrapPositions: a part-shifted line's tiles
│   └── storage/         # BrowserSaveRepository: one KeyValueStore entry per save under
│                        # `mordovorot.save.<name>`, in SaveFileCodec's format, SaveNames' rules;
│                        # BrowserPreferenceRepository: `mordovorot.pref.<key>` (GH-139)
├── worker/                              # :web:worker (GH-218): Kotlin/JS executable, depends only on
│                                        # :shared:mordie; its Main.kt runs BackgroundMordieSearch in a
│                                        # Web Worker and posts the changed snapshots. Built into
│                                        # mordie-worker.js; its shipToWeb task copies that into :web's
│                                        # generated resources (the worker project writes, because
│                                        # `serve` moves build directories, GH-124), and
│                                        # checkWorkerShipped fails a distribution without it
├── src/commonMain/kotlin/web/WorkerMordieSearch.kt # GH-218: the page's MordieSearch over a SearchPort
│                                        # (one worker per search; cancel terminates it); DOM-free
├── src/jsMain/kotlin/web/DomSearchPort.kt # GH-218: SearchPort over Worker("mordie-worker.js")
├── src/commonTest/kotlin/web/WorkerMordieSearchTest.kt # GH-218: with a fake port
├── src/jsMain/kotlin/web/Main.kt       # The page's composition root: BoardImpl, saves and
│                                       # preferences in localStorage, Mordie with the worker search,
│                                       # MainScope, the window's Layout, the beforeunload prompt;
│                                       # the footer, once, outside #app (GH-111); stopPinchZoom;
│                                       # applySystemTextSize first (GH-168)
├── src/jsMain/kotlin/web/DomGestures.kt # stopPinchZoom: on touch screens, cancels WebKit's pinch
│                                       # events, which iOS fires whatever touch-action says (GH-149)
├── src/jsMain/kotlin/web/DomRenderer.kt # WebState -> DOM, clicks -> WebGame
│                                       # intents; the page root holds a drag's pointer capture;
│                                       # runs Slides and MessageFeed on animation frames, redrawing
│                                       # only the tiles and Mordie's line; keeps the step list's
│                                       # scroll across frames, the current step in view (GH-126);
│                                       # the board sits in `.stage`, the box the tall layout's
│                                       # tiles are sized from (GH-147)
├── src/jsMain/kotlin/web/DomBoard.kt   # The board: tiles, arrows and pointer drags -> WebGame, split from DomRenderer (GH-171).
│                                       # Its cells are kept and rewritten across a slide; in keyboard mode, where
│                                       # WebState.keepsBoardFrom says, across a frame too, so a screen reader hears
│                                       # the live region rather than a refocused board
├── src/jsMain/kotlin/web/DomKeyboardMode.kt # GH-171: the selection's outlines and the key hints, drawn beside the board
├── src/jsMain/kotlin/web/DomTextSize.kt # GH-168: at load, reads the browser's font size and (iPhone/iPad)
│                                       # Dynamic Type from a removed `-apple-system-body` probe; sets
│                                       # `--text-scale`, and the root font size where the default isn't it
├── src/jsMain/kotlin/web/DomFooter.kt  # footerElement: <footer>, its links in a new tab (GH-111);
│                                       # wide-only parts in `.wide-only` (GH-147)
├── src/jsMain/kotlin/web/DomQr.kt      # GH-243: the Share dialog's QR code as an inline SVG from QrSvg, hidden from readers
├── src/jsMain/kotlin/web/DomDialogs.kt  # The modal dialogs, and Escape -> WebGame.onEscape (GH-163, listenForEscape);
│                                       # DomElements.kt: element/text/button helpers, and KeyboardEvent.composing
├── src/jsMain/kotlin/web/a11y/DomKeyboard.kt # GH-163: Tab wraps inside the open dialog, the page
│                                       # behind it `inert`, applyFocus puts a FocusTarget on the page;
│                                       # keeps the focused control across redraws, else the fallback
│                                       # it is given, and drops held Enter repeats (GH-164)
├── src/jsMain/kotlin/web/DomPlayKeys.kt # GH-171: keys outside dialogs -> WebGame.onKey, and PressedWith from
│                                       # every keydown and pointerdown, in the capture phase
├── src/jsMain/kotlin/web/DomSolution.kt # The step list, Prev/Next/Restart and the Give Up row (GH-126)
├── src/jsMain/kotlin/web/a11y/DomBoardGrid.kt # GH-167: during play, the tiles as a table for screen
│                                       # readers - a row element per board row, each cell heard as
│                                       # said (`delta, in place`), a wrapped copy aria-hidden - and the hidden Read board
│                                       # button, which says its words through DomAnnouncer. In keyboard
│                                       # mode a grid, one Tab stop, the selected cell its
│                                       # aria-activedescendant; on the solution screen a named group, one Tab stop
├── src/jsMain/kotlin/web/a11y/DomAria.kt # GH-201: every role and aria- attribute the page sets (dialogs, radios,
│                                       # lists, hidden, shortcuts); A11yBoundaryTest keeps them out of the rest of web
├── src/jsMain/kotlin/web/a11y/DomAnnouncer.kt # GH-166: says WebState.announcement through #announcer,
│                                       # emptied then refilled after 100 ms so a repeat is re-read
├── src/jsMain/kotlin/web/storage/LocalStorageStore.kt # KeyValueStore over window.localStorage
├── src/jsMain/resources/index.html     # The page: `<title>Mordovorot</title>`, links the seven
│                                       # sheets below in this order (the cascade relies on it), loads web.js;
│                                       # #announcer, the polite live region, next to #app so no redraw
│                                       # or `inert` touches it (GH-166); the `text-scale` meta, for
│                                       # Chrome for Android's text size (GH-168)
├── src/jsMain/resources/palette.css    # GH-165: the colour tokens, light and dark (one set of tile
│                                       # colours for both); linked first
├── src/jsMain/resources/styles.css     # The look: layout tokens, wide/tall by `orientation` (as layoutOf
│                                       # counts), panel, toolbar, footer; wide: the page is
│                                       # the window's height, the board's room from named column
│                                       # widths and a one-line footer, no size container (GH-148);
│                                       # past 16px tiles the page scrolls and rows wrap (GH-168)
├── src/jsMain/resources/buttons.css    # Buttons and the keyboard focus ring (GH-163), split from
│                                       # styles.css (GH-168)
├── src/jsMain/resources/fit.css        # GH-148: fitting phones: tall, the page is the window's
│                                       # height and tiles fit `.stage` (GH-147), scrolling past 16px
│                                       # tiles (GH-168); wide, the one-line footer and tighter spacing
│                                       # on short and narrow phones
├── src/jsMain/resources/keyboard.css   # GH-171: keyboard mode: arrows hidden, the selection's outlines,
│                                       # the board's focus ring, the key hints
├── src/jsMain/resources/board.css      # GH-153: tile colours by home row/column, arrows, solve
│                                       # animations; no arrows under `pointer: coarse` (GH-111);
│                                       # tile labels grow with `--text-scale`, capped (GH-168)
├── src/jsMain/resources/dialogs.css    # GH-153: the modal dialogs, the Tile labels picker (GH-139)
├── src/jsMain/resources/solution.css   # GH-153: `.app.solution` and the step list (GH-126); its
│                                       # tall `.steps` overrides the base `.steps` by coming later
├── src/commonTest/kotlin/web/scenario/ # GH-110 end to end: WebGame over startWebGame's wiring
│                                       # (WebHarness), recording each WebState (startup, play,
│                                       # save/load, unsaved progress, drag, analytics; GH-126 give up;
│                                       # GH-139 tile labels; GH-166 announcements, as WebHarness.said)
├── src/commonTest/kotlin/web/FooterTest.kt # GH-111: the footer's wording and links
├── src/commonTest/kotlin/web/HostTest.kt # GH-111: hostOf's buckets
├── src/commonTest/kotlin/web/LayoutTest.kt, SlidesTest.kt, MessageFeedTest.kt
├── src/commonTest/kotlin/web/a11y/      # AnnouncementsTest (GH-179); DialogFocusTest, PageFocusTest are in web/ (GH-201)
├── src/commonTest/kotlin/web/TextScaleTest.kt # GH-168: textScaleOf's ratios, floor and bad readings;
│                                       # when rootFontPx leaves the default alone
├── src/commonTest/kotlin/web/drag/     # LineDragTest, WrapPositionsTest
├── src/commonTest/kotlin/web/storage/  # BrowserSaveRepositoryTest, BrowserPreferenceRepositoryTest
│                                       # over InMemoryKeyValueStore
├── src/jvmTest/kotlin/web/IndexPageTest.kt # Pins the title rule (the title appears only in the tab)
│                                       # and relative asset URLs (GH-111: the public site's sub-path);
│                                       # every sheet linked, in order (GH-153); #announcer (GH-166);
│                                       # the `text-scale` meta (GH-168)
├── src/jvmTest/kotlin/web/a11y/AnnouncerStylesTest.kt # GH-166: .visually-hidden clips, never hides from
│                                       # screen readers (no display: none or visibility: hidden)
├── src/jvmTest/kotlin/web/a11y/A11yBoundaryTest.kt # GH-201: nothing in web.a11y imports another package of web
├── src/jvmTest/kotlin/web/a11y/KeyboardFocusStylesTest.kt # GH-163: the focus ring waits for the first key
├── src/jvmTest/kotlin/web/BuildInfoTest.kt, CreditsTest.kt # GH-111: the stamped version is the
│                                       # build's; the footer's credit matches the README's
├── src/jvmTest/kotlin/web/PhoneFitStylesTest.kt # GH-147, GH-148: pins the tall and wide fit rules
│                                       # (styles.css, fit.css, solution.css)
├── src/jvmTest/kotlin/web/TextSizeStylesTest.kt # GH-168: text in rem, tile labels' cap; with
│                                       # larger text, rows and step lines wrap, the columns beside
│                                       # the board leave it 16px tiles, never wider than the window
├── src/jvmTest/kotlin/web/ColourContrastStylesTest.kt # GH-165: palette.css's text pairs, non-text
│                                       # edges and every tile label of every board meet WCAG AA
│                                       # in both themes; pins board.css's tile formula (Wcag.kt)
└── build/generated/buildInfo/          # BuildInfo.VERSION, written by :web:generateBuildInfo from
                                        # the root build.gradle.kts's version (GH-111)
```

The root project holds no code (GH-228): it only aggregates the modules. Each `shared/` module
and `web` use KMP's source sets (`src/commonMain/kotlin`, `src/commonTest/kotlin`) — see
"Dependencies" below.

## Layers

Classic MVVM. `board_model` is an interface + one implementation; the `view` is the browser's
`web.WebGame` (no interface: it is the only client).
`viewmodel` (GH-42) is the shared `GameViewModel` interface + one `GameViewModelImpl`
(`:shared:viewmodel`, GH-41),
holding no reference to any `View` — it communicates with whichever `View` is driving it
through a request channel instead of a constructor-injected dependency. Callers should always depend on an interface, not an impl,
to keep layers swappable.

### board_model (Domain)
Core game state and rules: board array, shifting, reset, and the win check
(`isCorrect`). No dependency on `viewmodel` or `view`.

**Board size (GH-44):** the board stays square, with a side of 3–6 (default 4; 6 since GH-160).
`BoardImpl(maxSide)` widens that limit (default `BoardSize.MAX`); only the web builds it with 9, for
the very large sizes 7–9 (`BoardSize.VERY_LARGE`, GH-207), so a board stops at 6 elsewhere.
`BoardSize` holds that range and its validation — the single place it lives, per
this layer's "range validation lives in `board_model`, the view translates but
does not validate" rule (see the 1-based display dialect note under `view`).
`BoardImpl.newGame(side)` is the only operation that ever changes the side: it
validates first, so an out-of-range value throws with the current board left
untouched, then reallocates and shuffles. `resetGame()` keeps its older meaning —
reshuffle at the same size. Every shuffle is solvable (GH-74): `Solvability`
holds the parity rule, and `resetGame()` swaps tiles 0/1 when the shuffle lands
on an unsolvable board. Nothing resizes an *in-progress* board: every
size-setting surface starts a fresh game instead. The side mutates in place rather than the board instance being swapped.

**Move count (GH-62):** `BoardImpl` owns the move rule — a move is a run of
consecutive shifts on one row or column. Each shift checks bounds first, then
counts a move only if its line differs from the last shift's. `resetGame`/`newGame`
zero the count; `restoreState(state, moveCount)` sets it and clears the last line,
so the first shift after a load always starts a new move.

### mordie (GH-83: the computer opponent)
Pure domain: plain `IntArray` arrangements, no dependency on `board_model`,
`storage`, `viewmodel` or `view` (`viewmodel` → `mordie`). `IdaStarSolver` searches the
game's original arrangement for its shortest solution in the GH-62 move metric, with
`MoveMetricHeuristic` as the admissible bound. It runs in slices, so
`BackgroundMordieSearch` can throttle it (about half of one core; the constants sit in
one place for the GH-41 mobile port), and memory stays O(depth). A search started from a
saved N begins at N + 1, so it never re-reports below it. In the browser the search runs in a
Web Worker (GH-218): `SearchProtocol` carries the start and the progress between the page's
`WorkerMordieSearch` and the worker, so the page's timers aren't delayed and a hidden tab keeps
searching. Loading an unproven save (GH-224) starts the search with the kept solution, so the
`BestSolutionSolver` skips the fast solver and goes on from the saved N. `Mordie` turns the player's
shifts into the line state: a check every 5 shifts, a reveal of "found", and the outcome
frozen on solve, using Mordie's live state at that shift. Its `MordieSnapshot` (with the proven flag and R, GH-224) mirrors
`storage.SavedMordie` on purpose, so `mordie` never imports `storage`; `MordieHooks` maps
between the two. The page builds one `Mordie` per session. `GameViewModelImpl` reaches it through `MordieHooks`, which
`buildGameViewModel` builds so it tracks through the decorated analytics.
Mordie plays every board size, 3x3 to 9x9 (GH-225); only a loaded board already played on, with no
Mordie data, is sat out through `Mordie.clear()` (GH-83).
Test builders default to an inert `Mordie(MordieSearchFactory.NONE)`, so unit tests never
leave real searches running.

### viewmodel (GH-42: ViewModel-style, no View reference)
**Status: landed (WU4/4 of the GH-42 redesign — final WU).** Everything described below is
real and in use; the `presenter` package/naming this superseded is gone.

Mediates between `view`, `board_model`, and `storage`, but never calls into `view`
directly. `GameViewModel` is one interface (shift/newGame/save/load plus the
query surface a View needs — `isSolved`/`boardState`/`squareSide`/`moveCount`/`listSaves`/
`saveExists`), and `GameViewModelImpl` is its one implementation. It keeps the shifts and
queries and delegates each other flow to a use case in `viewmodel.usecase` (GH-78), all
sharing one `UiRequestChannel`. Every collaborator is a constructor param with no
default: `buildGameViewModel` wires them; tests build it through `testViewModel`'s fakes.

The interactions that used to be blocking calls into `View` (`showMessage`,
`confirmRestore`, `chooseSaveToRestore`, and later the size and unsolvable-save prompts)
are now `viewmodel.UiRequest<R>` values sent on `ViewModel.uiRequests`, a
`Channel.RENDEZVOUS` the owning `View` drains in a sibling coroutine and answers via
`UiRequest.respond`. `WebGame` hands each to `WebRequestRouter`, which routes it to the
web dialog that answers it; `ShowMessage` becomes the status line. Rendezvous delivery
(the channel has zero buffer, and `UiRequestChannel.ask()` doesn't return until the View has actually
handled the request) is what keeps message/prompt ordering identical to the old
blocking-call behavior. The one invariant that keeps this deadlock-free: a View's
request handler must never call a request-raising `ViewModel` method
(`saveGame`/`loadGame`/`restoreOnStartup`) from inside
itself — only the plain query methods are safe there, and `deleteSave` (GH-251), which is not
`suspend` and raises no request. All of them are `suspend`; a cancelled coroutine resumes them with `CancellationException`, which
each use case's catch-all (`catchingNonCancellation`) rethrows rather than swallows (ordinary
structured-concurrency hygiene).

**Mordie wiring (GH-83).** `ViewModel.mordieLine()` (the text after `Mordie:`, or `null`)
and `mordieEnding()` (the solved title, or `null` → `Congratulations ✓`) are plain
queries. `shift()` calls `mordie.onShift()` and then, on solve, `mordie.onSolved()`;
`newGame` starts Mordie fresh; `saveGame` passes `mordie.snapshot()`. A
successful load binds Mordie through one rule: a solved save with Mordie data gets that
outcome, a solved save without it clears Mordie, a found save resumes found, a searching
save with a solvable original resumes at its N, and anything else (and a Repair) starts
fresh from the loaded board if it has no moves yet, or clears Mordie if it has. All of this
lives in `MordieHooks`. In the web, `WebState` carries the line and the solved title
(`mordieEnding() ?: WebGame.SOLVED_TITLE`). Every `restoreOnStartup` path ends with
`mordie.ensureStarted`, which does nothing once Mordie is bound.

**Give Up and the solution screen (GH-126).** Mordie's search keeps its solution's moves
(`SearchProgress.solution`), `Mordie` holds them, and a found save stores them. `SolutionUseCase`
owns the rest: `giveUpButton()` from Mordie's state and the board, entering the screen
(`giveUp()`, `showSolution()` after a win, or a load of a solution-screen save), and the
replay. On the screen the board holds the original arrangement with 0 moves, which is exactly
what a save there writes; `SolutionReplay` steps its own copy, so a replay never counts moves,
feeds Mordie, fires `board_solved` or sets unsaved progress. While it is open,
`boardState()` is the replay's, `shift()` does nothing and `mordieLine()` says how many moves
Mordie made. `replayNext()`/`replayPrevious()` return the one shift they made, so a view can
animate it. `SolutionScreen` is one per session, like `CurrentSaveName`. The give-up confirmation is the view's, like
`[ New ]`'s size picker, not a `UiRequest`: `WebGame` asks, then
calls the synchronous `giveUp()`. `screen_solution` is tracked here rather than in the views,
since all four ways onto the screen are shared flows.

### view
UI only: `web.WebGame` drives the page, renders the board and
calls into `GameViewModel`. Should not manipulate `board_model` directly. The terminal client
(GH-3, GH-18) and the line-based console mode (GH-79) are gone (GH-228).

**1-based display dialect (GH-10):** displayed tile values are 1-based by default;
`board_model` and `storage` stay 0-based. `viewmodel.TileLabels` is the single
translation point: `NUMBERS` adds 1, and the other schemes (GH-139) label the 0-based
tile their own way. `WebState.tileTexts` is read from `GameViewModel.tileLabels()` on every frame, so nothing
parses a typed index.
Range validation lives in `board_model` (`BoardImpl`'s bounds check), not `view`.

`BoardImpl.restoreState` and `SaveFileCodec`'s validation errors avoid
stating an explicit numeric range (e.g. "contain each of N tile values
exactly once" rather than "permutation of 0..N-1") so they read correctly
next to the 1-based board when shown in a Load dialog message.

### storage
Persistence for save games and preferences. `SaveRepository` is the contract `viewmodel`
depends on; the web's `BrowserSaveRepository` is the only class that touches the saves, one
`localStorage` entry each (deleted by `KeyValueStore.remove`, without decoding it, so a corrupted save can go too, GH-251), and `BrowserPreferenceRepository` (GH-139) the preferences.
`PreferenceRepository` is a plain string store, so `storage` still knows no viewmodel types;
`viewmodel.TileLabelsPreference` gives it a type. The save format (`SaveFileCodec`) and the accepted names (`SaveNames`)
live in `:shared:storage` so every platform reads and writes the same saves (GH-110). No dependency on `board_model`, `viewmodel`, or `view` — it
deals in plain data (`SavedBoard`), not domain objects.

Text save format (the `.save` text of the terminal days, read-only since GH-237 and deprecated since GH-241: `TextSaveFormat` is `@Deprecated`, and `SaveFileCodec.decode` suppresses the warning because old saves must keep loading; no removal date; GH-228 has no import): line 1 is the square side, line 2 the space-separated tile
values, line 3 the move count (GH-62). A missing or blank line 3 (a save from
before GH-62) loads as 0; a non-integer or negative one is a
`SaveFileFormatException`. GH-83 adds two optional lines: line 4 is the original
arrangement and line 5 is `<N> found` or `<N> searching`. A missing or blank line 4 (an
older save, or a GH-74 Repair save) means no Mordie data. Once line 4 is present, a bad
line 4 or 5 is a `SaveFileFormatException`. GH-126 adds two more: line 6 is Mordie's solution on
a found save, space-separated `r<i>:<k>`/`c<i>:<k>` (rotate row i right, or column i down, by k),
and line 7 is `solution` on a save made on the solution screen. A missing or blank line means
neither; a solution that doesn't solve the original in N moves is a `SaveFileFormatException`.
GH-139 adds line 8, the tile labels' wire name, which every save of that era wrote; lines 4-7 are blank
where they don't apply, which older builds already read as "nothing there". A missing or blank
line 8 means no labels, and any other word is kept, so a name this build doesn't know loads with
the preference rather than as a corrupted save.
GH-224 adds line 9, `unproven <R>`, written only for a found answer not proven shortest, R being the
largest length the exact search had ruled out (0 to N-2). A missing line, or a first word this build
doesn't know, reads as proven; `unproven` on a searching save or with R out of range is a
`SaveFileFormatException`. Old builds stop at line 8 and read such a save as proven.

Packed format (GH-237, ADR-0004; the only format written; the text format above is read-only): a save that starts with `~`
is base64url, no padding, of a bit stream: a header (the side in 4 bits, the labels in 3 bits, the status in 2 bits,
the solution-screen and has-solution flags), the labels' name when it is not one of the known ones, the move count
as a varint, the current board and Mordie's original as Lehmer codes (a permutation as digits of shrinking range),
N and, when unproven, R as varints, and last Mordie's solution, one joint number (axis, index, shift) per move.
Every width that depends on the side is read after the side. `SaveFileCodec.decode` sends a save starting with `~`
to the packed reader and anything else to the text reader, so old saves keep loading, and `encode` accepts a side of 2 to 15 only; a save that fails either is a
`SaveFileFormatException`. A 9x9 save with a 340-move solution is about 600 characters (2.1 KB as text).

### analytics
Cross-cutting: `AnalyticsService` is injected into `viewmodel` (and any layer
that needs to track an event), currently backed by `NoopAnalyticsService`.
`InputMethodAnalyticsService` (GH-3) decorates another `AnalyticsService`,
adding an `input_method` property (`keyboard` or `rich_ui`, the press that fired the event,
GH-171; `mouse` went with the terminal, GH-228) to every forwarded event — the decorator pattern lets the page distinguish events
by input mode without `GameViewModelImpl` itself knowing which UI mode is
running. `PlatformAnalyticsService` (GH-110) adds `platform` the same way; it is the
innermost decorator, wrapping the outermost service in `startWebGame`, so
`app_launched` and `input_mode_switched` carry it too. Its value is always `web` now; the property
stays for the mobile clients.

`board_solved` (GH-62) is tracked in `GameViewModelImpl`, not a View: every shift goes
through one private `shift` helper that fires it when the shift turns an unsolved
board into a solved one, so it covers both input modes. Loads bypass that helper,
so restoring a solved save never fires it. GH-83 adds `mordie_result`/`mordie_moves` to it
and tracks `mordie_solution_found` from the same helper, when a 5-shift check reveals
the solution. `shiftRow`/`shiftColumn` (GH-110, a web drag) run the same helper over several
steps: each is a board shift Mordie counts, and solved is checked once at the end, so a drag
passing through the solved board doesn't fire it. `shift_keys`/`shift_input` follow the solving
shift's `ShiftSource`.

### Composition root (GH-78)
`web`'s `Main.kt` (GH-110) is the only production code that constructs collaborators. Production
classes take every collaborator as a constructor parameter with no real-implementation default;
value defaults (a board side, a flag) are fine. Tests build through `testing/`'s `testViewModel`
and the web scenario harness, which call the same functions with fakes.

It builds the page's
collaborators and hands them to `startWebGame` (`Launch.kt`), which decorates analytics
(`platform: web`, then `input_method` from the last press for everything but `app_launched`), calls
`buildGameViewModel` and starts a `WebGame`.
`Main.kt` also passes the stamped `BuildInfo.VERSION` and `hostOf(window.location.hostname)` for
`app_launched` (GH-111), and appends the footer to `<body>` once, outside `#app`, which
`DomRenderer` rebuilds (all of it every frame, but for keyboard mode's board, GH-171).
The web scenario tests go through the same `startWebGame` with fakes.

### Board-size surfaces (GH-44)
Two entry points, both of which *start a fresh game* rather than resizing the
running one: a startup prompt raised by
`StartupRestoreUseCase.restoreOnStartup` as `UiRequest.ChooseBoardSize`, and the page's `[ New ]`
button (1 in keyboard mode), which opens the
same picker. (The terminal's `--size=N` flag went with it, GH-228.) Both funnel into `GameViewModel.newGame(size, trigger)` →
`BoardImpl.newGame`, which is also where `new_game_size_selected` is tracked
(only on success). Save/load auto-resizes instead of rejecting a size
mismatch (WU5) — `BoardModel.restoreState` is the second way (besides
`newGame`) the board's side ever changes: it derives `squareSide` from the
restored state's own length, so loading a save of a different size resizes
the board to match rather than requiring a coincidental match. There is no
`size_mismatch` outcome any more; every successful `saves.load()` restores.

**Save size visibility (WU4).** Every surface that lists a save's name — the
startup restore dialog, the mid-game Load dialog, and the "not found"/"no save
named" messages — also shows that save's board size, so a
player can tell up front what size loading it will resize the board to.
`GameViewModel.listSaves()` returns `List<SaveInfo>` (`name` + nullable
`squareSide`, `viewmodel/SaveInfo.kt`) instead of bare names; `squareSide` is
`null` when the save can't be read (corrupted/IO error) rather than dropping
the entry. `UiRequest.ConfirmRestore`/`ChooseSaveToRestore` now carry
`SaveInfo` too. No save-file-format change — `SaveCatalog` derives each
size from `saves.load(name)?.squareSide`, one extra read per save already
being listed.
`SaveInfo.solvable` (GH-74) comes from the same read via `Solvability`;
`display()` appends `UNSOLVABLE_MARK` for a save that can't be solved.
`LoadGameUseCase` never restores an unsolvable save directly: it raises
`UiRequest.ResolveUnsolvableSave` (`atStartup` set from the startup restore) and
acts on the `UnsolvableSaveChoice`. REPAIR writes `Solvability.repaired` back
through `saves.save` and loads it. NEW_GAME raises
`ChooseBoardSize(origin = "unsolvable_save")` mid-game, or returns `LoadResult.NOT_LOADED` at
startup so `restoreOnStartup`'s own size prompt runs.
`ChooseBoardSize.origin` is what the page reports as
`screen_size_dialog.opened_from`. The page answers the unsolvable-save prompt with a
non-cancellable dialog.

The startup restore prompt runs once per session, and the size prompt raises only once it's
established nothing was actually restored (no saves, declined, or a failed load). With 2+ saves,
`restoreOnStartup` matches the answer against save names exactly; the page always answers with a
listed name, so anything else counts as declined (GH-79).

### Input mode (GH-3, GH-18, GH-30, GH-171)
`app_launched` fires once per page load. The page starts in the stored `PlayMode` (mouse on a
first visit). The Controls dialog (`[ Controls 6 ]` / 6, GH-170) marks the current mode and
`SessionKeyboardLayout.current`; OK applies the marked layout and, with the other mode marked,
switches `PlayMode` in place, tracking `input_mode_switched` (`trigger: controls_dialog`). Nothing
is rebuilt: the same board, saves, Mordie and viewmodel carry on, so a switch never reshuffles
(`docs/ANALYTICS_EVENTS.md`).

## Agent tooling (not part of the game)

`scripts/` holds the Claude Code workflow tooling, tested with Python `unittest` in `tests.yml`. Three
bash hooks are wired in the committed `.claude/settings.json` through `${CLAUDE_PROJECT_DIR}`:
`scripts/onboard/gate.sh` (SessionStart/UserPromptExpansion/PreToolUse, blocks work until `/onboard`
has run) and `scripts/alert/alert.sh` (GH-107: Notification/Stop, a macOS notification and spoken
alert when a session waits on the user, toggled per machine in the gitignored `.claude/alert.env`) and `scripts/curfew/curfew.sh` (GH-237,
GH-248: UserPromptSubmit; a hard curfew blocks prompts 23:00-07:59, a soft one warns the user and Claude
at :00/:15/:30/:45 of the hour before, once per mark per day, kept in the gitignored `.claude/curfew.state`; a non-hour start/end fails open).
`scripts/web/harness/` (GH-147; `phone_fit/` until GH-156) measures the built web game at phone sizes in headless Chrome
(`measure.sh`, see its README; a `landscape` preset and checks on all four edges, GH-148) and serves it to a real phone on the local network (`serve_lan.sh`). `styles_diff.sh` (GH-156)
compares every element's computed styles against a master bundle, so a CSS refactor can show the
game looks the same; its compare logic, `styles_diff.py`, is tested in `tests.yml` too. Its
`report=keys` (GH-174, `keys.js`) plays keys and clicks and reports focus, accessible names, the
ring and `inert` after each; `keys_check.sh` diffs the GH-163 dialog flows against
`tests/keys_expected.txt`, run by the agent rather than CI (it needs Chrome and a bundle).
`--text-size=<percent>` (GH-168) sets the throwaway profile's font size for both scripts, and
`fit=scroll` checks a page allowed to scroll at larger text. `input=keyboard` and `layout=left` (GH-171) seed
the preferences.

## Dependencies

- **Build tool:** Gradle (Kotlin DSL), via the wrapper (`./gradlew`) — pinned to 9.7.1.
  Versions shared across modules (Kotlin, kotlinx-coroutines) live in the
  `gradle/libs.versions.toml` version catalog (GH-109).
  The root project is an aggregate with no code (GH-228); JVM toolchain 17 for the JVM targets.
- **Kotlin Multiplatform (GH-41).** The `shared/` modules use `kotlin("multiplatform")`, the
  first-party JetBrains plugin (declared once in the root `plugins {}` with `apply false`). Each module
  has a `jvm()` target and a `js { nodejs() }` one: with only `jvm()`, `commonMain` would
  accept `java.lang.Math` and the like, so the JS target is the compiler check that shared
  code stays platform-independent. Since GH-103 it is a real target with a Node.js test task;
  KGP downloads Node.js and yarn, and `kotlin-js-store/yarn.lock` pins the npm packages.
  `:shared:testing` (the test doubles) is multiplatform too, so common tests can use them.
  No `expect`/`actual` — every platform seam is a plain interface. Their tests are in
  `commonTest` and run on both the JVM and Node.js (GH-103); only mordie's
  `BackgroundMordieSearchThreadTest` stays in `jvmTest`, since it reads the search from a
  second thread. `./gradlew check` runs them all. Module graph:
  `core`, `analytics`, `storage` and `mordie` depend on no other module; `viewmodel` has an
  `api` dependency on all four (its public types expose them).
  `:web` (GH-110) depends on `viewmodel`. Its `browser()` target makes KGP add
  webpack and webpack-dev-server (build time only, pinned in the same `yarn.lock`); the
  browser test task is off, so CI needs no headless browser. `serve` (GH-124) needs `-t`:
  `settings.gradle.kts` fails it fast without, and points every project's build directory to
  `build-serve` (outside `build/`, so `clean` leaves it), so its outputs don't collide with a
  `check`/`build` running alongside; one Kotlin compile daemon serves both, so
  `gradle.properties` sets `kotlin.daemon.jvmargs=-Xmx2g`.
  **Deployment (GH-111):** `build.yml`'s `deploy` job publishes `:web`'s production bundle
  (`jsBrowserDistribution`) to GitHub Pages after a green `master` build; the repo's Pages
  source is "GitHub Actions". The site lives under `/mordovorot/`, so `index.html` must load
  its assets by relative URLs (`IndexPageTest` pins this).
- **Test framework:** `kotlin("test")` on the JUnit 5 platform (`useJUnitPlatform()`).
  No mocking library — hand-written fakes; doubles of
  shared types are in the `:shared:testing` module.
- **`kotlinx-coroutines-core` (GH-42) — the project's first third-party runtime
  dependency.** Powers `viewmodel`'s View-request channel (see above): `ViewModel`'s
  suspend functions and `Channel`. It cleared the zero-third-party bar because it's the
  JetBrains-maintained concurrency primitive the KMP UI layers need anyway, and it's the
  multiplatform artifact, so the same coordinate resolves per target. The shared modules'
  common tests use `kotlinx-coroutines-test`'s `runTest` (GH-103), since `runBlocking` doesn't
  exist on JS; checkup CHK-2026-10-14-light-2 had dropped it as unused. `withTimeout` guards
  the tests where a deadlock would otherwise hang silently. The page runs on one event loop, so the request/response ordering stays deterministic.
  **One exception (GH-83):** `BackgroundMordieSearch` runs the Mordie search as one
  throttled coroutine on `Dispatchers.Default`. It never touches the UI request channel;
  the event loop only reads its volatile progress snapshot.
- Beyond that, production code has no dependencies past the Kotlin standard library
  (board shuffling uses `kotlin.collections.shuffle()`, not a custom implementation).
