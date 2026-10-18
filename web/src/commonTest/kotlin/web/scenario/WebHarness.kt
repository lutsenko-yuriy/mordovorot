package web.scenario

import board_model.BoardImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import storage.SaveRepository
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.RecordingAnalyticsService
import web.Layout
import web.WebGame
import web.WebRenderer
import web.WebState
import web.startWebGame

/** Solved 4x4 with row 0 shifted one tile right: `shiftLeft(0)` solves it. */
internal val ONE_SHIFT_FROM_SOLVED = intArrayOf(3, 0, 1, 2) + IntArray(12) { it + 4 }

/** The page over the production wiring ([startWebGame]) with fakes at its edges. */
internal class WebHarness(
    private val scope: CoroutineScope,
    val board: BoardImpl = BoardImpl(),
    private val saves: SaveRepository = FakeSaveRepository(),
    private val layout: Layout = Layout.WIDE,
) {
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

    /** Starts on [tiles] with the startup size picker cancelled and the events so far cleared. */
    fun startOn(tiles: IntArray = ONE_SHIFT_FROM_SOLVED): WebHarness {
        board.restoreState(tiles, 0)
        start()
        game.onSizeChosen(null)
        analytics.events.clear()
        return this
    }

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

/** [tiles] as the page shows them: 1-based. */
internal fun shown(tiles: IntArray) = tiles.map { it + 1 }
