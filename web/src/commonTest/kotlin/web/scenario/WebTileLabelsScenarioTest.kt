package web.scenario

import kotlin.test.Test

/** GH-139 scenarios: tile labels in the web client. A relaunch is a new [WebHarness] over the same store. */
class WebTileLabelsScenarioTest {

    @Test
    fun `labels button after load opens the dialog`() {
        // TODO: start a fresh page with an empty store
        // TODO: verify the toolbar is `[ New ] [ Save ] [ Load ] [ Labels ]`
        // TODO: press Labels - verify the Tile labels dialog with six schemes, Numbers marked
        // TODO: verify screen_labels_dialog {opened_from: toolbar, screen: play, current_labels: numbers}
    }

    @Test
    fun `choosing letter pairs relabels the tiles`() {
        // TODO: load a known 4x4 arrangement with tile N = 6 at a known cell
        // TODO: open the dialog, choose Letter pairs, OK
        // TODO: verify that tile's text is `BC`
        // TODO: verify tile_labels_changed {from: numbers, to: letter_pairs, from_source: default, screen: play, board_size: 4}
    }

    @Test
    fun `cancel changes nothing`() {
        // TODO: open the dialog, select Letters, Cancel (web dialogs have no Escape, D5)
        // TODO: verify the tiles still show numbers and dialog_cancelled {dialog: labels} fires
        // TODO: open it again and OK on Numbers - verify no tile_labels_changed fires
    }

    @Test
    fun `preference survives a reload`() {
        // TODO: choose Letters
        // TODO: start a new harness over the same store - verify letters and app_launched.tile_labels == letters
    }

    @Test
    fun `a save keeps its labels for that game only`() {
        // TODO: with Letters chosen, save as `foo`; then choose Pairs
        // TODO: load `foo` - verify letters, and load_command_used.labels_differ_from_preference == true
        // TODO: New game - verify pairs
        // TODO: load `foo` again, change to Numbers - verify tile_labels_changed.from_source == save
    }

    @Test
    fun `labels on the solution screen`() {
        // TODO: give up to reach the solution screen; change to Letters there
        // TODO: verify the tiles show letters, the step list is unchanged, and tile_labels_changed.screen == solution
    }
}
