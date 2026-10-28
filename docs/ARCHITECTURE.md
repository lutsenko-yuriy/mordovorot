# Architecture

## Related docs

- `docs/TECH_STACK.md` — languages, frameworks, tooling
- `docs/CODE_STYLE.md` — style rules
- `docs/CONSTRAINTS.md` — standing constraints that shape design trade-offs

## Overview

MVVM (Model-View-ViewModel): board_model + viewmodel + view packages, plus the `mordie`
opponent package (GH-83).

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
│       │                      # validation - the one place the range lives (GH-44, GH-160)
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
│       └── PlatformAnalyticsService.kt     # Decorator adding `platform` (`terminal`/`web`) the
│                                             # same way (GH-110)
├── storage/             # :shared:storage - the save contract and text format; the file I/O is the app's
│   └── storage/
│       ├── SaveRepository.kt          # Persistence contract: list/exists/save/load
│       ├── PreferenceRepository.kt    # Player preferences as plain strings: get/set (GH-139)
│       ├── SaveFileCodec.kt           # The save text format: encode/decode with all its validation (GH-110)
│       ├── SaveNames.kt               # Which save names are accepted, on every platform (GH-110)
│       ├── SaveFileFormatException.kt # Thrown on a malformed save (from the app, GH-110)
│       ├── SavedBoard.kt              # Data carrier: square side + tile arrangement (+ optional
│       │                                # SavedMordie, GH-83, and the labels' wire name, GH-139)
│       ├── SavedMordie.kt             # Original arrangement + Mordie's N + found flag (GH-83);
│       │                                # Mordie's solution and the solution-screen flag (GH-126)
│       └── SavedMove.kt               # One step of that solution; mirrors mordie.SolutionMove (GH-126)
├── mordie/              # :shared:mordie (depends on kotlinx-coroutines-core)
│   └── mordie/              # GH-83: the computer opponent - pure domain, no project imports
│       ├── MoveMetricHeuristic.kt   # Admissible lower bound in the GH-62 move metric:
│       │                              # ceil(wrong-row tiles/n) + ceil(wrong-column tiles/n)
│       ├── IdaStarSolver.kt         # Steppable IDA* (explicit stack, O(depth) memory): ruledOut,
│       │                              # solutionLength and its SolutionMoves (GH-126); can resume
│       │                              # above a saved N
│       ├── MordieSearch.kt          # MordieSearch / SearchProgress / MordieSearchFactory (+ NONE) -
│       │                              # the seam tests replace with FakeMordieSearchFactory
│       ├── BackgroundMordieSearch.kt # The real search: one throttled coroutine on
│       │                               # Dispatchers.Default, publishing a volatile snapshot
│       ├── Mordie.kt                # Opponent state: shift count, 5-shift checks, opening/resume
│       │                              # kind, found reveal, frozen outcome, save snapshot;
│       │                              # MAX_SIDE 5 and sitOut() for larger boards (GH-160)
│       └── MordieLine.kt            # MordieLine (sealed) / MordieOutcome / MordieResult /
│                                      # MordieSnapshot (what a save stores)
├── viewmodel/           # :shared:viewmodel - everything but Exit and session control (api
│   │                      # dependency on the four modules above + coroutines)
│   └── viewmodel/
│       ├── GameViewModel.kt      # The shared viewmodel interface (GH-42 WU3; Exit split out for
│       │                           # GH-41) — shift/newGame/save/load plus the read-only query
│       │                           # surface (listSaves/saveExists/currentSaveName/isSolved/
│       │                           # boardState/squareSide/moveCount) and restoreOnStartup
│       ├── GameViewModelImpl.kt  # Its one implementation (GH-42 WU3, collapsing BaseViewModel +
│       │                           # ConsoleViewModelImpl + TuiViewModelImpl). Keeps shifts and the
│       │                           # queries; delegates every other flow to a usecase/ class. All
│       │                           # collaborators are injected, with no defaults (GH-78). Holds no
│       │                           # `View` reference — see the "viewmodel" section below
│       ├── GameViewModelFactory.kt # buildGameViewModel — builds GameViewModelImpl and its use
│       │                             # cases; the only way a platform gets one (GH-41)
│       ├── UnsolvableSaveExit.kt # The platform hook for Exit on an unsolvable save, run after the
│       │                           # shared flow returns (GH-41); with NONE, Exit
│       │                           # just ends the load
│       ├── UiRequest.kt          # Sealed request/response type for the viewmodel→UI
│       │                           # interactions (ShowMessage/ConfirmRestore/ChooseSaveToRestore/
│       │                           # ConfirmSaveBeforeExit/PromptSaveName/ChooseBoardSize (GH-44)/
│       │                           # ResolveUnsolvableSave (GH-74) + UnsolvableSaveChoice,
│       │                           # ChooseFittingLabels (GH-160)); ShowMessage.loadedSave names
│       │                           # the save a load restored (GH-166),
│       │                           # sent on
│       │                           # GameViewModel.uiRequests (GH-42 WU2)
│       ├── UiRequestChannel.kt   # The rendezvous channel behind uiRequests: ask/showMessage. One
│       │                           # per rebuild, shared by GameViewModelImpl, every use case and
│       │                           # the terminal's Exit (GH-78, GH-41)
│       ├── CurrentSaveName.kt    # The session's current save name (`var name: String?`): set on
│       │                           # save/load/repair, cleared on new game, prefills the Save dialog.
│       │                           # One per session, made by buildGameSession, so it survives mode
│       │                           # switches; never persisted (GH-99)
│       ├── ShiftSource.kt        # What made a shift: ShiftKey {ARROW, HJKL} (GH-101) or
│       │                           # PointerShift {ARROW, DRAG} (web, GH-110); null for a click
│       ├── GameShiftSources.kt   # Which ShiftSources made this game's shifts, for board_solved.
│       │                           # shift_keys / shift_input. Reset on new game/load/repair; one
│       │                           # per session, made by buildGameSession (GH-101, GH-110)
│       ├── UnsavedProgress.kt    # Whether the game has shifts no save holds: set by a shift,
│       │                           # cleared by new game/save/load/repair. Backs the web's
│       │                           # leave-site prompt; one per session, the terminal never reads it (GH-110)
│       ├── SaveCatalog.kt        # Read-only SaveInfo views of the saves: infos (throws) /
│       │                           # listSaves / saveExists (swallow) / availableSavesMessage (GH-78)
│       ├── CatchingNonCancellation.kt # catchingNonCancellation — the use cases' catch-all: rethrows
│       │                                 # only CancellationException, since no session-control
│       │                                 # exception passes through shared flows (GH-41)
│       ├── MordieHooks.kt         # GameViewModelImpl's Mordie part: binds it on new game/load/repair/
│       │                             # startup (sits out a loaded board already played on, and any side
│       │                             # above Mordie.MAX_SIDE, GH-160), tracks
│       │                             # its analytics, words its line and ending (GH-83)
│       ├── MordieText.kt          # Every Mordie string (line bodies + the four endings), singular
│       │                             # `1 move` included (GH-83)
│       ├── SolutionScreen.kt      # The session's solution screen: its SolutionReplay, null off it.
│       │                             # One per session, so a mode switch keeps it (GH-126)
│       ├── TileLabels.kt          # The six label schemes: wireName (analytics, save line 8, the
│       │                             # preference), title, maxSide, fits(side), label(tile, side), sampleRow.
│       │                             # NUMBERS is the default (GH-139)
│       ├── TileLabelsPreference.kt # TileLabels over a PreferenceRepository under `tile_labels`;
│       │                             # a failed read is no preference, a failed write is silent (GH-139)
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
│                                     # ResolveUnsolvableSaveUseCase (repair / new game / exit, GH-74;
│                                     # both return LoadResult, GH-41), StartupRestoreUseCase (restore or
│                                     # size prompt, then Mordie; takes `startupRestoreDone`, which
│                                     # GameSession seeds true after a mode switch (GH-30), and
│                                     # `sizeChosenAtLaunch`, which also fits the labels to the
│                                     # launch size, GH-160), SolutionUseCase (the Give Up row,
│                                     # entering the solution screen, the replay and its
│                                     # analytics, GH-126), TileLabelsUseCase (the labels dialog's
│                                     # screen view and OK, GH-139)
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
│   │                    # GH-126's onGiveUp/onMordiesSolution/onReplay*, GH-139's onLabels*, GH-163's onEscape) ->
│   │                    # GameViewModel, a WebState to the WebRenderer after every change; drains
│   │                    # uiRequests; tracks the view-layer analytics (screen views, dialog_cancelled);
│   │                    # sets each frame's announcement (GH-166)
│   ├── WebDialogFlows.kt # WebGame's Save, Load, size and can't-be-solved dialogs over its modal (GH-166)
│   ├── Announcements.kt # GH-166: what a screen reader hears - shifts, the win, new game, load,
│   │                    # solution steps - and the arrows' accessible names; Read board's
│   │                    # row-by-row words, letters in the phonetic alphabet (GH-167)
│   ├── WebState.kt      # One frame: tiles (1-based), Moves, Mordie's spot, solved, status, WebDialog,
│   │                    # the dragged line's live offset; the Give Up row, the solution screen and
│   │                    # the replay shift to slide (GH-126); the tile labels and tileTexts (GH-139);
│   │                    # LabelOption.available and Labels.message (GH-160); the one-time focus (GH-163)
│   │                    # and announcement (GH-166); which tiles are home and Read board's words (GH-167)
│   ├── WebRenderer.kt   # fun interface render(WebState) - the DOM on the page, a recorder in tests
│   ├── WebRequestRouter.kt # Answers each UiRequest (exhaustive `when`, like UiRequestRouter)
│   ├── WebLabelsDialog.kt # The Tile labels dialog's flows over WebGame's modal (GH-160), like
│   │                    # the terminal's LabelsDialogController
│   ├── WebModal.kt      # WebGame's dialog machinery: open/answer a WebDialog, the button that
│   │                    # opened it, the cancel's `via` (GH-163)
│   ├── DialogFocus.kt   # GH-163: Focus (a data-focus key), each dialog's default focus and title
│   ├── PageFocus.kt     # GH-164: fallbackFocus - where focus goes when its control is gone or disabled
│   ├── Layout.kt        # layoutOf(width, height): WIDE (wider than tall) or TALL
│   ├── Slides.kt        # The eased slide after an arrow shift, a drag release or a replay shift
│   │                    # (GH-126), drawn by WrapPositions
│   ├── MessageFeed.kt   # Chat-like timing for Mordie's lines: the old one rises out, then the new rises in
│   ├── Launch.kt        # startWebGame: the page's wiring (PlatformAnalyticsService "web" wrapped
│   │                    # by InputMethodAnalyticsService "rich_ui"), app_launched {mode, layout,
│   │                    # app_version, host} (GH-111) and tile_labels (GH-139)
│   ├── TextScale.kt     # GH-168: textScaleOf(root, appleBody): the system text size as a scale, never below 1
│   ├── Host.kt          # hostOf(hostname): PUBLIC_SITE, LOCALHOST or OTHER - only the bucket is sent (GH-111)
│   ├── Footer.kt        # footer(version): the footer line as Text/Link parts, worded as the README's Credits (GH-111)
│   ├── drag/            # LineDrag: one drag in pixels - axis lock past a threshold, live offset,
│   │                    # snap to whole tiles mod the side; WrapPositions: a part-shifted line's tiles
│   └── storage/         # BrowserSaveRepository: one KeyValueStore entry per save under
│                        # `mordovorot.save.<name>`, in SaveFileCodec's format, SaveNames' rules;
│                        # BrowserPreferenceRepository: `mordovorot.pref.<key>` (GH-139)
├── src/jsMain/kotlin/web/Main.kt       # The page's composition root: BoardImpl, saves and
│                                       # preferences in localStorage, Mordie with the background search,
│                                       # MainScope, the window's Layout, the beforeunload prompt;
│                                       # the footer, once, outside #app (GH-111); stopPinchZoom;
│                                       # applySystemTextSize first (GH-168)
├── src/jsMain/kotlin/web/DomGestures.kt # stopPinchZoom: on touch screens, cancels WebKit's pinch
│                                       # events, which iOS fires whatever touch-action says (GH-149)
├── src/jsMain/kotlin/web/DomRenderer.kt # WebState -> DOM, clicks and pointer drags -> WebGame
│                                       # intents; the page root holds a drag's pointer capture;
│                                       # runs Slides and MessageFeed on animation frames, redrawing
│                                       # only the tiles and Mordie's line; keeps the step list's
│                                       # scroll across frames, the current step in view (GH-126);
│                                       # the board sits in `.stage`, the box the tall layout's
│                                       # tiles are sized from (GH-147)
├── src/jsMain/kotlin/web/DomBoardGrid.kt # GH-167: during play, the tiles as a table for screen
│                                       # readers - a row element per board row, each cell heard as
│                                       # said (`delta, in place`), a wrapped copy aria-hidden - and the hidden Read board
│                                       # button, which says its words through DomAnnouncer
├── src/jsMain/kotlin/web/DomTextSize.kt # GH-168: at load, reads the browser's font size and (iPhone/iPad)
│                                       # Dynamic Type from a removed `-apple-system-body` probe; sets
│                                       # `--text-scale` and the root font size
├── src/jsMain/kotlin/web/DomFooter.kt  # footerElement: <footer>, its links in a new tab (GH-111);
│                                       # wide-only parts in `.wide-only` (GH-147)
├── src/jsMain/kotlin/web/DomDialogs.kt  # The modal dialogs; DomElements.kt: element/text/button helpers
├── src/jsMain/kotlin/web/DomKeyboard.kt # GH-163: Escape -> WebGame.onEscape, Tab wraps inside the
│                                       # open dialog, the page behind it `inert`, applies WebState.focus;
                                       # keeps the focused control across redraws, else fallbackFocus,
                                       # and drops held Enter repeats (GH-164)
