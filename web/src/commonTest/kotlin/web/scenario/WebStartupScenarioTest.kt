package web.scenario

import kotlin.test.Test

/** GH-110 scenarios: what the page opens with. Each scenario drives `WebGame` through a
 *  `FakeWebRenderer` that records every `WebState` frame, over a real `BoardImpl`,
 *  `BrowserSaveRepository(InMemoryKeyValueStore)`, `Mordie(FakeMordieSearchFactory())` and a
 *  `RecordingAnalyticsService` in the production decorators. Each test names the WU that makes it green. */
class WebStartupScenarioTest {

    @Test
    fun `no saves - the page opens the size picker, with no Cancel`() {
        // WU4
        // TODO: 1. Start with an empty store.
        // TODO: 2. The first dialog is the size picker (3-5) with no Cancel.
        // TODO: 3. Pick 4: a 4x4 board, "Moves: 0", no dialog.
        // TODO: 4. screen_size_dialog {opened_from: startup} fired.
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
        // TODO: 1. Seed "odd" (3x3 "2 1 0 3 4 5 6 7 8", 5 moves); pick it.
        // TODO: 2. The "Can't be solved" dialog has exactly [ Repair & load ] [ New game ].
        // TODO: 3. Repair & load: the board is "1 2 0 3 4 5 6 7 8"; the status is
        //          "Repaired 'odd' (two tiles swapped) and loaded it."
    }
}
