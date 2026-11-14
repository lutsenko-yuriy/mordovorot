package web.scenario

import kotlin.test.Test

/** GH-267 scenarios: the Share dialog's `Include my tile labels` toggle, through [WebHarness]. Stubs until WU2. */
class WebShareLabelsScenarioTest {

    @Test
    fun `the toggle starts off and the link is board-only`() = webTest {
        // TODO: 1. Open Share: the toggle reads `Include my tile labels (Numbers)` and is off
        // TODO: 2. The link is exactly today's, with no `&l=`
        // TODO: 3. `screen_share_dialog` has `include_labels: false`
    }

    @Test
    fun `turning it on adds the labels to the link and QR at once`() = webTest {
        // TODO: 1. The labels are Pairs; open Share and flip the toggle
        // TODO: 2. The link now ends `&l=pairs` and the QR code is regenerated; focus stays on the toggle
        // TODO: 3. `share_labels_toggled {enabled: true, labels: pairs, board_size: 4}` is sent
        // TODO: 4. Flip it again: the link is board-only again
    }

    @Test
    fun `flipping it resets Copied`() = webTest {
        // TODO: 1. Copy the link: the button reads `Copied!`
        // TODO: 2. Flip the toggle: the button reads `Copy link` again
        // TODO: 3. Copy again: the clipboard holds the labelled link, and `board_share_link_copied` has `includes_labels: true`
    }

    @Test
    fun `the choice is remembered`() = webTest {
        // TODO: 1. Flip it on and close the dialog
        // TODO: 2. Reopen Share: still on, with `include_labels: true`
        // TODO: 3. Reload (a new harness over the same store): still on
    }

    @Test
    fun `the labels shown travel, not the preference`() = webTest {
        // TODO: 1. The preference is Letters and the board is 6x6, which shows Numbers
        // TODO: 2. Flip the toggle on: the link carries `&l=numbers`, and the toggle reads `(Numbers)`
    }

    @Test
    fun `a labelled link reaches the receiver end to end`() = webTest {
        // TODO: 1. The sender plays with Pairs and flips the toggle on
        // TODO: 2. A second page, with Numbers, opens the copied link and gets the prompt
        // TODO: 3. Yes: the receiver sees the same board with Pairs
    }

    @Test
    fun `a failing preference write still toggles`() = webTest {
        // TODO: 1. Make the store refuse writes and flip the toggle: the link changes
        // TODO: 2. After a reload the toggle is off again, and no error shows
    }
}
