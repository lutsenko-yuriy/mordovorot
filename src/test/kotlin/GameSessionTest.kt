import board_model.BoardModel
import presenter.ModeSwitchRequestedException
import storage.SaveRepository
import testing.FakeBoardModel
import testing.FakeSaveRepository
import view.View
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers GH-30's session loop: `GameSession` rebuilding the View/presenter/analytics stack
 * around the same `BoardModel`/`SaveRepository` instances every time `play()` unwinds with a
 * `ModeSwitchRequestedException`, and ending cleanly when `play()` returns normally instead.
 */
class GameSessionTest {

    /** A minimal [View] test double - every method but [play] is a no-op, [play] runs the
     *  scripted [onPlay] (throw a [ModeSwitchRequestedException], or return normally to
     *  simulate a clean exit). Lets these tests drive [GameSession.run]'s rebuild loop without
     *  any real console/terminal I/O. */
    private class ScriptedView(private val onPlay: () -> Unit) : View {
        override fun displayBoard(boardState: IntArray, squareSide: Int) {}
        override fun showMessage(message: String) {}
        override fun processCommand() {}
        override fun confirmRestore(saveName: String): Boolean = false
        override fun chooseSaveToRestore(saveNames: List<String>): String? = null
        override fun confirmSaveBeforeExit(): Boolean = false
        override fun promptSaveName(): String? = null
        override fun play() = onPlay()
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

        session.run()

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

        session.run()

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

        session.run()

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

        session.run()

        assertEquals(1, built)
    }
}
