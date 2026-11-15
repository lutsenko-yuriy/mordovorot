package web.scenario

import kotlin.test.Test

/** GH-287 (WU1) scenario stubs: on a computer everything shows and works, with no mode to choose; a touch screen
 *  keeps its look. */
class WebComputerScenarioTest {

    @Test
    fun `the arrows and key hints show on a computer`() = webTest {
        // TODO: 1. Start on a computer.
        // TODO: 2. The arrows are in the page.
        // TODO: 3. The key hints are shown.
    }

    @Test
    fun `on a touch screen there are no key hints`() = webTest {
        // TODO: 1. Start on a touch screen.
        // TODO: 2. The arrows show.
        // TODO: 3. No key hints are shown.
    }

    @Test
    fun `every toolbar button shows its digit on a computer`() = webTest {
        // TODO: 1. Start on a computer.
        // TODO: 2. Each toolbar button carries its digit.
    }

    // One loop over the digits: splitting it into seven scenarios would repeat the same steps.
    @Test
    fun `each digit works wherever focus is`() = webTest {
        // TODO: 1. Focus a toolbar button, not the board.
        // TODO: 2. Press 1 to 7 in turn.
        // TODO: 3. Each one does what its button does, and Escape closes the dialog it opened.
    }

    @Test
    fun `digits do nothing inside a dialog`() = webTest {
        // TODO: 1. Open the Save dialog.
        // TODO: 2. Press 1.
        // TODO: 3. The dialog stays and no other action starts.
    }

    @Test
    fun `on a touch screen no digit shows and none works`() = webTest {
        // TODO: 1. Start on a touch screen.
        // TODO: 2. No toolbar button shows a digit.
        // TODO: 3. Press 1.
        // TODO: 4. Nothing happens.
    }

    @Test
    fun `the board has focus at load`() = webTest {
        // TODO: 1. Start on a computer.
        // TODO: 2. Focus is on the board.
    }

    @Test
    fun `the board has focus after a new, loaded or saved game`() = webTest {
        // TODO: 1. Start a new game from the toolbar.
        // TODO: 2. Focus is on the board.
        // TODO: 3. Load a game.
        // TODO: 4. Focus is on the board.
        // TODO: 5. Save a game.
        // TODO: 6. Focus is on the board.
    }

    @Test
    fun `closing a dialog puts focus back on the board`() = webTest {
        // TODO: 1. Open the Share dialog.
        // TODO: 2. Close it.
        // TODO: 3. Focus is on the board.
    }

    @Test
    fun `a press on a tile selects it and focuses the board`() = webTest {
        // TODO: 1. Press a tile with the mouse.
        // TODO: 2. The tile is selected.
        // TODO: 3. Focus is on the board.
    }
}