├── src/jsMain/kotlin/web/DomSolution.kt # The step list, Prev/Next/Restart and the Give Up row (GH-126)
├── src/jsMain/kotlin/web/DomAnnouncer.kt # GH-166: says WebState.announcement through #announcer,
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
├── src/commonTest/kotlin/web/LayoutTest.kt, SlidesTest.kt, MessageFeedTest.kt, AnnouncementsTest.kt
├── src/commonTest/kotlin/web/TextScaleTest.kt # GH-168: textScaleOf's ratios, floor and bad readings
├── src/commonTest/kotlin/web/drag/     # LineDragTest, WrapPositionsTest
├── src/commonTest/kotlin/web/storage/  # BrowserSaveRepositoryTest, BrowserPreferenceRepositoryTest
│                                       # over InMemoryKeyValueStore
├── src/jvmTest/kotlin/web/IndexPageTest.kt # Pins the title rule (the title appears only in the tab)
│                                       # and relative asset URLs (GH-111: the public site's sub-path);
│                                       # every sheet linked, in order (GH-153); #announcer (GH-166)
├── src/jvmTest/kotlin/web/AnnouncerStylesTest.kt # GH-166: .visually-hidden clips, never hides from
│                                       # screen readers (no display: none or visibility: hidden)
├── src/jvmTest/kotlin/web/BuildInfoTest.kt, CreditsTest.kt # GH-111: the stamped version is the
│                                       # build's; the footer's credit matches the README's
├── src/jvmTest/kotlin/web/PhoneFitStylesTest.kt # GH-147, GH-148: pins the tall and wide fit rules
│                                       # (styles.css, fit.css, solution.css)
├── src/jvmTest/kotlin/web/TextSizeStylesTest.kt # GH-168: text in rem, tile labels' cap; with larger text, rows wrap and the
│                                       # columns beside the board leave it 16px tiles
├── src/jvmTest/kotlin/web/ColourContrastStylesTest.kt # GH-165: palette.css's text pairs, non-text
│                                       # edges and every tile label of every board meet WCAG AA
│                                       # in both themes; pins board.css's tile formula (Wcag.kt)
└── build/generated/buildInfo/          # BuildInfo.VERSION, written by :web:generateBuildInfo from
                                        # the root build.gradle.kts's version (GH-111)

