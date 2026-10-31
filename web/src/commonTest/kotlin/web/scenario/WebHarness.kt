package web.scenario

import board_model.BoardImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import storage.SavedMordie
import testing.FakeMordieSearchFactory
import testing.RecordingAnalyticsService
import viewmodel.KeyboardLayout
import web.Focus
import web.Host
import web.KeyPress
import web.Layout
import web.PlayMode
import web.PressedWith
import web.WebDialog
import web.WebGame
import web.WebRenderer
import web.WebState
import web.startWebGame
import web.storage.BrowserPreferenceRepository
import web.storage.BrowserSaveRepository
import web.storage.InMemoryKeyValueStore

/** Solved 4x4 with row 0 shifted one tile right: `shiftLeft(0)` solves it. */
internal val ONE_SHIFT_FROM_SOLVED = intArrayOf(3, 0, 1, 2) + IntArray(12) { it + 4 }

/** The page over the production wiring ([startWebGame]): a real [BoardImpl], the browser's save
 *  and preference repositories over an in-memory [store], a fake Mordie search and recording
 *  analytics. A new harness over the same [store] is a reload. */
internal class WebHarness(
    private val scope: CoroutineScope,
    val board: BoardImpl = BoardImpl(),
    val store: InMemoryKeyValueStore = InMemoryKeyValueStore(),
    private val layout: Layout = Layout.WIDE,
    private val appVersion: String = "9.9.9",
    private val host: Host = Host.LOCALHOST,
    private val touch: Boolean = false,
) {
    val saves = BrowserSaveRepository(store)
    val preferences = BrowserPreferenceRepository(store)
    val analytics = RecordingAnalyticsService()
    val mordieSearches = FakeMordieSearchFactory()
    val mordie = Mordie(mordieSearches)
    val pressedWith = PressedWith()
    private val renderer = FakeWebRenderer()
    lateinit var game: WebGame
        private set

    val state: WebState get() = renderer.frames.last()

    /** Every frame drawn so far, the latest last. */
    val frames: List<WebState> get() = renderer.frames

    /** Every announcement so far (GH-166), one per announcing frame; [startOn] clears it. */
    val said: MutableList<String> get() = renderer.said

    /** Every request to scroll the solution's steps, in lines (GH-171). */
    val scrolled: List<Int> get() = renderer.scrolled

    fun start(): WebHarness {
        game = startWebGame(board, saves, preferences, analytics, mordie, renderer, scope, layout, appVersion, host, touch, pressedWith)
        return this
    }

    /** Starts on [tiles] with the startup restore and size picker cancelled and the events and
     *  announcements so far cleared. */
    fun startOn(tiles: IntArray = ONE_SHIFT_FROM_SOLVED): WebHarness {
        board.restoreState(tiles, 0)
        start()
        if (state.dialog is WebDialog.Load) game.onLoadChosen(null)
        game.onSizeChosen(null)
        analytics.events.clear()
        said.clear()
        return this
    }

    /** Remembers keyboard mode, and the left-handed layout when asked, as an earlier visit would. */
    fun remembering(mode: PlayMode = PlayMode.KEYBOARD, keys: KeyboardLayout? = null): WebHarness {
        preferences.set("input_mode", mode.wireName)
        keys?.let { preferences.set("keyboard_layout", it.wireName) }
        return this
    }

    /** A key pressed outside a dialog with focus on [from]; whether the game took it. */
    fun press(key: String, from: Focus? = null, modified: Boolean = false, repeat: Boolean = false, code: String = ""): Boolean =
        game.onKey(KeyPress(key, modified, repeat, code), from?.key)

    fun seed(name: String, tiles: IntArray, moves: Int = 0, mordie: SavedMordie? = null): WebHarness {
        saves.save(name, tiles, sideOf(tiles), moves, mordie)
        return this
    }

    /** [props] without the decorators' properties. */
    fun ownProps(event: String) = props(event).map { it - "platform" - "input_method" }

    fun props(event: String) = analytics.events.filter { it.name == event }.map { it.properties }
}

private class FakeWebRenderer : WebRenderer {
    val frames = mutableListOf<WebState>()
    val said = mutableListOf<String>()
    val scrolled = mutableListOf<Int>()

    override fun scrollSteps(lines: Int) {
        scrolled += lines
    }

    override fun render(state: WebState) {
        frames += state
        state.announcement?.let { said += it }
    }
}

/** Runs [block] with the page's coroutines running eagerly, so each intent has finished when it returns. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun webTest(block: suspend TestScope.() -> Unit): TestResult = runTest(UnconfinedTestDispatcher()) { block() }

/** A 3x3 with two tiles swapped: no sequence of shifts solves it. */
internal val UNSOLVABLE_3X3 = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)

/** The solved board of [side]. */
internal fun solved(side: Int) = IntArray(side * side) { it }

private fun sideOf(tiles: IntArray) = generateSequence(1) { it + 1 }.first { it * it >= tiles.size }

/** [tiles] as the page shows them: 1-based. */
internal fun shown(tiles: IntArray) = tiles.map { it + 1 }

/** The frame without its one-time focus (GH-163), announcement (GH-166) and slide, for comparing what
 *  two frames show. */
internal fun WebState.unfocused() = copy(focus = null, announcement = null, slide = null)
