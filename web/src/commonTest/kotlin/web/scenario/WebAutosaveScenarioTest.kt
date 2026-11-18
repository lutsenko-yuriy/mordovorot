package web.scenario

import kotlin.test.Test

/** GH-338: leaving the page autosaves to `Autosave_<scheme>_<size>`; every scenario is made green in WU1. */
class WebAutosaveScenarioTest {
    @Test
    fun `leaving after a shift writes the autosave slot`() = webTest {
        // TODO: 1. Make one shift on a 4x4 board
        // TODO: 2. Leave the page
        // TODO: 3. `Autosave_numbers_4x4` exists in the store
        // TODO: 4. Leaving reports no prompt
        // TODO: 5. The progress is no longer unsaved
    }

    @Test
    fun `each label scheme and size has its own slot`() = webTest {
        // TODO: 1. Pick Pairs on a 5x5 board and shift
        // TODO: 2. Leave: `Autosave_pairs_5x5` exists
        // TODO: 3. Pick Cyrillic letter pairs on a 9x9 board and shift
        // TODO: 4. Leave: `Autosave_cyrillic_letter_pairs_9x9` exists
        // TODO: 5. The first slot is untouched
    }

    @Test
    fun `an autosave replaces its slot silently`() = webTest {
        // TODO: 1. Shift once and leave
        // TODO: 2. Shift again and leave
        // TODO: 3. The slot holds the second board
        // TODO: 4. No dialog opened
        // TODO: 5. No status message and nothing announced
    }

    @Test
    fun `an autosave leaves the current save name alone`() = webTest {
        // TODO: 1. Save the game as `mine`
        // TODO: 2. Shift and leave
        // TODO: 3. Open Save: the prefill is still `mine`
    }

    @Test
    fun `leaving twice writes once`() = webTest {
        // TODO: 1. Shift
        // TODO: 2. Leave
        // TODO: 3. Leave again
        // TODO: 4. One write happened
        // TODO: 5. One `autosave_written` event is sent
    }

    @Test
    fun `playing on after loading an autosave rewrites the same slot`() = webTest {
        // TODO: 1. Shift and leave
        // TODO: 2. Start a new page on the same store and Load the autosave
        // TODO: 3. Shift
        // TODO: 4. Leave
        // TODO: 5. The slot holds the new board
    }

    @Test
    fun `leaving with no moves writes nothing`() = webTest {
        // TODO: 1. Start a game and make no shift
        // TODO: 2. Leave
        // TODO: 3. No autosave slot exists
    }

    @Test
    fun `leaving after Save, Load or New writes nothing`() = webTest {
        // TODO: 1. Shift, Save and leave: no autosave slot exists
        // TODO: 2. Shift, Load a save and leave: no autosave slot exists
        // TODO: 3. Shift, start a New game and leave: no autosave slot exists
    }

    @Test
    fun `leaving the solution screen writes nothing`() = webTest {
        // TODO: 1. Make a shift and Give Up
        // TODO: 2. Leave
        // TODO: 3. No autosave slot exists
    }

    @Test
    fun `leaving a solved board writes nothing and still prompts`() = webTest {
        // TODO: 1. Solve the board
        // TODO: 2. Leave
        // TODO: 3. No autosave slot exists
        // TODO: 4. Leaving reports a prompt
    }

    @Test
    fun `leaving while the tutorial runs writes nothing`() = webTest {
        // TODO: 1. Start the tutorial and make a shift
        // TODO: 2. Leave
        // TODO: 3. No autosave slot exists
        // TODO: 4. Leaving reports no prompt
    }

    @Test
    fun `a full store keeps the prompt and the progress unsaved`() = webTest {
        // TODO: 1. Make the store refuse writes
        // TODO: 2. Shift
        // TODO: 3. Leave
        // TODO: 4. Leaving reports a prompt
        // TODO: 5. The progress is still unsaved
        // TODO: 6. The `autosave_written` event says `error`
    }

    @Test
    fun `the next start offers the autosave in Restore`() = webTest {
        // TODO: 1. Shift and leave
        // TODO: 2. Start a new page on the same store
        // TODO: 3. `Restore a saved game?` lists `Autosave_numbers_4x4 (4x4)`
    }

    @Test
    fun `Load lists the autosave`() = webTest {
        // TODO: 1. Shift and leave
        // TODO: 2. Start a new page on the same store and press Load
        // TODO: 3. The list has the row `Autosave_numbers_4x4 (4x4)`
    }

    @Test
    fun `autosave_written carries its properties`() = webTest {
        // TODO: 1. Shift and leave
        // TODO: 2. The `autosave_written` event is sent with `trigger: page_left`
    }

    @Test
    fun `loading an autosave is marked in load_command_used`() = webTest {
        // TODO: 1. Load an autosave: `autosave` is true
        // TODO: 2. Load a named save: `autosave` is false
    }

    @Test
    fun `restoring an autosave is marked in startup_restore_decision`() = webTest {
        // TODO: 1. Restore an autosave at startup: `restored_autosave` is true
        // TODO: 2. Restore a named save at startup: `restored_autosave` is false
    }
}