src/main/kotlin/                # the terminal app (the root project)
├── Main.kt              # Entry point — resolves the starting InputMode (or aborts with no
│                          # terminal, GH-79), builds the session-lifetime objects (BoardImpl,
│                          # FileSaveRepository, Mordie) and runs buildGameSession (GH-3, GH-30)
├── AppWiring.kt         # The composition root (GH-78): buildGameSession / defaultView /
│                          # buildViewModel / buildTuiView - the only production code that
│                          # constructs collaborators (MordieHooks, ScreenRenderer,
│                          # ModeSwitcherImpl, TuiInputs). Test builders call the same functions
├── InputMode.kt         # MOUSE / KEYBOARD — parses the starting mode from `--keyboard`/
│                          # `--mouse`; also the type GameSession/ModeSwitcher pass around
│                          # mid-session (GH-3, GH-18, renamed from LaunchMode for GH-30)
├── GameSession.kt       # Session loop extracted out of Main: builds a fresh View per mode
│                          # through its buildView(mode, startupRestoreDone) - wired by
│                          # buildGameSession so every rebuild shares one board, saves and
│                          # CurrentSaveName (GH-99), KeyboardSelection and GameShiftSources
│                          # (GH-101), UnsavedProgress (GH-110) -
│                          # runs play(), and re-enters on ModeSwitchRequestedException. A
│                          # rebuild's play() throwing anything else falls back to the previous
│                          # mode (GH-79) instead of crashing and losing the in-progress game -
│                          # a first-session failure, or a failed fallback, still throws (GH-30).
│                          # Also holds the one `Mordie` (GH-83) and stops its search in
│                          # run()'s `finally`
├── viewmodel/           # the terminal-only part; the shared rest is in shared/viewmodel
│   ├── ViewModel.kt          # The terminal's viewmodel: GameViewModel + exitGame() (GH-41)
│   ├── ViewModelImpl.kt      # Delegates to GameViewModelImpl, adds ExitGameUseCase; built
│   │                           # only by buildViewModel (GH-41)
│   ├── SessionControlException.kt # Sealed marker base for control-flow exceptions that unwind
│   │                                 # play() intentionally — lets a catch-all rethrow via this
│   │                                 # one type instead of naming each subtype (GH-30)
│   ├── CatchingNonControl.kt # catchingNonControl — GameSession's catch-all: rethrows
│   │                           # SessionControlException/CancellationException, routes any
│   │                           # other Exception to onError (GH-65)
│   ├── ExitRequestedException.kt # Signals play() (or view.tui.TuiView's own loop, GH-3) to stop
│   ├── ModeSwitcher.kt        # ModeSwitcher / ModeSwitcherImpl — tracks
│   │                             # input_mode_switched and throws ModeSwitchRequestedException;
│   │                             # same-mode no-op (GH-30)
│   ├── ModeSwitchRequestedException.kt # Carries the requested InputMode; unwinds a running
│   │                                      # session's play() back to GameSession's loop (GH-30)
│   └── usecase/
│       └── ExitGameUseCase.kt # The toolbar Exit flow: Save before quitting?, then
│                                # ExitRequestedException (GH-12); delegated to by ViewModelImpl (GH-41)
├── storage/                       # the contract, the format and the name rules are in
│   │                                # shared/storage
│   ├── FileSaveRepository.kt      # Plain-text file implementation (the only class
│   │                                touching the filesystem); atomic writes via a temp file
│   └── FilePreferenceRepository.kt # `key=value` lines in `saves/preferences`, kept apart
│                                    # from the `.save` files; unknown lines kept (GH-139)
└── view/
    ├── View.kt          # View interface — play() (GH-79 removed the console-only members)
    └── tui/             # GH-3: the View implementation, the mouse/keyboard TUI
        ├── Terminal.kt / AnsiTerminal.kt   # Raw-mode terminal I/O (stty via ProcessBuilder,
        │                                     # xterm mouse-reporting escapes) - the one seam
        │                                     # touching the real terminal
        ├── TerminalEvent.kt / TerminalInputParser.kt # Byte-stream -> event decoding (SGR-1006
        │                                               # and legacy X10 mouse reports, keys,
        │                                               # arrow/Tab/BackTab/function-key
        │                                               # decoding for keyboard mode, incl.
        │                                               # F7 for the mode-switch shortcut)
        │                                               # (GH-18, GH-30), wheel notches (GH-126)
        ├── BoardLayout.kt / HitTarget.kt   # Pure board geometry + click hit-testing, incl. the
        │                                     # toolbar's mode-switch buttons
        │                                     # (HitTarget.ToolbarMode) (GH-30), the reserved
        │                                     # Mordie rows under `Moves:` (GH-83), and the Give Up
        │                                     # row, the step list and Prev/Next/Restart (GH-126)
        ├── StepListWindow.kt # Which steps of the solution screen's list show: follows the
        │                      # current step, scrolled by the wheel or W/S (GH-126)
        ├── WordWrap.kt      # Shared word wrap for dialogs and the Mordie line (GH-83)
        ├── LineSelection.kt # Pure keyboard line selection (GH-101, replaced GH-18's ArrowRing):
        │                      # LineSelection(row, col, axis) - WASD select with wraparound,
        │                      # HJKL/arrows shift the active line or switch axis, clampedTo(side).
        │                      # Plus KeyboardSelection, its session-lifetime holder (made by
        │                      # buildGameSession, so it survives mode switches)
        ├── Dialog.kt / DialogLayout.kt     # One model + one geometry class for all four
        │                                     # modal dialogs (Save/Load/Exit/Size (GH-44, a
        │                                     # buttons-only kind, so DialogLayout/DialogFocus
        │                                     # need no per-kind branch for it)) and the
        │                                     # Load-shaped startup restore prompt (GH-3 WU4);
        │                                     # Dialog also carries keyboard focus state
        │                                     # (focusedButtonId, textFieldFocused) (GH-18)
        │                                     # and disabledRows: dimmed, unclickable and
        │                                     # out of the focus ring (GH-160)
        ├── DialogFocus.kt   # Pure focus ring over a dialog's controls (text field/list rows,
        │                      # then buttons) for Tab/arrow-key dialog navigation (GH-18)
        ├── ScreenState.kt / ScreenRenderer.kt # Pure ScreenState -> frame String rendering,
        │                                        # including the dialog overlay, keyboard mode's
        │                                        # selected-line highlight and side hints
        │                                        # (GH-101, arrows hidden), the controls hint, and
        │                                        # toolbar shortcut labels (GH-18)
        ├── TuiInput.kt      # Per-mode input strategy interface (prepare/onBoardEvent/
        │                      # onDialogEvent/decorateBoard/decorateDialog/onDialogOpened) +
        │                      # InputAction sum type, so TuiView is mode-agnostic. onDialogOpened
        │                      # is called at the start of every modal loop so a stateful TuiInput
        │                      # can reset per-session state at an explicit boundary, even when
        │                      # one dialog opens another directly with no board repaint in
        │                      # between (e.g. Exit -> Save) (GH-18)
        ├── MouseInput.kt    # TuiInput impl: today's click-driven behaviour, stateless. Used
        │                      # by both the default mouse mode and the explicit --mouse flag
        │                      # (GH-18)
        ├── KeyboardInput.kt # TuiInput impl for --keyboard mode (GH-18): the WASD/HJKL key map
        │                      # over the shared KeyboardSelection (GH-101) and DialogFocus state
        ├── dialog/          # One controller per dialog flow (GH-78), each over TuiScreen's
        │                      # runModal: SaveDialogController (toolbar Save + the exit flow's
        │                      # name prompt), ExitDialogController (owns the toolbar answer the
        │                      # viewModel's ConfirmSaveBeforeExit then consumes),
        │                      # LoadDialogController (toolbar Load + the Load-shaped dialog),
        │                      # RestoreDialogController (startup restore prompt over it),
        │                      # SizeDialogController (toolbar New/F9 + ChooseBoardSize),
        │                      # UnsolvableDialogController (GH-74's repair/new game/exit),
        │                      # GiveUpDialogController (Give Up/F8's confirmation, GH-126),
        │                      # LabelsDialogController (Labels/F4's Tile labels dialog, GH-139)
        ├── UiRequestRouter.kt # Answers each UiRequest with the dialog that owns it - an
        │                      # exhaustive `when`, so a new request needs an answer to compile
        ├── ToolbarRouter.kt # Runs the activated toolbar button: Save/Load/Exit/New, Labels
        │                      # (GH-139) and the mode switch (GH-78)
        ├── TuiScreen.kt     # What the TUI draws (GH-78): the last-drawn BoardLayout/DialogLayout,
        │                      # the parked status message, wasSolved, repaint, and runModal -
        │                      # the one blocking dialog loop (inline, calls
        │                      # TuiInput.onDialogOpened first)
        └── TuiView.kt   # Owns its own event loop: runs startup restore, the board loop and
                          # shifts, and hands toolbar targets to ToolbarRouter and UiRequests to
                          # UiRequestRouter; every dialog shares TuiScreen's blocking modal loop
                          # over Terminal (WU4, GH-64). State-driven solved screen (WU5): every
                          # repaint asks ViewModel.isSolved() fresh rather than tracking a phase
                          # flag, so the Congratulations title/dimmed arrows and a load back to an
                          # unsolved board both fall out of the same repaint path with no extra
                          # branching. Delegates input handling to an injected TuiInput
                          # (MouseInput or KeyboardInput, picked per mode in defaultView) so
                          # mouse and keyboard modes share one event loop (GH-18).

