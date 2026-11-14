package web.scenario

import kotlin.test.Test

/** GH-267 scenarios: opening a share link that carries tile labels, through [WebHarness]. Stubs until WU1. */
class WebSharedLabelsScenarioTest {

    @Test
    fun `a link with different labels asks before showing them`() = webTest {
        // TODO: 1. The receiver's labels are Numbers; start from a 4x4 link carrying `pairs`
        // TODO: 2. The shared board has started: Moves 0, status `Started a shared 4x4 board.`
        // TODO: 3. The `Use the sender's labels (Pairs)?` dialog is open, focus on Yes, its message says `For this game only…`
        // TODO: 4. The board shows the Pairs labels behind the dialog
        // TODO: 5. `screen_shared_labels_dialog {board_size, sender_labels: pairs, receiver_labels: numbers}` and `shared_board_opened {…, labels: differs}` are sent
    }

    @Test
    fun `Yes shows the sender's labels for this game only`() = webTest {
        // TODO: 1. Open the labelled link, then press Yes: the dialog closes and the board shows Pairs
        // TODO: 2. `shared_labels_accepted {sender_labels, receiver_labels, board_size}` is sent
        // TODO: 3. The stored `tile_labels` preference is unchanged
        // TODO: 4. A new game shows Numbers again, and so does a reload
    }

    @Test
    fun `No keeps the receiver's own labels`() = webTest {
        // TODO: 1. Open the labelled link, then press No: the board shows Numbers again
        // TODO: 2. `dialog_cancelled {dialog: shared_labels, via: button}` is sent, and no `shared_labels_accepted`
    }

    @Test
    fun `Escape is No`() = webTest {
        // TODO: 1. Open the labelled link, then press Escape: the board shows Numbers again
        // TODO: 2. `dialog_cancelled {dialog: shared_labels, via: escape}` is sent, and no `shared_labels_accepted`
    }

    @Test
    fun `the same labels raise no prompt`() = webTest {
        // TODO: 1. The receiver's preference is Pairs; open a link carrying `pairs`
        // TODO: 2. No dialog opens and no `screen_shared_labels_dialog` is sent
        // TODO: 3. `shared_board_opened` has `labels: same`
    }

    @Test
    fun `a link without labels is as before`() = webTest {
        // TODO: 1. Open a plain link: the board starts, no prompt
        // TODO: 2. `shared_board_opened` has `labels: none`
    }

    @Test
    fun `unreadable labels are ignored silently`() = webTest {
        // TODO: 1. Open links with an unknown name, an empty value, `PAIRS` in upper case, and `letters` on a 6x6 board
        // TODO: 2. Each starts the board with the receiver's labels: no dialog, no error
        // TODO: 3. Each reports `labels: unreadable`
    }

    @Test
    fun `with unsaved moves, Open the shared board comes first`() = webTest {
        // TODO: 1. Shift a few times, then open a labelled link: `Open the shared board?` opens
        // TODO: 2. Yes: the board starts, then the labels prompt follows
        // TODO: 3. A second run: Cancel on the first prompt changes nothing, and neither the labels prompt nor any analytics event appears
    }

    @Test
    fun `a labelled link in an open tab asks the same way`() = webTest {
        // TODO: 1. Start a game, then change the address to a labelled link (`hashchange`)
        // TODO: 2. The board starts and the labels prompt opens
    }

    @Test
    fun `a later Labels OK reports the link as the source`() = webTest {
        // TODO: 1. Press Yes on the prompt, then open Labels, pick Numbers and press OK
        // TODO: 2. `tile_labels_changed {from: pairs, to: numbers, from_source: shared_link}` is sent
    }
}
