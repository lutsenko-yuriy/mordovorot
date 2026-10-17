import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import storage.FileSaveRepository
import testing.FakeMordieSearchFactory
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.HitTarget
import view.tui.TerminalEvent
import view.tui.TerminalSize
import viewmodel.CurrentSaveName

/** GH-79 scenarios: console mode removed - the no-terminal abort, the `--console` notice, the TUI
 *  without a console switch, the failed-switch fallback, and old saves still loading. */
class ConsoleRemovalScenarioTest {

    @Test
    fun `a launch with no terminal prints the abort message and starts no game`() {
        val cases = listOf(
            emptyArray<String>() to "none",
            arrayOf("--mouse") to "mouse",
            arrayOf("--keyboard") to "keyboard",
            arrayOf("--console") to "console",
        )
        for ((args, requested) in cases) {
            val analytics = RecordingAnalyticsService()
            val messages = mutableListOf<String>()

            val mode = resolveInputMode(
                args, analytics,
                reportNoInteractiveTerminal = { messages += it },
                hasInteractiveTerminal = { false },
            )

            assertNull(mode, "args=${args.toList()}")
            assertEquals(listOf("No interactive terminal found - Mordovorot only runs as a terminal UI."), messages)
            assertEquals(
                listOf(Event("app_launch_aborted", mapOf("reason" to "no_interactive_terminal", "requested_mode" to requested))),
                analytics.events,
                "args=${args.toList()}",
            )
        }
    }

    @Test
    fun `--keyboard outranks --console in requested_mode`() {
        val analytics = RecordingAnalyticsService()

        resolveInputMode(arrayOf("--console", "--keyboard"), analytics, reportNoInteractiveTerminal = {}, hasInteractiveTerminal = { false })

        assertEquals("keyboard", analytics.events.single().properties["requested_mode"])
    }

    @Test
    fun `--console in a terminal shows the removal notice and starts the mouse TUI`() {
        val analytics = RecordingAnalyticsService()
        val notices = mutableListOf<String>()
        val unrecognized = mutableListOf<String>()

        val mode = resolveInputMode(
            arrayOf("--console"), analytics,
            warnUnrecognizedArg = { unrecognized += it },
            noticeConsoleRemoved = { notices += it },
            hasInteractiveTerminal = { true },
        )

        assertEquals(InputMode.MOUSE, mode)
        assertEquals(listOf("Console mode was removed - starting the terminal UI."), notices)
        assertEquals(
            listOf(Event("app_launched", mapOf("mode" to "mouse", "board_size" to 4, "console_flag_ignored" to true))),
            analytics.events,
        )
        assertEquals(emptyList(), unrecognized)
    }

    @Test
    fun `--console with --keyboard starts keyboard mode with the notice`() {
        val notices = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val mode = resolveInputMode(
            arrayOf("--console", "--keyboard"), RecordingAnalyticsService(),
            warnUnrecognizedArg = { warnings += it },
            noticeConsoleRemoved = { notices += it },
            hasInteractiveTerminal = { true },
        )

        assertEquals(InputMode.KEYBOARD, mode)
        assertEquals(1, notices.size)
        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `a normal launch reports console_flag_ignored=false and no notice`() {
        val analytics = RecordingAnalyticsService()
        val notices = mutableListOf<String>()

        resolveInputMode(emptyArray(), analytics, noticeConsoleRemoved = { notices += it }, hasInteractiveTerminal = { true })

        assertEquals(false, analytics.events.single().properties["console_flag_ignored"])
        assertEquals(emptyList(), notices)
    }

    private val scrambled = IntArray(16) { 15 - it }

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService)

