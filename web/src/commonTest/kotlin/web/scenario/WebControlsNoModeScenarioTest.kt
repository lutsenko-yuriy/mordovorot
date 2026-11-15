package web.scenario

import kotlin.test.Test

/** GH-287 (WU1) scenario stubs: the Controls dialog without the Mouse/Keyboard choice. WU1 moves them into
 *  [WebControlsScenarioTest], which loses its mode scenarios. */
class WebControlsNoModeScenarioTest {

    @Test
    fun `Controls opens on the layout, with no mode choice`() = webTest {
        // TODO: 1. Start on a computer.
        // TODO: 2. Press 6.
        // TODO: 3. The Controls dialog shows.
        // TODO: 4. It has no `Play with` choice.
        // TODO: 5. Focus is on the marked layout.
    }

    @Test
    fun `OK in Controls saves the layout and returns to the opener`() = webTest {
        // TODO: 1. Open Controls with the toolbar button.
        // TODO: 2. Mark Left-handed.
        // TODO: 3. Press OK.
        // TODO: 4. The dialog closes.
        // TODO: 5. The left-handed layout is stored.
        // TODO: 6. Focus is back on `[ Controls ]`.
    }

    @Test
    fun `OK after pressing 6 puts focus on the board`() = webTest {
        // TODO: 1. Press 6.
        // TODO: 2. Mark Left-handed.
        // TODO: 3. Press OK.
        // TODO: 4. Focus is on the board.
    }

    @Test
    fun `a first visit is right-handed`() = webTest {
        // TODO: 1. Start with nothing stored.
        // TODO: 2. The layout is Right-handed.
    }

    @Test
    fun `a stored keyboard mode is ignored and the layout stays`() = webTest {
        // TODO: 1. Store `input_mode=keyboard` and the left-handed layout.
        // TODO: 2. Start on a computer.
        // TODO: 3. The layout is Left-handed.
        // TODO: 4. Nothing reads or changes the mode.
    }

    @Test
    fun `app_launched has no mode`() = webTest {
        // TODO: 1. Start on a computer.
        // TODO: 2. The `app_launched` event is sent.
        // TODO: 3. It has no `mode` property.
    }

    @Test
    fun `no input_mode_switched event after OK in Controls`() = webTest {
        // TODO: 1. Open Controls and mark the other layout.
        // TODO: 2. Press OK.
        // TODO: 3. No `input_mode_switched` event is sent.
    }
}
