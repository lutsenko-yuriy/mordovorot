package web.scenario

import kotlin.test.Test

/** GH-287 (WU2) scenario stubs: the selected row and column show after a select or shift key, and hide after a
 *  tile or arrow press. The selection itself is kept. A hover never hides it (a cursor can't be moved headless). */
class WebSelectionScenarioTest {

    @Test
    fun `the selection is hidden at page load`() = webTest {
        // TODO: 1. Start on a computer.
        // TODO: 2. The selection outlines are hidden.
        // TODO: 3. The selection is (0, 0).
    }

    @Test
    fun `a select key shows the selection`() = webTest {
        // TODO: 1. Press `d`.
        // TODO: 2. The outlines show on the new row and column.
    }

    @Test
    fun `a shift key shows the selection`() = webTest {
        // TODO: 1. Press the shift-left key.
        // TODO: 2. The outlines show.
    }

    @Test
    fun `a press on a tile hides the selection and keeps it`() = webTest {
        // TODO: 1. Press `d`.
        // TODO: 2. The outlines show.
        // TODO: 3. Press a tile with the mouse.
        // TODO: 4. The outlines are hidden.
        // TODO: 5. The selection is on that tile.
        // TODO: 6. Focus is on the board.
    }

    @Test
    fun `an arrow press with the mouse hides the selection`() = webTest {
        // TODO: 1. Press `d`.
        // TODO: 2. Click an arrow.
        // TODO: 3. The outlines are hidden.
    }

    @Test
    fun `an arrow pressed by key hides the selection`() = webTest {
        // TODO: 1. Press `d`.
        // TODO: 2. Focus an arrow and press Enter.
        // TODO: 3. The outlines are hidden.
    }

    @Test
    fun `a key shows it again where it was`() = webTest {
        // TODO: 1. Press `d` and `s`, then press a tile (the selection moves to that tile).
        // TODO: 2. Press `a`.
        // TODO: 3. The outlines show, one column left of the pressed tile.
    }

    @Test
    fun `the selection's visibility carries across a new game`() = webTest {
        // TODO: 1. Press `d`, then start a new game.
        // TODO: 2. The outlines still show.
        // TODO: 3. Press a tile, then start a new game.
        // TODO: 4. The outlines are still hidden.
    }

    @Test
    fun `the selection never shows on a touch screen`() = webTest {
        // TODO: 1. Start on a touch screen.
        // TODO: 2. The outlines are never in the page.
    }
}
