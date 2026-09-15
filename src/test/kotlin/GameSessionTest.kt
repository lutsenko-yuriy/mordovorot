import board_model.BoardModel
import presenter.ExitRequestedException
import presenter.ModeSwitchRequestedException
import presenter.ModeSwitcherImpl
import storage.SaveRepository
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import view.View
import view.ViewImpl
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Covers GH-30's `GameSession`: rebuilds around the same board/saves on
 *  `ModeSwitchRequestedException`, ends when `play()` returns normally. */
class GameSessionTest {

    /** [View] test double: only [play] does anything, running the scripted [onPlay]. */
    private class ScriptedView(private val onPlay: () -> Unit) : View {
        override fun displayBoard(boardState: IntArray, squareSide: Int) {}
        override suspend fun showMessage(message: String) {}
        override suspend fun processCommand() {}
        override suspend fun confirmRestore(saveName: String): Boolean = false
        override suspend fun chooseSaveToRestore(saveNames: List<String>): String? = null
        override suspend fun confirmSaveBeforeExit(): Boolean = false
        override suspend fun promptSaveName(): String? = null
        override suspend fun play() = onPlay()
    }

    private data class BuildCall(val mode: InputMode, val board: BoardModel, val saves: SaveRepository, val startupRestoreDone: Boolean)

    @Test
    fun `switching modes preserves board state and does not reshuffle`() {
        val board = FakeBoardModel(boardArray = intArrayOf(3, 2, 1, 0))
        val calls = mutableListOf<BuildCall>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            board = board,
            buildView = { mode, b, s, _, restoreDone ->
                calls.add(BuildCall(mode, b, s, restoreDone))
                built++
                ScriptedView {
                    if (built == 1) throw ModeSwitchRequestedException(InputMode.MOUSE)
                    // second build (mouse mode): end the session
                }
            },
        )

        runBlocking { session.run() }

        assertEquals(listOf(InputMode.CONSOLE, InputMode.MOUSE), calls.map { it.mode })
        assertTrue(calls.all { it.board === board })
        assertFalse(board.calls.contains("resetGame"))
    }

    @Test
    fun `switching modes skips the startup restore prompt on sessions after the first`() {
        val saves = FakeSaveRepository(mutableMapOf("slot1" to storage.SavedBoard(4, IntArray(16) { it })))
        val calls = mutableListOf<BuildCall>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            saves = saves,
            buildView = { mode, b, s, _, restoreDone ->
                calls.add(BuildCall(mode, b, s, restoreDone))
                built++
                ScriptedView {
                    if (built == 1) throw ModeSwitchRequestedException(InputMode.KEYBOARD)
                }
            },
        )

        runBlocking { session.run() }

        assertEquals(listOf(false, true), calls.map { it.startupRestoreDone })
    }

    @Test
    fun `three consecutive switches cycle through console, mouse, and keyboard, each rebuilding a fresh View-presenter`() {
        val modesBuilt = mutableListOf<InputMode>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            buildView = { mode, _, _, _, _ ->
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

        runBlocking { session.run() }

        assertEquals(listOf(InputMode.CONSOLE, InputMode.MOUSE, InputMode.KEYBOARD), modesBuilt)
        assertEquals(3, built)
    }

    @Test
    fun `play returning normally (exit) ends the session loop without another rebuild`() {
        var built = 0

        val session = GameSession(
            initialMode = InputMode.MOUSE,
            buildView = { _, _, _, _, _ ->
                built++
                ScriptedView {} // returns normally - simulates exit
            },
        )

        runBlocking { session.run() }

        assertEquals(1, built)
    }

    @Test
    fun `a failed rebuild after a mode switch falls back to console instead of crashing, skipping the restore prompt`() {
        val calls = mutableListOf<BuildCall>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            buildView = { mode, b, s, _, restoreDone ->
                calls.add(BuildCall(mode, b, s, restoreDone))
                built++
                when (built) {
                    1 -> ScriptedView { throw ModeSwitchRequestedException(InputMode.MOUSE) }
                    2 -> ScriptedView { throw RuntimeException("stty not found") } // simulated terminal-setup failure
                    else -> ScriptedView {} // recovered console session exits normally
                }
            },
        )

        runBlocking { session.run() } // must not throw

        assertEquals(listOf(InputMode.CONSOLE, InputMode.MOUSE, InputMode.CONSOLE), calls.map { it.mode })
        // The fallback build (3rd) must not re-show the startup restore prompt over a live board.
        assertEquals(listOf(false, true, true), calls.map { it.startupRestoreDone })
    }

    @Test
    fun `a failure on the very first session still throws - no game in progress to protect`() {
        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            buildView = { _, _, _, _, _ -> ScriptedView { throw RuntimeException("stty not found") } },
        )

        assertFailsWith<RuntimeException> { runBlocking { session.run() } }
    }

    @Test
    fun `a second consecutive failure, once already falling back to console, has nowhere safer to go - it throws`() {
        val modesBuilt = mutableListOf<InputMode>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.MOUSE,
            buildView = { mode, _, _, _, _ ->
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

        assertFailsWith<RuntimeException> { runBlocking { session.run() } }
        assertEquals(listOf(InputMode.MOUSE, InputMode.KEYBOARD, InputMode.CONSOLE), modesBuilt)
    }

    @Test
    fun `ExitRequestedException escaping a rebuild reaches the caller, not swallowed as a failed switch`() {
        val session = GameSession(
            initialMode = InputMode.CONSOLE,
            buildView = { mode, _, _, _, _ ->
                ScriptedView {
                    if (mode == InputMode.CONSOLE) throw ModeSwitchRequestedException(InputMode.MOUSE)
                    throw ExitRequestedException()
                }
            },
        )

        assertFailsWith<ExitRequestedException> { runBlocking { session.run() } }
    }

    @Test
    fun `the production console View is wired with a real ModeSwitcher, not a silent no-op`() {
        val view = defaultView(
            InputMode.CONSOLE,
            FakeBoardModel(),
            FakeSaveRepository(),
            RecordingAnalyticsService(),
            startupRestoreDone = true,
            terminalFactory = { FakeTerminal() },
        )

        assertIs<ViewImpl>(view)
        assertIs<ModeSwitcherImpl>(view.modeSwitcher)
    }
}
