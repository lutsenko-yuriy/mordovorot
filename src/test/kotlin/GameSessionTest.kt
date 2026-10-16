import board_model.BoardModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import testing.FakeMordieSearchFactory
import storage.SaveRepository
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import view.View
import viewmodel.ExitRequestedException
import viewmodel.ModeSwitchRequestedException
import view.tui.TuiView
import viewmodel.ModeSwitcherImpl

/** Covers GH-30's `GameSession`: rebuilds around the same board/saves on
 *  `ModeSwitchRequestedException`, ends when `play()` returns normally. */
class GameSessionTest {

    /** [View] test double running the scripted [onPlay]; records the notice [play] got. */
    private class ScriptedView(private val onPlay: () -> Unit) : View {
        var notice: String? = null

        override suspend fun play(notice: String?) {
            this.notice = notice
            onPlay()
        }
    }

    private data class BuildCall(val mode: InputMode, val board: BoardModel, val saves: SaveRepository, val startupRestoreDone: Boolean)

    @Test
    fun `switching modes preserves board state and does not reshuffle`(): Unit = runBlocking {
        val board = FakeBoardModel(boardArray = intArrayOf(3, 2, 1, 0))
        val calls = mutableListOf<BuildCall>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.KEYBOARD,
            board = board,
            buildView = { mode, b, s, _, restoreDone, _, _ ->
                calls.add(BuildCall(mode, b, s, restoreDone))
                built++
                ScriptedView {
                    if (built == 1) throw ModeSwitchRequestedException(InputMode.MOUSE)
                    // second build (mouse mode): end the session
                }
            },
        )

        session.run()

