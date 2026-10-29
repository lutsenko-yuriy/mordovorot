import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.FakePreferenceRepository
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.switchModeKeys
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.HitTarget
import view.tui.KeyboardSelection
import view.tui.TerminalEvent
import view.tui.TerminalSize
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.ModeSwitchRequestedException
import viewmodel.SessionTileLabels
import viewmodel.SolutionScreen
import viewmodel.TileLabelsPreference
import viewmodel.UnsavedProgress

/** Covers the composition root (GH-78): what production wiring builds and what every rebuild
 *  shares. */
class AppWiringTest {

    @Test
    fun `the production View is wired with a real ModeSwitcher, not a silent no-op`(): Unit = runBlocking {
        val terminalSize = TerminalSize(80, 40)
        for (mode in InputMode.entries) {
            val view = defaultView(
                mode,
                BoardImpl(4),
                FakeSaveRepository(),
                RecordingAnalyticsService(),
                startupRestoreDone = true,
                sizeChosenAtLaunch = false,
                mordie = Mordie(FakeMordieSearchFactory()),
                currentSaveName = CurrentSaveName(),
                gameShiftSources = GameShiftSources(),
                unsavedProgress = UnsavedProgress(),
                solutionScreen = SolutionScreen(),
                tileLabels = SessionTileLabels(TileLabelsPreference(FakePreferenceRepository())),
                keyboardSelection = KeyboardSelection(),
                terminalFactory = { FakeTerminal(switchModeKeys.toMutableList(), terminalSize) },
            )

            val switch = assertFailsWith<ModeSwitchRequestedException> { view.play(null) }
            assertEquals(InputMode.entries.single { it != mode }, switch.target)
        }
    }

    @Test
    fun `a mode switch rebuilds around the session's one board and Mordie`(): Unit = runBlocking {
        val terminalSize = TerminalSize(80, 40)
        val layout = BoardLayout(terminalSize, 4)
        val (leftX, leftY) = layout.leftArrowPosition(0)
        val terminal = FakeTerminal(
            (listOf(TerminalEvent.MouseClick(leftX, leftY)) + switchModeKeys).toMutableList(),
            terminalSize,
        )
        val scrambled = IntArray(16) { 15 - it }
        val board = BoardImpl(4).apply { restoreState(scrambled) }
        val factory = FakeMordieSearchFactory()
        val analytics = RecordingAnalyticsService()

        testGameSession(
            initialMode = InputMode.MOUSE,
            board = board,
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
            mordie = Mordie(factory),
        ).run()

        val shifted = BoardImpl(4).apply { restoreState(scrambled); shiftLeft(0) }
        assertEquals(shifted.boardArray.toList(), board.boardArray.toList(), "not reshuffled by the rebuild")
        val keyboardFrame = terminal.frames.last { it.contains("[Controls 6]") }
        assertTrue(keyboardFrame.contains("Moves: 1"), "the keyboard View reads the same board")
        assertEquals(1, factory.started.size, "the keyboard rebuild reuses Mordie's running search")
        val switched = analytics.events.single { it.name == "input_mode_switched" }
        assertFalse("input_method" in switched.properties, "the ModeSwitcher tracks undecorated")
    }

    @Test
    fun `the View tracks with the mode's input_method`(): Unit = runBlocking {
        val terminalSize = TerminalSize(80, 40)
        val layout = BoardLayout(terminalSize, 4)
        val newButton = layout.toolbarButtons().first { it.target == HitTarget.ToolbarNew }
        val terminal = FakeTerminal(mutableListOf(TerminalEvent.MouseClick(newButton.x, newButton.y)), terminalSize)
        val analytics = RecordingAnalyticsService()

        testGameSession(
            initialMode = InputMode.MOUSE,
            board = BoardImpl(4),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
        ).run()

        val opened = analytics.events.first { it.name == "screen_size_dialog" }
        assertEquals("mouse", opened.properties["input_method"])
    }

    @Test
    fun `terminal analytics tag every event with platform terminal, launch and mode switch included`(): Unit = runBlocking {
        val terminalSize = TerminalSize(80, 40)
        val layout = BoardLayout(terminalSize, 4)
        val newButton = layout.toolbarButtons().first { it.target == HitTarget.ToolbarNew }
        val terminal = FakeTerminal(
            (listOf(TerminalEvent.MouseClick(newButton.x, newButton.y), TerminalEvent.Escape) + switchModeKeys).toMutableList(),
            terminalSize,
        )
        val recording = RecordingAnalyticsService()
        val analytics = terminalAnalytics(recording)

        resolveInputMode(emptyArray(), analytics, hasInteractiveTerminal = { true })
        testGameSession(
            initialMode = InputMode.MOUSE,
            board = BoardImpl(4),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
        ).run()

        val names = recording.events.map { it.name }
        assertTrue(listOf("app_launched", "screen_size_dialog", "input_mode_switched").all { it in names }, "$names")
        assertTrue(recording.events.all { it.properties["platform"] == "terminal" }, "${recording.events}")
    }
}
