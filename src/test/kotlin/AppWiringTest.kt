import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.HitTarget
import view.tui.KeyboardSelection
import view.tui.TerminalEvent
import view.tui.TerminalSize
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.ModeSwitchRequestedException

/** Covers the composition root (GH-78): what production wiring builds and what every rebuild
 *  shares. */
class AppWiringTest {

    @Test
    fun `the production View is wired with a real ModeSwitcher, not a silent no-op`(): Unit = runBlocking {
        val terminalSize = TerminalSize(80, 40)
        val keyboardButton = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD))
            .toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.KEYBOARD) }
        val switchKey = mapOf(
            InputMode.MOUSE to TerminalEvent.MouseClick(keyboardButton.x, keyboardButton.y),
            InputMode.KEYBOARD to TerminalEvent.FunctionKey(7),
        )
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
                keyboardSelection = KeyboardSelection(),
                terminalFactory = { FakeTerminal(mutableListOf(switchKey.getValue(mode)), terminalSize) },
            )

            val switch = assertFailsWith<ModeSwitchRequestedException> { view.play(null) }
            assertEquals(InputMode.entries.single { it != mode }, switch.target)
        }
    }

    @Test
    fun `a mode switch rebuilds around the session's one board and Mordie`(): Unit = runBlocking {
        val terminalSize = TerminalSize(80, 40)
        val layout = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD))
        val (leftX, leftY) = layout.leftArrowPosition(0)
        val keyboardButton = layout.toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.KEYBOARD) }
        val terminal = FakeTerminal(
            mutableListOf(TerminalEvent.MouseClick(leftX, leftY), TerminalEvent.MouseClick(keyboardButton.x, keyboardButton.y)),
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
        val keyboardFrame = terminal.frames.last { it.contains("[Mouse F7]") }
        assertTrue(keyboardFrame.contains("Moves: 1"), "the keyboard View reads the same board")
        assertEquals(1, factory.started.size, "the keyboard rebuild reuses Mordie's running search")
        val switched = analytics.events.single { it.name == "input_mode_switched" }
        assertFalse("input_method" in switched.properties, "the ModeSwitcher tracks undecorated")
    }

    @Test
    fun `the View tracks with the mode's input_method`(): Unit = runBlocking {
        val terminalSize = TerminalSize(80, 40)
        val layout = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD))
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
}
