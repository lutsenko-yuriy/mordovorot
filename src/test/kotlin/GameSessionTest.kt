import board_model.BoardModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import storage.SaveRepository
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import view.View
import view.ViewImpl
import viewmodel.ExitRequestedException
import viewmodel.ModeSwitchRequestedException
import viewmodel.ModeSwitcherImpl

/** Covers GH-30's `GameSession`: rebuilds around the same board/saves on
 *  `ModeSwitchRequestedException`, ends when `play()` returns normally. */
class GameSessionTest {

    /** [View] test double: only [play] does anything, running the scripted [onPlay]. */
    private class ScriptedView(private val onPlay: () -> Unit) : View {
        override fun displayBoard(boardState: IntArray, squareSide: Int) {}
        override suspend fun showMessage(message: String) {}
        override suspend fun processCommand() {}
        override suspend fun play() = onPlay()
    }

    private data class BuildCall(val mode: InputMode, val board: BoardModel, val saves: SaveRepository, val startupRestoreDone: Boolean)

    @Test
    fun `switching modes preserves board state and does not reshuffle`(): Unit = runBlocking {
        val board = FakeBoardModel(boardArray = intArrayOf(3, 2, 1, 0))
        val calls = mutableListOf<BuildCall>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            board = board,
            buildView = { mode, b, s, _, restoreDone, _ ->
                calls.add(BuildCall(mode, b, s, restoreDone))
                built++
                ScriptedView {
                    if (built == 1) throw ModeSwitchRequestedException(InputMode.MOUSE)
                    // second build (mouse mode): end the session
                }
            },
        )

        session.run()

        assertEquals(listOf(InputMode.CONSOLE, InputMode.MOUSE), calls.map { it.mode })
        assertTrue(calls.all { it.board === board })
        assertFalse(board.calls.contains("resetGame"))
    }

    @Test
    fun `switching modes skips the startup restore prompt on sessions after the first`(): Unit = runBlocking {
        val saves = FakeSaveRepository(mutableMapOf("slot1" to storage.SavedBoard(4, IntArray(16) { it })))
        val calls = mutableListOf<BuildCall>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            saves = saves,
            buildView = { mode, b, s, _, restoreDone, _ ->
                calls.add(BuildCall(mode, b, s, restoreDone))
                built++
                ScriptedView {
                    if (built == 1) throw ModeSwitchRequestedException(InputMode.KEYBOARD)
                }
            },
        )

        session.run()

        assertEquals(listOf(false, true), calls.map { it.startupRestoreDone })
    }

    @Test
    fun `sizeChosenAtLaunch reaches every rebuild, including across a mode switch`(): Unit = runBlocking {
        val sizeFlags = mutableListOf<Boolean>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            sizeChosenAtLaunch = true,
            buildView = { mode, _, _, _, _, sizeAtLaunch ->
                sizeFlags.add(sizeAtLaunch)
                built++
                ScriptedView {
                    if (built == 1) throw ModeSwitchRequestedException(InputMode.MOUSE)
                }
            },
        )

        session.run()

        assertEquals(listOf(true, true), sizeFlags)
    }

    @Test
    fun `three consecutive switches cycle through console, mouse, and keyboard, each rebuilding a fresh View-viewModel`(): Unit = runBlocking {
        val modesBuilt = mutableListOf<InputMode>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            buildView = { mode, _, _, _, _, _ ->
                modesBuilt.add(mode)
                built++
                ScriptedView {
                    when (built) {
                        1 -> throw ModeSwitchRequestedException(InputMode.MOUSE)
                        2 -> throw ModeSwitchRequestedException(InputMode.KEYBOARD)
                        else -> {} // third build (keyboard mode): return normally, exit
                    }
                }
            },
        )

        session.run()

        assertEquals(listOf(InputMode.CONSOLE, InputMode.MOUSE, InputMode.KEYBOARD), modesBuilt)
        assertEquals(3, built)
    }

    @Test
    fun `play returning normally (exit) ends the session loop without another rebuild`(): Unit = runBlocking {
        var built = 0

        val session = GameSession(
            initialMode = InputMode.MOUSE,
            buildView = { _, _, _, _, _, _ ->
                built++
                ScriptedView {} // returns normally - simulates exit
            },
        )

        session.run()

        assertEquals(1, built)
    }

    @Test
    fun `a failed rebuild after a mode switch falls back to console instead of crashing, skipping the restore prompt`(): Unit = runBlocking {
        val calls = mutableListOf<BuildCall>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            buildView = { mode, b, s, _, restoreDone, _ ->
                calls.add(BuildCall(mode, b, s, restoreDone))
                built++
                when (built) {
                    1 -> ScriptedView { throw ModeSwitchRequestedException(InputMode.MOUSE) }
                    2 -> ScriptedView { throw RuntimeException("stty not found") } // simulated terminal-setup failure
                    else -> ScriptedView {} // recovered console session exits normally
                }
            },
        )

        session.run() // must not throw

        assertEquals(listOf(InputMode.CONSOLE, InputMode.MOUSE, InputMode.CONSOLE), calls.map { it.mode })
        // The fallback build (3rd) must not re-show the startup restore prompt over a live board.
        assertEquals(listOf(false, true, true), calls.map { it.startupRestoreDone })
    }

    @Test
    fun `a failure on the very first session still throws - no game in progress to protect`(): Unit = runBlocking {
        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            buildView = { _, _, _, _, _, _ -> ScriptedView { throw RuntimeException("stty not found") } },
        )

        assertFailsWith<RuntimeException> { session.run() }
    }

    @Test
    fun `a second consecutive failure, once already falling back to console, has nowhere safer to go - it throws`(): Unit = runBlocking {
        val modesBuilt = mutableListOf<InputMode>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.MOUSE,
            buildView = { mode, _, _, _, _, _ ->
                modesBuilt.add(mode)
                built++
                when (built) {
                    // a real switch first (not a failure) so isFirstSession is already false
                    // by the time the organic fallback-to-console path below is exercised.
                    1 -> ScriptedView { throw ModeSwitchRequestedException(InputMode.KEYBOARD) }
                    2 -> ScriptedView { throw RuntimeException("stty not found") }
                    else -> ScriptedView { throw RuntimeException("console broke too") }
                }
            },
        )

        assertFailsWith<RuntimeException> { session.run() }
        assertEquals(listOf(InputMode.MOUSE, InputMode.KEYBOARD, InputMode.CONSOLE), modesBuilt)
    }

    @Test
    fun `ExitRequestedException escaping a rebuild reaches the caller, not swallowed as a failed switch`(): Unit = runBlocking {
        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            buildView = { mode, _, _, _, _, _ ->
                ScriptedView {
                    if (mode == InputMode.CONSOLE) throw ModeSwitchRequestedException(InputMode.MOUSE)
                    throw ExitRequestedException()
                }
            },
        )

        assertFailsWith<ExitRequestedException> { session.run() }
    }

    @Test
    fun `the production console View is wired with a real ModeSwitcher, not a silent no-op`(): Unit = runBlocking {
        val view = defaultView(
            InputMode.CONSOLE,
            FakeBoardModel(),
            FakeSaveRepository(),
            RecordingAnalyticsService(),
            startupRestoreDone = true,
            sizeChosenAtLaunch = false,
            terminalFactory = { FakeTerminal() },
        )

        assertIs<ViewImpl>(view)
        assertIs<ModeSwitcherImpl>(view.modeSwitcher)
    }
}
