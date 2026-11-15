package web.scenario

import kotlin.test.Test

/** GH-288 (WU0) scenario stubs: steps 5 and 6, the game after the tutorial, the phone variant and the labels. The WU that wires the tutorial fills them in. */
class WebTutorialEndingScenarioTest {

    @Test
    fun `step 5 says Can you beat my results over the solved board`() = webTest {
        // TODO: 1. Play the four steps.
        // TODO: 2. The board is solved.
        // TODO: 3. The panel says `Can you beat my results?`.
    }

    @Test
    fun `step 5 holds Mordie's ending back`() = webTest {
        // TODO: 1. Be on step 5.
        // TODO: 2. Mordie's ending isn't shown yet.
    }

    @Test
    fun `step 6 comes after 3 seconds`() = webTest {
        // TODO: 1. Be on step 5.
        // TODO: 2. Advance 3 s.
        // TODO: 3. The panel is gone.
        // TODO: 4. The toolbar and the Give Up row are back.
    }

    @Test
    fun `Mordie's ending shows at step 6`() = webTest {
        // TODO: 1. Be on step 5.
        // TODO: 2. Advance 3 s.
        // TODO: 3. Mordie says `Cool, you matched me! 4 moves each!`.
    }

    @Test
    fun `Save works after step 6`() = webTest {
        // TODO: 1. Play to step 6.
        // TODO: 2. Save as `first`.
        // TODO: 3. The save exists.
    }

    @Test
    fun `Load works after step 6`() = webTest {
        // TODO: 1. Seed a save and play to step 6.
        // TODO: 2. Load the save.
        // TODO: 3. The loaded board is shown.
    }

    @Test
    fun `New works after step 6`() = webTest {
        // TODO: 1. Play to step 6.
        // TODO: 2. Choose New, then 5x5.
        // TODO: 3. A 5x5 board is shown.
    }

    @Test
    fun `on a phone, the texts name dragging only`() = webTest {
        // TODO: 1. First launch on touch.
        // TODO: 2. Step 1's text says to drag.
        // TODO: 3. It doesn't mention the `◀` arrow.
        // TODO: 4. Do step 1.
        // TODO: 5. Step 2's text says to drag.
    }

    @Test
    fun `on a phone, the panel has no Controls button`() = webTest {
        // TODO: 1. First launch on touch.
        // TODO: 2. The panel has Labels and Skip only.
    }

    @Test
    fun `a Labels change renames the tile in step 2's text`() = webTest {
        // TODO: 1. Switch the labels to Letters.
        // TODO: 2. Do step 1.
        // TODO: 3. Step 2's text names the tile by its letter.
    }
}
