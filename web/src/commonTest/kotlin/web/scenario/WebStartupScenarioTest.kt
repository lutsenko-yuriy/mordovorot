package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import web.WebDialog

/** GH-110 scenarios: what the page opens with. Each scenario drives `WebGame` through a
 *  `FakeWebRenderer` that records every `WebState` frame, over a real `BoardImpl`,
 *  `BrowserSaveRepository(InMemoryKeyValueStore)`, `Mordie(FakeMordieSearchFactory())` and a
 *  `RecordingAnalyticsService` in the production decorators. Each test names the WU that makes it green. */
class WebStartupScenarioTest {

    @Test
    fun `no saves - the page opens the size picker`() = webTest {
        val page = WebHarness(backgroundScope).start()
        assertEquals(WebDialog.Size(listOf(3, 4, 5)), page.state.dialog)

        page.game.onSizeChosen(4)

        assertEquals(4, page.state.side)
        assertEquals(16, page.state.tiles.size)
        assertEquals(0, page.state.moves)
        assertNull(page.state.dialog)
        assertEquals(listOf("startup"), page.props("screen_size_dialog").map { it["opened_from"] })
    }

    @Test
    fun `no saves - Cancel on the startup size picker keeps the current board`() = webTest {
        val page = WebHarness(backgroundScope)
        page.board.restoreState(ONE_SHIFT_FROM_SOLVED, 0)
        page.start()

        page.game.onSizeChosen(null)

        assertEquals(shown(ONE_SHIFT_FROM_SOLVED), page.state.tiles)
        assertEquals(0, page.state.moves)
        assertNull(page.state.dialog)
        assertEquals(listOf("size"), page.props("dialog_cancelled").map { it["dialog"] })
    }

    @Test
    fun `one save - Restore a saved game? yes loads it`() {
        // WU5
        // TODO: 1. Seed "foo" (5x5, 3 moves).
        // TODO: 2. The dialog is "Restore a saved game?" for "foo (5x5)".
        // TODO: 3. Answer yes: the board is foo's arrangement, "Moves: 3".
    }

    @Test
    fun `one save - answering no opens the size picker`() {
        // WU5
        // TODO: 1. Seed "foo"; answer no.
        // TODO: 2. The size picker opens; "foo" is still in the store.
    }

    @Test
    fun `two or more saves - a pick list, and the chosen one loads`() {
        // WU5
        // TODO: 1. Seed "a" (3x3) and "b" (4x4).
        // TODO: 2. The pick list shows "a (3x3)" and "b (4x4)".
        // TODO: 3. Pick "b": b's board loads.
    }

    @Test
    fun `unsolvable save at startup - Repair & load or New game, no Exit`() {
        // WU5
        // TODO: 1. Seed "odd" (3x3 "2 1 0 3 4 5 6 7 8", 5 moves); answer yes to restore it.
        // TODO: 2. The "Can't be solved" dialog has exactly [ Repair & load ] [ New game ].
        // TODO: 3. Repair & load: the board is "1 2 0 3 4 5 6 7 8"; the status is
        //          "Repaired 'odd' (two tiles swapped) and loaded it."
    }
}
