import kotlin.test.Test

/** GH-139 scenarios: tile labels in the terminal, mouse and keyboard mode. */
class TileLabelsScenarioTest {

    @Test
    fun `labels button opens the dialog with numbers marked`() {
        // TODO: for mouse and keyboard mode: start a fresh 4x4 session with no preference
        // TODO: verify the second row shows `[ Labels ]` (mouse) or `[Labels F4]` (keyboard)
        // TODO: click it (mouse) or press F4 (keyboard)
        // TODO: verify `Tile labels` opens with six rows and their samples, `● Numbers` marked
        // TODO: verify screen_labels_dialog {opened_from: toolbar (mouse) / shortcut (keyboard), screen: play, current_labels: numbers}
    }

    @Test
    fun `choosing pairs relabels the board`() {
        // TODO: for mouse and keyboard mode: load a known 4x4 arrangement with tile N = 6 at a known cell
        // TODO: open the dialog, choose Pairs, OK
        // TODO: verify that cell now reads `23` instead of `7`
        // TODO: verify tile_labels_changed {from: numbers, to: pairs, from_source: default, screen: play, board_size: 4}
    }

    @Test
    fun `cancel and escape change nothing`() {
        // TODO: open the dialog, select Letters, Cancel
        // TODO: verify the board still shows numbers and dialog_cancelled {dialog: labels} fires
        // TODO: repeat with Escape - same result
        // TODO: open it again and OK on Numbers (the current scheme) - verify no tile_labels_changed fires
    }

    @Test
    fun `labels survive a mode switch, a new game and a relaunch`() {
        // TODO: choose Letters
        // TODO: switch input mode - verify the board still shows letters
        // TODO: New game - verify letters
        // TODO: start a new session over the same temp dir - verify letters and app_launched.tile_labels == letters
    }

    @Test
    fun `a save keeps its labels for that game only`() {
        // TODO: with Letters chosen, save as `foo`
        // TODO: choose Pairs
        // TODO: load `foo` - verify letters, and load_command_used.labels_differ_from_preference == true
        // TODO: New game - verify pairs (the preference was untouched)
        // TODO: load `foo` again, change to Numbers in the dialog - verify tile_labels_changed.from_source == save
    }

    @Test
    fun `startup restore and repair keep the save's labels`() {
        // TODO: seed a Letters save; set the preference to Pairs
        // TODO: relaunch, restore it from the startup prompt - verify letters
        // TODO: seed an unsolvable 3x3 save with Letters, Repair & load - verify letters, and the rewritten file still has them
    }

    @Test
    fun `an older save loads with the preference`() {
        // TODO: seed a save in today's format (no labels line)
        // TODO: set the preference to Pairs and load the save
        // TODO: verify pairs, and load_command_used.labels_differ_from_preference == false
    }

    @Test
    fun `labels on the solved board and the solution screen`() {
        // TODO: solve a board with Pairs - verify board_solved.tile_labels == pairs
        // TODO: open the dialog after the win - verify screen_labels_dialog.screen == solved
        // TODO: give up on another game to reach the solution screen; change to Letters there
        // TODO: verify the board shows letters, the step list still reads `Row …`/`Col …`, and tile_labels_changed.screen == solution
    }
}
