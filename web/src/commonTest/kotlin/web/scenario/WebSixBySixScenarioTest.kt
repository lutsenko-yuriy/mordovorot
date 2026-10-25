package web.scenario

import kotlin.test.Test

/** GH-160 scenarios: 6x6 boards in the web client, through [WebHarness]. Stubs from
 *  draft-scenarios; implement fills them in. */
class WebSixBySixScenarioTest {

    @Test
    fun `New offers 6x6 and starts a 6x6 board`() = webTest {
        // TODO: open New - assert the size picker offers 3 to 6
        // TODO: pick 6 - assert the board has 36 tiles labelled 1-36
        // TODO: assert new_game_size_selected {size: 6}
    }

    @Test
    fun `Mordie sits 6x6 out, and a win is Congratulations`() = webTest {
        // TODO: start a 6x6 game - assert Mordie's text is the sit-out line, no search started, Give Up dimmed
        // TODO: shift 5 times - assert the text is gone
        // TODO: load a 6x6 board one shift from solved and solve it - assert `Congratulations ✓` and no mordie_result
    }

    @Test
    fun `a Letters preference shows Numbers on 6x6, with Letters unavailable in the dialog`() = webTest {
        // TODO: set the preference to Letters, start a 6x6 game - assert the tiles show numbers
        // TODO: open Labels - assert the Letters option is unavailable with the sample `up to 5x5`
        // TODO: select Letters - assert nothing changes
        // TODO: reload into a 4x4 game - assert letters come back
    }

    @Test
    fun `a 6x6 save with Letters asks for labels - OK and Cancel`() = webTest {
        // TODO: store a 6x6 save with Letters labels and load it
        // TODO: assert the Labels dialog opens with the warning message, Letters unavailable, screen_labels_dialog {opened_from: letters_save}
        // TODO: pick Pairs and OK - assert the tiles show pairs
        // TODO: reload, load the save again and Cancel - assert the tiles show numbers and dialog_cancelled {dialog: labels}
    }
}
