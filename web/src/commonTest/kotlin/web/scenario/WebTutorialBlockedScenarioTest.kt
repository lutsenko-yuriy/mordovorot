package web.scenario

import kotlin.test.Test

/** GH-288 (WU0) scenario stubs: shifts that aren't asked for, and the keys and buttons during steps 1 to 5. The WU that wires the tutorial fills them in. */
class WebTutorialBlockedScenarioTest {

    @Test
    fun `another row is blocked`() = webTest {
        // TODO: 1. On step 1, shift row 2.
        // TODO: 2. The board is unchanged.
        // TODO: 3. Moves reads 0.
        // TODO: 4. The panel shows `You can do that later. For now shift that row please!`.
    }

    @Test
    fun `a column on step 1 is blocked`() = webTest {
        // TODO: 1. On step 1, shift column 2.
        // TODO: 2. The board is unchanged.
        // TODO: 3. The panel shows the blocked line.
    }

    @Test
    fun `the wrong direction is blocked`() = webTest {
        // TODO: 1. On step 1, click `▶` on row 1.
        // TODO: 2. The board is unchanged.
        // TODO: 3. The panel shows the blocked line.
    }

    @Test
    fun `a drag of two tiles on step 1 is blocked`() = webTest {
        // TODO: 1. Drag the top row two tiles left.
        // TODO: 2. The board is unchanged.
        // TODO: 3. Moves reads 0.
        // TODO: 4. The panel shows the blocked line.
    }

    @Test
    fun `a blocked shift on a column step says that column`() = webTest {
        // TODO: 1. Do step 1.
        // TODO: 2. Shift row 2.
        // TODO: 3. The panel says `… shift that column please!`.
    }

    @Test
    fun `a blocked shift doesn't reach Mordie`() = webTest {
        // TODO: 1. Make a blocked shift.
        // TODO: 2. Mordie's shift count is unchanged.
    }

    @Test
    fun `the next shift that counts replaces the blocked line`() = webTest {
        // TODO: 1. Make a blocked shift.
        // TODO: 2. Make the asked shift.
        // TODO: 3. The panel shows step 2's text.
    }

    @Test
    fun `a drag released at 0 tiles does nothing`() = webTest {
        // TODO: 1. Drag the top row and release where it began.
        // TODO: 2. The panel is unchanged.
        // TODO: 3. No blocked line shows.
    }

    @Test
    fun `a shift key that isn't asked for is blocked`() = webTest {
        // TODO: 1. On step 1, select row 2.
        // TODO: 2. Press `ArrowLeft`.
        // TODO: 3. The board is unchanged.
        // TODO: 4. The panel shows the blocked line.
    }

    @Test
    fun `select keys work during the tutorial`() = webTest {
        // TODO: 1. Press `d`.
        // TODO: 2. The selection moves right.
        // TODO: 3. The selection outlines show.
    }

    @Test
    fun `digits 1, 2, 3, 5 and 7 do nothing`() = webTest {
        // TODO: 1. Press each of the five digits in turn.
        // TODO: 2. No dialog opens. (One loop over the digits, as in GH-287.)
    }

    @Test
    fun `4 opens Labels and 6 opens Controls`() = webTest {
        // TODO: 1. Press 4.
        // TODO: 2. Labels opens.
        // TODO: 3. Close it.
        // TODO: 4. Press 6.
        // TODO: 5. Controls opens.
    }

    @Test
    fun `the panel's Controls and Labels buttons open their dialogs`() = webTest {
        // TODO: 1. Press the panel's Controls button.
        // TODO: 2. Controls opens.
        // TODO: 3. Close it.
        // TODO: 4. Press the panel's Labels button.
        // TODO: 5. Labels opens.
    }

    @Test
    fun `closing a dialog keeps the step`() = webTest {
        // TODO: 1. Do step 1.
        // TODO: 2. Open and close Labels.
        // TODO: 3. The panel still shows step 2.
    }
}