        assertEquals(listOf(InputMode.KEYBOARD, InputMode.MOUSE), calls.map { it.mode })
        assertTrue(calls.all { it.board === board })
        assertFalse(board.calls.contains("resetGame"))
    }

    @Test
    fun `switching modes skips the startup restore prompt on sessions after the first`(): Unit = runBlocking {
        val saves = FakeSaveRepository(mutableMapOf("slot1" to storage.SavedBoard(4, IntArray(16) { it })))
        val calls = mutableListOf<BuildCall>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.MOUSE,
            saves = saves,
            buildView = { mode, b, s, _, restoreDone, _, _ ->
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
            initialMode = InputMode.KEYBOARD,
            sizeChosenAtLaunch = true,
            buildView = { _, _, _, _, _, sizeAtLaunch, _ ->
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
    fun `three consecutive switches cycle mouse, keyboard, mouse, each rebuilding a fresh View-viewModel`(): Unit = runBlocking {
        val modesBuilt = mutableListOf<InputMode>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.MOUSE,
            buildView = { mode, _, _, _, _, _, _ ->
                modesBuilt.add(mode)
                built++
                ScriptedView {
                    when (built) {
                        1 -> throw ModeSwitchRequestedException(InputMode.KEYBOARD)
                        2 -> throw ModeSwitchRequestedException(InputMode.MOUSE)
                        else -> {} // third build (mouse mode): return normally, exit
                    }
                }
            },
        )

        session.run()

        assertEquals(listOf(InputMode.MOUSE, InputMode.KEYBOARD, InputMode.MOUSE), modesBuilt)
        assertEquals(3, built)
    }

    @Test
    fun `play returning normally (exit) ends the session loop without another rebuild`(): Unit = runBlocking {
        var built = 0

        val session = GameSession(
            initialMode = InputMode.MOUSE,
            buildView = { _, _, _, _, _, _, _ ->
                built++
                ScriptedView {} // returns normally - simulates exit
            },
        )

        session.run()

        assertEquals(1, built)
    }

    @Test
    fun `a failed rebuild after a mode switch falls back to the previous mode instead of crashing, skipping the restore prompt`(): Unit = runBlocking {
        val calls = mutableListOf<BuildCall>()
        val views = mutableListOf<ScriptedView>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.KEYBOARD,
            buildView = { mode, b, s, _, restoreDone, _, _ ->
                calls.add(BuildCall(mode, b, s, restoreDone))
                built++
                when (built) {
                    1 -> ScriptedView { throw ModeSwitchRequestedException(InputMode.MOUSE) }
                    2 -> ScriptedView { throw RuntimeException("stty not found") } // simulated terminal-setup failure
                    else -> ScriptedView {} // recovered keyboard session exits normally
                }.also { views += it }
            },
        )

        session.run() // must not throw

        assertEquals(listOf(InputMode.KEYBOARD, InputMode.MOUSE, InputMode.KEYBOARD), calls.map { it.mode })
        assertEquals(listOf(null, null, "Could not switch to mouse mode (stty not found) - staying in keyboard mode."), views.map { it.notice })
        // The fallback build (3rd) must not re-show the startup restore prompt over a live board.
        assertEquals(listOf(false, true, true), calls.map { it.startupRestoreDone })
    }

    @Test
    fun `a failure on the very first session still throws - no game in progress to protect`(): Unit = runBlocking {
        val session = GameSession(
            initialMode = InputMode.MOUSE,
            buildView = { _, _, _, _, _, _, _ -> ScriptedView { throw RuntimeException("stty not found") } },
        )

        assertFailsWith<RuntimeException> { session.run() }
    }

    @Test
    fun `a second consecutive failure, once already falling back, has nowhere safer to go - it throws`(): Unit = runBlocking {
        val modesBuilt = mutableListOf<InputMode>()
        var built = 0

        val session = GameSession(
            initialMode = InputMode.MOUSE,
            buildView = { mode, _, _, _, _, _, _ ->
                modesBuilt.add(mode)
                built++
                when (built) {
                    // a real switch first (not a failure) so isFirstSession is already false
                    // by the time the fallback path below is exercised.
                    1 -> ScriptedView { throw ModeSwitchRequestedException(InputMode.KEYBOARD) }
                    2 -> ScriptedView { throw RuntimeException("stty not found") }
                    else -> ScriptedView { throw RuntimeException("mouse broke too") }
                }
            },
        )

        assertFailsWith<RuntimeException> { session.run() }
        assertEquals(listOf(InputMode.MOUSE, InputMode.KEYBOARD, InputMode.MOUSE), modesBuilt)
    }

    @Test
    fun `ExitRequestedException escaping a rebuild reaches the caller, not swallowed as a failed switch`(): Unit = runBlocking {
        val session = GameSession(
            initialMode = InputMode.KEYBOARD,
            buildView = { mode, _, _, _, _, _, _ ->
                ScriptedView {
                    if (mode == InputMode.KEYBOARD) throw ModeSwitchRequestedException(InputMode.MOUSE)
                    throw ExitRequestedException()
                }
            },
        )

        assertFailsWith<ExitRequestedException> { session.run() }
    }

    @Test
    fun `the production View is wired with a real ModeSwitcher, not a silent no-op`(): Unit = runBlocking {
        for (mode in InputMode.entries) {
            val view = defaultView(
                mode,
                FakeBoardModel(),
                FakeSaveRepository(),
                RecordingAnalyticsService(),
                startupRestoreDone = true,
                sizeChosenAtLaunch = false,
                mordie = Mordie(FakeMordieSearchFactory()),
                terminalFactory = { FakeTerminal() },
            )

            assertIs<TuiView>(view)
            assertIs<ModeSwitcherImpl>(view.modeSwitcher)
        }
    }

    @Test
    fun `every rebuild gets the session's one Mordie`(): Unit = runBlocking {
        val mordie = Mordie(FakeMordieSearchFactory())
        val received = mutableListOf<Mordie>()
        var built = 0

        GameSession(
            initialMode = InputMode.MOUSE,
            mordie = mordie,
            buildView = { _, _, _, _, _, _, m ->
                received += m
                built++
                ScriptedView { if (built == 1) throw ModeSwitchRequestedException(InputMode.KEYBOARD) }
            },
        ).run()

        assertEquals(2, received.size)
        assertTrue(received.all { it === mordie })
    }

    @Test
    fun `the search is stopped when the session ends`(): Unit = runBlocking {
        val factory = FakeMordieSearchFactory()
        val mordie = Mordie(factory).apply { startFresh(IntArray(16) { 15 - it }) }

        GameSession(initialMode = InputMode.MOUSE, mordie = mordie, buildView = { _, _, _, _, _, _, _ -> ScriptedView {} }).run()

        assertTrue(factory.latest.cancelled)
    }

    @Test
    fun `the search is stopped when the session ends by throwing`(): Unit = runBlocking {
        val factory = FakeMordieSearchFactory()
        val mordie = Mordie(factory).apply { startFresh(IntArray(16) { 15 - it }) }
        val session = GameSession(
            initialMode = InputMode.MOUSE,
            mordie = mordie,
            buildView = { _, _, _, _, _, _, _ -> ScriptedView { throw ExitRequestedException() } },
        )

        assertFailsWith<ExitRequestedException> { session.run() }
        assertTrue(factory.latest.cancelled)
    }
}