src/test/kotlin/                # the terminal app's tests; board_model / analytics / mordie /
│                            # most viewmodel tests live in their shared/ module's
│                            # src/commonTest/kotlin (GH-41, GH-103)
├── InputModeTest.kt       # InputMode resolution + app_launched tracking (GH-3, renamed
│                            # from LaunchModeTest for GH-30)
├── ConsoleRemovalScenarioTest.kt # GH-79 end to end: no-terminal abort, --console notice, no
│                            # console switch in the TUI, failed-switch fallback, old saves
├── MoveCounterScenarioTest.kt / UnsolvableSaveScenarioTest.kt # GH-62/GH-74 end to end
│                            # through the TUI over a real board and temp-dir saves
├── MordieScenarioTest.kt  # GH-83 end to end with a scripted FakeMordieSearchFactory
├── GiveUpScenarioTest.kt  # GH-126 end to end: Give Up, the solution screen and its replay
├── TileLabelsScenarioTest.kt # GH-139 end to end: the Tile labels dialog, the preference and
│                            # saves' labels, in mouse and keyboard mode
├── GameSessionTest.kt     # Session loop: startupRestoreDone seeding, multi-switch chains,
│                            # failed-switch fallback, Mordie stopped on exit (GH-30)
├── AppWiringTest.kt      # Composition root: each mode's switch key really switches, a mode
│                            # switch rebuilds around the session's one board and Mordie, which
│                            # analytics each collaborator gets (GH-78)
├── ModeSwitcherTest.kt    # input_mode_switched tracking, trigger pass-through, same-mode
│                            # no-op (GH-30)
├── viewmodel/            # One test file per viewmodel concern (GH-42 WU3), driving ViewModelImpl
│                           # directly, no play() loop involved. The ones here exercise Exit or
│                           # build SaveFileFormatException (ViewModelExitTest/
│                           # OrderingTest/UnsolvableLoadTest/SaveLoadTest/CurrentSaveNameTest/
│                           # QueriesTest/StartupRestoreTest) and still cover the shared code
│                           # through ViewModelImpl's delegation; CatchingNonControlTest pins
│                           # GameSession's catch-all (GH-65). The rest (Delegation, Cancellation,
│                           # Mordie, MoveCount, NewGame, ShiftSources, ShiftSteps, SizePrompt,
│                           # UnsavedProgress, UnsolvableExitHook, GiveUp, SolutionSaveLoad,
│                           # CatchingNonCancellation, MordieText, SaveInfo, and GH-126's
│                           # SolutionReplay/SolutionSteps) are in
│                           # shared/viewmodel's commonTest, built by its own testViewModel (GH-41)
├── storage/               # FileSaveRepository and FilePreferenceRepository coverage
├── view/tui/              # TUI coverage (GH-3, WU2-WU5)
└── testing/              # testViewModel / testTuiView / testGameSession - the only way tests
                            # build these, calling AppWiring with fake defaults (GH-78) /
                            # NoopModeSwitcher / FakeTerminal /
                            # FakeViewModel (recording double, collapsing GH-23's
                            # RecordingViewModel/FakeConsoleViewModel/FakeTuiViewModel split in
                            # GH-42 WU3) / RecordingModeSwitcher (GH-30). The shared doubles
                            # (FakeBoardModel, FakeSaveRepository, FakeViewModelUi, ...) are in
                            # shared/testing