    /** A real [GameSession] in [mode] with `--size` semantics, wired through `defaultView`. */
    private fun session(
        mode: InputMode,
        events: List<TerminalEvent> = emptyList(),
        savesDir: Path = createTempDirectory("mordovorot-gh79"),
        sizeChosenAtLaunch: Boolean = true,
    ): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), TerminalSize(80, 40))
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(scrambled) }
        testGameSession(
            initialMode = mode,
            board = board,
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = sizeChosenAtLaunch,
            terminalFactory = { terminal },
            mordie = Mordie(FakeMordieSearchFactory()),
        ).run()
        Run(terminal, board, analytics)
    }

    @Test
    fun `the mouse toolbar offers only the Keyboard switch`() {
        val frame = session(InputMode.MOUSE).terminal.frames.first()

        assertTrue(frame.contains("[ Keyboard ]"))
        assertFalse(frame.contains("Console"))
    }

    @Test
    fun `keyboard mode offers only Mouse F7, and F8 does nothing`() {
        val run = session(InputMode.KEYBOARD, listOf(TerminalEvent.FunctionKey(8)))

        for (frame in run.terminal.frames) {
            assertTrue(frame.contains("[Mouse F7]"))
            assertFalse(frame.contains("Console"))
            assertFalse(frame.contains("F8"))
        }
        assertTrue(run.analytics.events.none { it.name == "input_mode_switched" })
        assertEquals(scrambled.toList(), run.board.boardArray.toList())
    }

    @Test
    fun `a failed switch falls back to the previous TUI mode and keeps the game`(): Unit = runBlocking {
        val terminalSize = TerminalSize(80, 40)
        val layout = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD))
        val (leftX, leftY) = layout.leftArrowPosition(0)
        val keyboardButton = layout.toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.KEYBOARD) }
        val terminal = FakeTerminal(
            mutableListOf(TerminalEvent.MouseClick(leftX, leftY), TerminalEvent.MouseClick(keyboardButton.x, keyboardButton.y)),
            terminalSize,
        )
        val board = BoardImpl(4).apply { restoreState(scrambled) }
        val saves = FileSaveRepository(createTempDirectory("mordovorot-gh79"))
        val mordie = Mordie(FakeMordieSearchFactory())
        val builds = mutableListOf<Pair<InputMode, Boolean>>()
        testGameSession(
            initialMode = InputMode.MOUSE,
            mordie = mordie,
            buildView = { mode, restoreDone ->
                builds += mode to restoreDone
                if (mode == InputMode.KEYBOARD) throw RuntimeException("stty not found")
                defaultView(mode, board, saves, RecordingAnalyticsService(), restoreDone, sizeChosenAtLaunch = true, mordie, CurrentSaveName()) { terminal }
            },
        ).run()

        // Shown in the rebuilt TUI itself - on stderr the full-screen redraw would hide it.
        assertTrue(terminal.frames.any { it.contains("Could not switch to keyboard mode (stty not found) - staying in mouse mode.") })
        assertEquals(listOf(InputMode.MOUSE to false, InputMode.KEYBOARD to true, InputMode.MOUSE to true), builds)
        val lastFrame = terminal.frames.last().replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
        assertEquals("Moves: 1", lastFrame[layout.movesRow].trim())
        assertTrue(terminal.frames.none { it.contains("Restore a saved game?") })
        assertEquals(1, board.moveCount)
    }

    @Test
    fun `a save made before GH-79 still restores at TUI startup`() {
        val saved = IntArray(16) { (it + 5) % 16 }
        val dir = createTempDirectory("mordovorot-gh79")
        dir.resolve("old.save").writeText("4\n${saved.joinToString(" ")}\n7\n")

        // Restore dialog: Tab from the one row to Load, Enter.
        val run = session(InputMode.KEYBOARD, listOf(TerminalEvent.Tab, TerminalEvent.Enter), dir, sizeChosenAtLaunch = false)

        assertTrue(run.terminal.frames.first().contains("old (4x4)"))
        assertEquals(saved.toList(), run.board.boardArray.toList())
        val lastFrame = run.terminal.frames.last().replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
        assertEquals("Moves: 7", lastFrame[BoardLayout(TerminalSize(80, 40), 4).movesRow].trim())
    }
}
