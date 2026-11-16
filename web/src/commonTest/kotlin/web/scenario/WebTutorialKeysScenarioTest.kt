package web.scenario

import kotlin.test.Test

/** GH-290: on a computer, the tutorial's steps list the ways to do the asked shift: drag, click, keys. */
class WebTutorialKeysScenarioTest {

    @Test
    fun `step 1 lists drag, click and keys`() = webTest {
        // TODO: 1. First launch on a computer, right hand, selection on tile 1
        // TODO: 2. The bubble's lead sentence is the computer's step 1 text
        // TODO: 3. Its lines are `Drag it left`, `Click its ◀`, `Press H or ←`
    }

    @Test
    fun `step 2 starts the key line with a select key`() = webTest {
        // TODO: 1. Play step 1 by arrow
        // TODO: 2. The lines are `Drag it up`, `Click its ▲`, `Press D, then K or ↑`
    }

    @Test
    fun `the key line names what is left after a select key`() = webTest {
        // TODO: 1. Play step 1
        // TODO: 2. Press D
        // TODO: 3. The key line is `Press K or ↑`
    }

    @Test
    fun `a tile press redraws the key line`() = webTest {
        // TODO: 1. Play step 1
        // TODO: 2. Press the tile in the second column
        // TODO: 3. The key line is `Press K or ↑`
    }

    @Test
    fun `a column far from the selection asks for at most three select keys`() = webTest {
        // TODO: 1. Play steps 1 to 3 (the selection stays on column 1)
        // TODO: 2. Step 4's column is the fourth
        // TODO: 3. The key line is `Press D three times, then J or ↓`
    }

    @Test
    fun `a row asks only for up and down select keys`() = webTest {
        // TODO: 1. Play steps 1 and 2
        // TODO: 2. Step 3's row is the third; the selection is on the first
        // TODO: 3. The key line is `Press S twice, then L or →`
    }

    @Test
    fun `the left hand's lines`() = webTest {
        // TODO: 1. A visit with the left hand remembered
        // TODO: 2. Step 1's key line is `Press A`: the left hand shifts a row with A and D
        // TODO: 3. Step 2's key line starts with the left hand's select keys
    }

    @Test
    fun `OK in Controls changes the key line`() = webTest {
        // TODO: 1. Open Controls during step 1
        // TODO: 2. Mark Left-handed: the key line is unchanged
        // TODO: 3. Press OK: the key line names the left hand's keys
    }

    @Test
    fun `Cancel in Controls keeps the key line`() = webTest {
        // TODO: 1. Open Controls and mark Left-handed
        // TODO: 2. Cancel: the key line is unchanged
    }

    @Test
    fun `a replay's step 1 starts with select keys when the selection is elsewhere`() = webTest {
        // TODO: 1. Finish the tutorial with the selection on another row
        // TODO: 2. Replay it with [ Tutorial ]
        // TODO: 3. Step 1's key line starts with the up and down select keys
    }

    @Test
    fun `the lines stay under a blocked message`() = webTest {
        // TODO: 1. Shift another row on step 1
        // TODO: 2. The blocked message shows
        // TODO: 3. The three lines are still there
    }

    @Test
    fun `the lines stay under Once more, naming the way begun`() = webTest {
        // TODO: 1. Play steps 1 and 2
        // TODO: 2. Shift step 3's row one tile by arrow
        // TODO: 3. `Once more!` shows
        // TODO: 4. The click line is the one for the arrow already used
    }

    @Test
    fun `a phone shows no lines`() = webTest {
        // TODO: 1. First launch on touch
        // TODO: 2. Step 1's text is today's drag-only text
        // TODO: 3. There are no lines
    }

    @Test
    fun `the Key hints are unchanged through the tutorial`() = webTest {
        // TODO: 1. Play steps 1 to 4, checking the hints at each step
        // TODO: 2. The hints equal the board's hints for the hand
    }
}