```

The terminal app uses Gradle's standard source sets (`src/main/kotlin`, `src/test/kotlin`);
each `shared/` module uses KMP's (`src/commonMain/kotlin`, `src/commonTest/kotlin`) — see
"Dependencies" below.

## Layers

Classic MVVM. `board_model` and `view` are each an interface + one implementation
(`view`'s is the TUI, `view.tui.TuiView`).
`viewmodel` (GH-42) is the shared `GameViewModel` interface + one `GameViewModelImpl`
(`:shared:viewmodel`, GH-41),
holding no reference to any `View` — it communicates with whichever `View` is driving it
through a request channel instead of a constructor-injected dependency. The terminal's
`ViewModel` adds Exit on top (GH-41), implemented by `ViewModelImpl`, which delegates the
rest to `GameViewModelImpl`. Callers should always depend on an interface, not an impl,
to keep layers swappable.

### board_model (Domain)
Core game state and rules: board array, shifting, reset, and the win check
(`isCorrect`). No dependency on `viewmodel` or `view`.

**Board size (GH-44):** the board stays square, with a side of 3–6 (default 4; 6 since GH-160).
`BoardSize` holds that range and its validation — the single place it lives, per
this layer's "range validation lives in `board_model`, the view translates but
does not validate" rule (see the 1-based display dialect note under `view`).
`BoardImpl.newGame(side)` is the only operation that ever changes the side: it
validates first, so an out-of-range value throws with the current board left
untouched, then reallocates and shuffles. `resetGame()` keeps its older meaning —
reshuffle at the same size. Every shuffle is solvable (GH-74): `Solvability`
holds the parity rule, and `resetGame()` swaps tiles 0/1 when the shuffle lands
on an unsolvable board. Nothing resizes an *in-progress* board: every
size-setting surface starts a fresh game instead. The board instance itself is
still the one every rebuild shares across mode switches (GH-30) — the side mutates
in place rather than the instance being swapped, so that invariant is unaffected.

**Move count (GH-62):** `BoardImpl` owns the move rule — a move is a run of
consecutive shifts on one row or column. Each shift checks bounds first, then
counts a move only if its line differs from the last shift's. `resetGame`/`newGame`
zero the count; `restoreState(state, moveCount)` sets it and clears the last line,
so the first shift after a load always starts a new move. Because the count lives
on the board, it survives mode switches with no extra wiring.

### mordie (GH-83: the computer opponent)
Pure domain: plain `IntArray` arrangements, no dependency on `board_model`,
`storage`, `viewmodel` or `view` (`viewmodel` → `mordie`). `IdaStarSolver` searches the
game's original arrangement for its shortest solution in the GH-62 move metric, with
`MoveMetricHeuristic` as the admissible bound. It runs in slices, so
`BackgroundMordieSearch` can throttle it (about half of one core; the constants sit in
one place for the GH-41 mobile port), and memory stays O(depth). A search started from a
saved N begins at N + 1, so it never re-reports below it. `Mordie` turns the player's
shifts into the line state: a check every 5 shifts, a reveal of "found", and the outcome
frozen on solve, using Mordie's live state at that shift. Its `MordieSnapshot` mirrors
`storage.SavedMordie` on purpose, so `mordie` never imports `storage`; `MordieHooks` maps
between the two. `Main` builds one `Mordie` per session, so it survives mode switches;
`GameSession` stops it. `GameViewModelImpl` reaches it through `MordieHooks`, which
`buildGameViewModel` builds per rebuild so it tracks through that mode's decorated analytics.
Boards above `Mordie.MAX_SIDE` (6x6, GH-160) get `Mordie.sitOut()`: no search and no
snapshot, only the opening `SitsOut` line; `MordieHooks` decides by board side before every
binding. Lifting it is GH-116's follow-up.
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
default: `buildGameViewModel` wires them. The terminal's `ViewModel` adds `exitGame()`
(GH-41): `buildViewModel` builds `ExitGameUseCase` on the same `UiRequestChannel` and wraps
the `GameViewModelImpl` in `ViewModelImpl`; tests build it through `testViewModel`'s fakes.

Five interactions that used to be blocking calls into `View` (`showMessage`,
`confirmRestore`, `chooseSaveToRestore`, `confirmSaveBeforeExit`, `promptSaveName`)
are now `viewmodel.UiRequest<R>` values sent on `ViewModel.uiRequests`, a
`Channel.RENDEZVOUS` the owning `View` drains in a sibling coroutine and answers via
`UiRequest.respond`. `TuiView` hands each to `UiRequestRouter`, which routes it to the
dialog controller that answers it (`view.tui.dialog`, GH-78); `ShowMessage` parks the text on
`TuiScreen`. Rendezvous delivery
(the channel has zero buffer, and `UiRequestChannel.ask()` doesn't return until the View has actually
handled the request) is what keeps message/prompt ordering identical to the old
blocking-call behavior. The one invariant that keeps this deadlock-free: a View's
request handler must never call a request-raising `ViewModel` method
(`saveGame`/`loadGame`/`exitGame`/`restoreOnStartup`) from inside
itself — only the plain query methods are safe there. All five are `suspend`; a cancelled coroutine resumes them with `CancellationException`, which
each use case's catch-all (`catchingNonCancellation`) rethrows rather than swallows (ordinary
structured-concurrency hygiene). No session-control exception passes through a use case
(GH-41), so that catch-all doesn't need to know about one.

**Mordie wiring (GH-83).** `ViewModel.mordieLine()` (the text after `Mordie:`, or `null`)
and `mordieEnding()` (the solved title, or `null` → `Congratulations ✓`) are plain
queries. `shift()` calls `mordie.onShift()` and then, on solve, `mordie.onSolved()`;
`newGame` starts Mordie fresh; `saveGame` passes `mordie.snapshot()`. A
successful load binds Mordie through one rule: a solved save with Mordie data gets that
outcome, a solved save without it clears Mordie, a found save resumes found, a searching
save with a solvable original resumes at its N, and anything else (and a Repair) starts
fresh from the loaded board if it has no moves yet, or clears Mordie if it has. All of this
lives in `MordieHooks`. In the TUI, `ScreenState` carries `mordieLine` and the solved
title (`mordieEnding() ?: TITLE_SOLVED`); `BoardLayout` reserves `mordieRows` under
`Moves:` from the longest possible line at `mordieWrapWidth`, so the grid stays put. Every `restoreOnStartup` path ends with
`mordie.ensureStarted`, which does nothing once Mordie is bound (e.g. after a mode switch).

**Give Up and the solution screen (GH-126).** Mordie's search keeps its solution's moves
(`SearchProgress.solution`), `Mordie` holds them, and a found save stores them. `SolutionUseCase`
owns the rest: `giveUpButton()` from Mordie's state and the board, entering the screen
(`giveUp()`, `showSolution()` after a win, or a load of a solution-screen save), and the
replay. On the screen the board holds the original arrangement with 0 moves, which is exactly
what a save there writes; `SolutionReplay` steps its own copy, so a replay never counts moves,
feeds Mordie, fires `board_solved` or sets unsaved progress. While it is open,
`boardState()` is the replay's, `shift()` does nothing and `mordieLine()` says how many moves
Mordie made. `replayNext()`/`replayPrevious()` return the one shift they made, so a view can
animate it. `SolutionScreen` is one per session, like `CurrentSaveName`, so a terminal mode
switch keeps the screen and the replay's place. The give-up confirmation is the view's, like
`[ New ]`'s size picker, not a `UiRequest`: `GiveUpDialogController` and `WebGame` ask, then
call the synchronous `giveUp()`. `screen_solution` is tracked here rather than in the views,
since all four ways onto the screen are shared flows.

### view
UI only: `view.tui.TuiView` drives the mouse/keyboard TUI, renders the board and
calls into `ViewModel`. Should not manipulate `board_model` directly. The line-based
console mode (`ViewImpl` and its command table) was removed in GH-79.

**1-based display dialect (GH-10):** displayed tile values are 1-based by default;
`board_model` and `storage` stay 0-based. `viewmodel.TileLabels` is the single
translation point: `NUMBERS` adds 1, and the other schemes (GH-139) label the 0-based
tile their own way. `ScreenRenderer` draws `ScreenState.tileLabels`, which `TuiScreen`
reads from `ViewModel.tileLabels()` on every repaint. Clicks and key
presses resolve to 0-based `HitTarget`s directly, so nothing parses a typed index.
Range validation lives in `board_model` (`BoardImpl`'s bounds check), not `view`.

`BoardImpl.restoreState` and `SaveFileCodec`'s validation errors avoid
stating an explicit numeric range (e.g. "contain each of N tile values
exactly once" rather than "permutation of 0..N-1") so they read correctly
next to the 1-based board when shown in a Load dialog message.

### storage
File persistence for save games and preferences. `SaveRepository` is the contract `viewmodel`
depends on; `FileSaveRepository` is the only class in the codebase that touches
the save files, and `FilePreferenceRepository` (GH-139) the preferences file beside them.
`PreferenceRepository` is a plain string store, so `storage` still knows no viewmodel types;
`viewmodel.TileLabelsPreference` gives it a type. The text format (`SaveFileCodec`) and the accepted names (`SaveNames`)
live in `:shared:storage` so every platform reads and writes the same saves (GH-110). No dependency on `board_model`, `viewmodel`, or `view` — it
deals in plain data (`SavedBoard`), not domain objects.

Save file format: line 1 is the square side, line 2 the space-separated tile
values, line 3 the move count (GH-62). A missing or blank line 3 (a save from
before GH-62) loads as 0; a non-integer or negative one is a
`SaveFileFormatException`. GH-83 adds two optional lines: line 4 is the original
arrangement and line 5 is `<N> found` or `<N> searching`. A missing or blank line 4 (an
older save, or a GH-74 Repair write) means no Mordie data. Once line 4 is present, a bad
line 4 or 5 is a `SaveFileFormatException`. GH-126 adds two more: line 6 is Mordie's solution on
a found save, space-separated `r<i>:<k>`/`c<i>:<k>` (rotate row i right, or column i down, by k),
and line 7 is `solution` on a save made on the solution screen. A missing or blank line means
neither; a solution that doesn't solve the original in N moves is a `SaveFileFormatException`.
GH-139 adds line 8, the tile labels' wire name, which every new save writes; lines 4-7 are blank
where they don't apply, which older builds already read as "nothing there". A missing or blank
line 8 means no labels, and any other word is kept, so a name this build doesn't know loads with
the preference rather than as a corrupted save.

### analytics
Cross-cutting: `AnalyticsService` is injected into `viewmodel` (and any layer
that needs to track an event), currently backed by `NoopAnalyticsService`.
`InputMethodAnalyticsService` (GH-3) decorates another `AnalyticsService`,
adding an `input_method` (`mouse`/`keyboard`, GH-18) property to
every forwarded event — the decorator pattern lets `Main` distinguish events
by launch mode without `GameViewModelImpl` itself knowing which UI mode is
running. `PlatformAnalyticsService` (GH-110) adds `platform` the same way; it is the
innermost decorator, wrapping the outermost service in `Main` (`terminalAnalytics`), so
`app_launched`, `app_launch_aborted` and `input_mode_switched` carry it too.

`board_solved` (GH-62) is tracked in `GameViewModelImpl`, not a View: every shift goes
through one private `shift` helper that fires it when the shift turns an unsolved
board into a solved one, so it covers both modes. Loads bypass that helper,
so restoring a solved save never fires it. GH-83 adds `mordie_result`/`mordie_moves` to it
and tracks `mordie_solution_found` from the same helper, when a 5-shift check reveals
the solution. `shiftRow`/`shiftColumn` (GH-110, a web drag) run the same helper over several
steps: each is a board shift Mordie counts, and solved is checked once at the end, so a drag
passing through the solved board doesn't fire it. `shift_keys`/`shift_input` follow the solving
shift's `ShiftSource`.

### Composition root (GH-78)
`AppWiring.kt` is the only production code that constructs collaborators. `Main` builds
the session-lifetime objects (`BoardImpl`, `FileSaveRepository`, `NoopAnalyticsService` with
`platform: terminal`,
`Mordie(BackgroundMordieSearch.Factory())`) and hands them to `buildGameSession`, whose
`buildView` closure calls `defaultView` per mode: it decorates analytics, then builds the
viewmodel (`buildViewModel`, including `MordieHooks`) and the TUI (`buildTuiView`, including
`ScreenRenderer`, the mode's `TuiInput` and a `ModeSwitcherImpl`). Production classes take
every collaborator as a constructor parameter with no real-implementation default; value
defaults (a board side, a save directory, a flag) are fine. Tests build through
`testing/`'s `testViewModel`/`testTuiView`/`testGameSession`, which call the same
functions with fakes.

The browser's composition root is `web`'s `Main.kt` (GH-110): it builds the page's
collaborators and hands them to `startWebGame` (`Launch.kt`), which decorates analytics
(`platform: web`, then `input_method: rich_ui` for everything but `app_launched`), calls
`buildGameViewModel` with no startup shortcuts and no Exit hook, and starts a `WebGame`.
`Main.kt` also passes the stamped `BuildInfo.VERSION` and `hostOf(window.location.hostname)` for
`app_launched` (GH-111), and appends the footer to `<body>` once, outside `#app`, which
`DomRenderer` rebuilds every frame.
The web scenario tests go through the same `startWebGame` with fakes.

