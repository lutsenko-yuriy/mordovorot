package web.scenario

import board_model.BoardImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import storage.SavedMordie
import testing.FakeMordieSearchFactory
import testing.RecordingAnalyticsService
import web.Layout
import web.WebDialog
import web.WebGame
import web.WebRenderer
import web.WebState
import web.startWebGame
import web.storage.BrowserSaveRepository
import web.storage.InMemoryKeyValueStore

/** Solved 4x4 with row 0 shifted one tile right: `shiftLeft(0)` solves it. */
internal val ONE_SHIFT_FROM_SOLVED = intArrayOf(3, 0, 1, 2) + IntArray(12) { it + 4 }

/** The page over the production wiring ([startWebGame]): a real [BoardImpl], the browser's save
 *  repository over an in-memory [store], a fake Mordie search and recording analytics. A new
 *  harness over the same [store] is a reload. */
internal class WebHarness(
    private val scope: CoroutineScope,
    val board: BoardImpl = BoardImpl(),
    val store: InMemoryKeyValueStore = InMemoryKeyValueStore(),
    private val layout: Layout = Layout.WIDE,
) {
    val saves = BrowserSaveRepository(store)
    val analytics = RecordingAnalyticsService()
    val mordieSearches = FakeMordieSearchFactory()
    private val renderer = FakeWebRenderer()
    lateinit var game: WebGame
        private set

    val state: WebState get() = renderer.frames.last()

    fun start(): WebHarness {
        game = startWebGame(board, saves, analytics, Mordie(mordieSearches), renderer, scope, layout)
        return this
    }

    /** Starts on [tiles] with the startup restore and size picker cancelled and the events so far cleared. */
    fun startOn(tiles: IntArray = ONE_SHIFT_FROM_SOLVED): WebHarness {
        board.restoreState(tiles, 0)
        start()
        if (state.dialog is WebDialog.Load) game.onLoadChosen(null)
        game.onSizeChosen(null)
        analytics.events.clear()
        return this
    }

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

    override fun render(state: WebState) {
        frames += state
    }
}

/** Runs [block] with the page's coroutines running eagerly, so each intent has finished when it returns. */
internal fun webTest(block: suspend TestScope.() -> Unit): TestResult = runTest(UnconfinedTestDispatcher()) { block() }

/** A 3x3 with two tiles swapped: no sequence of shifts solves it. */
internal val UNSOLVABLE_3X3 = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)

/** The solved board of [side]. */
internal fun solved(side: Int) = IntArray(side * side) { it }

private fun sideOf(tiles: IntArray) = generateSequence(1) { it + 1 }.first { it * it >= tiles.size }

/** [tiles] as the page shows them: 1-based. */
internal fun shown(tiles: IntArray) = tiles.map { it + 1 }