### Board-size surfaces (GH-44)
Three entry points, all of which *start a fresh game* rather than resizing the
running one: the `--size=N` launch flag (`Main.resolveBoardSize` returns `Int?`
- `null` for a missing, non-numeric, or out-of-range value, distinct from "asked
for exactly the default" - warning and falling back to 4 on `null`; reported as
`app_launched.board_size`); a startup prompt raised by
`StartupRestoreUseCase.restoreOnStartup` as `UiRequest.ChooseBoardSize`, answered by
the TUI's `Dialog.Kind.SIZE` picker; and the TUI's `[ New ]`
toolbar button (`HitTarget.ToolbarNew`, F9 in keyboard mode), which opens the
same picker. All of them funnel into `ViewModel.newGame(size, trigger)` →
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
`ViewModel.listSaves()` returns `List<SaveInfo>` (`name` + nullable
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
startup so `restoreOnStartup`'s own size prompt runs. EXIT only reports
`LoadResult.EXIT_CHOSEN` (GH-41); once the load has returned, `GameViewModelImpl` hands it to
its `UnsolvableSaveExit` hook. The terminal's hook (built in `buildViewModel`) runs
`exitGame()` mid-game, or throws `ExitRequestedException` at startup, which
`TuiView.play` catches around `restoreOnStartup`.
`ChooseBoardSize.origin` is what the TUI reports as
`screen_size_dialog.opened_from`. The TUI answers with a non-cancellable
`Dialog.Kind.UNSOLVABLE` (`runModal(cancellable = false)`), and
`ExitDialogController.confirmSaveBeforeExit` opens a Yes/No dialog itself when no toolbar
Exit answer is pending.

**`--size=N` bypasses the *entire* startup sequence, not just the size prompt.**
A usable `--size=N` sets `sizeChosenAtLaunch` (captured by `buildGameSession`
and passed through `defaultView` → `StartupRestoreUseCase` on every rebuild), and `restoreOnStartup` returns immediately when
it's set — no restore prompt, no size prompt, no `startup_restore_prompt_shown`/
`startup_restore_decision`, regardless of what saves exist on disk. It does
refit the labels to the launch size, so a Letters preference shows Numbers on
`--size=6` (GH-160). Without the
flag (or with an unusable one - `sizeChosenAtLaunch` is `false` in that case
too, matching `resolveBoardSize`'s `null`), the restore prompt runs as before
(GH-6), and the size prompt raises only once it's established nothing was
actually restored (no saves, declined, blank/EOF, or a failed load). With 2+
saves, `restoreOnStartup` matches the answer against save names exactly; the TUI
always answers with a listed name, so anything else counts as declined (GH-79).

### Launch mode and runtime mode switching (GH-3, GH-18, GH-30, GH-79)
`Main.resolveInputMode` decides how a launch starts. With no interactive
terminal (`System.console() == null`, e.g. `./gradlew run` or piped input) it
prints `No interactive terminal found - Mordovorot only runs as a terminal UI.`
to stderr, tracks `app_launch_aborted` instead of `app_launched`, and returns
`null`, so `main` returns and the JVM exits 0. Otherwise `InputMode.resolve`
(root package, renamed from `LaunchMode` for GH-30) is pure flag parsing:
`--keyboard` selects the TUI built with `KeyboardInput`, anything else (no
flag, `--mouse`) the TUI built with `MouseInput`. `--console` selects nothing:
in a terminal it prints `Console mode was removed - starting the terminal UI.`
and sets `app_launched.console_flag_ignored`. The terminal check and both
message sinks are injectable parameters, so all of this is unit-tested without
a real terminal.

`InputMode` no longer only decides where a session *begins* — `GameSession`
lets it switch mid-session, driven by `ModeSwitcher`. `TuiView`'s toolbar
`[ Keyboard ]` button (mouse mode) and F7 (keyboard mode), reached via
`HitTarget.ToolbarMode`/`ScreenState.modeButtons`, go through `ToolbarRouter` to `ModeSwitcherImpl`,
which tracks `input_mode_switched` and throws `ModeSwitchRequestedException`.
`GameSession` catches the exception, rebuilds the `View`/viewmodel/analytics
stack for the new mode (through `buildGameSession`'s closure) around the *same*
`BoardImpl`/`FileSaveRepository` instances, and re-enters `play()` — so switching preserves board state and
never reshuffles. If that rebuild fails (e.g. no `stty`), `GameSession`
rebuilds the previous mode and hands it `Could not switch to <target> mode
(<reason>) - staying in <previous> mode.` via `View.play(notice)`, shown as the
first status line (stderr would be hidden behind the full-screen TUI); a failure on the first session, or of that
fallback, still throws. `app_launched` still fires exactly once, from the
initial resolution in `Main`; each subsequent switch fires
`input_mode_switched` instead (`docs/ANALYTICS_EVENTS.md`).

## Agent tooling (not part of the game)

`scripts/` holds the Claude Code workflow tooling, tested with Python `unittest` in `tests.yml`. Two
bash hooks are wired in the committed `.claude/settings.json` through `${CLAUDE_PROJECT_DIR}`:
`scripts/onboard/gate.sh` (SessionStart/UserPromptExpansion/PreToolUse, blocks work until `/onboard`
has run) and `scripts/alert/alert.sh` (GH-107: Notification/Stop, a macOS notification and spoken
alert when a session waits on the user, toggled per machine in the gitignored `.claude/alert.env`).
`scripts/web/harness/` (GH-147; `phone_fit/` until GH-156) measures the built web game at phone sizes in headless Chrome
(`measure.sh`, see its README; a `landscape` preset and checks on all four edges, GH-148) and serves it to a real phone on the local network (`serve_lan.sh`). `styles_diff.sh` (GH-156)
compares every element's computed styles against a master bundle, so a CSS refactor can show the
game looks the same; its compare logic, `styles_diff.py`, is tested in `tests.yml` too. Its
`report=keys` (GH-174, `keys.js`) plays keys and clicks and reports focus, accessible names, the
ring and `inert` after each; `keys_check.sh` diffs the GH-163 dialog flows against
`tests/keys_expected.txt`, run by the agent rather than CI (it needs Chrome and a bundle).
`--text-size=<percent>` (GH-168) sets the throwaway profile's font size for both scripts, and
`fit=scroll` checks a page allowed to scroll at larger text.

## Dependencies

- **Build tool:** Gradle (Kotlin DSL), via the wrapper (`./gradlew`) — pinned to 9.7.1.
  Versions shared across modules (Kotlin, kotlinx-coroutines) live in the
  `gradle/libs.versions.toml` version catalog (GH-109).
  The terminal app (root project) uses `kotlin("jvm")` + `application`; JVM toolchain 17.
- **Kotlin Multiplatform (GH-41).** The `shared/` modules use `kotlin("multiplatform")`, the
  same first-party JetBrains plugin and version as `kotlin("jvm")` (declared once in the root
  `plugins {}` with `apply false`), so nothing new reaches the runtime classpath. Each module
  has a `jvm()` target and a `js { nodejs() }` one: with only `jvm()`, `commonMain` would
  accept `java.lang.Math` and the like, so the JS target is the compiler check that shared
  code stays platform-independent. Since GH-103 it is a real target with a Node.js test task;
  KGP downloads Node.js and yarn, and `kotlin-js-store/yarn.lock` pins the npm packages.
  `:shared:testing` (the test doubles) is multiplatform too, so common tests can use them.
  No `expect`/`actual` — every platform seam is a plain interface. The app
  depends on the modules' JVM jars, which `installDist` bundles into `lib/`. Their tests are in
  `commonTest` and run on both the JVM and Node.js (GH-103); only mordie's
  `BackgroundMordieSearchThreadTest` stays in `jvmTest`, since it reads the search from a
  second thread. `./gradlew check` — not `./gradlew test`, which only runs the root
  project's — runs them all. Module graph:
  `core`, `analytics`, `storage` and `mordie` depend on no other module; `viewmodel` has an
  `api` dependency on all four (its public types expose them); the app depends on all five.
  `:web` (GH-110) depends on `viewmodel`, like the app. Its `browser()` target makes KGP add
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
  Known follow-up for the first non-JVM port: seven viewmodel test files stay in the app
  because they use Exit or `SaveFileFormatException`, so they don't run on other targets
  (`SaveFileFormatException` is in `:shared:storage` since GH-110).
- **Test framework:** `kotlin("test")` on the JUnit 5 platform (`useJUnitPlatform()`).
  No mocking library — hand-written fakes in `src/test/kotlin/testing/` and, for doubles of
  shared types, the `:shared:testing` module.
- **`kotlinx-coroutines-core` (GH-42) — the project's first third-party runtime
  dependency.** Powers `viewmodel`'s View-request channel (see above): `ViewModel`'s
  suspend functions, `Channel`, and `runBlocking` at `Main`'s entry point. Unlike
  JLine (below), this cleared the zero-third-party bar because it's the
  JetBrains-maintained concurrency primitive the eventual KMP UI layer needs anyway,
  not a terminal-handling convenience — and it's the multiplatform artifact, so the
  same coordinate resolves per-target once other platforms are added, with no
  declaration change. The app's tests run on plain `runBlocking`. The shared modules'
  common tests use `kotlinx-coroutines-test`'s `runTest` (GH-103), since `runBlocking` doesn't
  exist on JS; checkup CHK-2026-10-14-light-2 had dropped it as unused. `withTimeout` guards
  the tests where a deadlock would otherwise hang silently. Run on a single `runBlocking` event
  loop with no dispatcher — there's no
  real concurrency to exploit here (every I/O call is blocking, single-consumer), so
  a thread pool (`Dispatchers.Default`/`IO`) or `Dispatchers.Unconfined` would only
  add nondeterminism to the request/response ordering for no benefit.
  **One exception (GH-83):** `BackgroundMordieSearch` runs the Mordie search as one
  throttled coroutine on `Dispatchers.Default`. It never touches the UI request channel;
  the event loop only reads its volatile progress snapshot. That keeps the request/response
  ordering above deterministic.
- Beyond that, production code has no dependencies past the Kotlin standard library
  (board shuffling uses `kotlin.collections.shuffle()`, not a custom implementation).
- **JLine considered and rejected (GH-6):** save/load filename tab-autocompletion
  would require raw/cbreak terminal input, which only a library like JLine 3 provides.
  Rejected as too heavyweight for this project's zero-third-party-dependency stance.
  The TUI later made the question moot: its Load dialog lists the saves to pick from,
  so nothing needs a typed save name.
