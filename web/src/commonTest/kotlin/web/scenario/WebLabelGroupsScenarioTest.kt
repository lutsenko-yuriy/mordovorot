package web.scenario

import kotlin.test.Test

/** GH-323: the Tile labels dialog's groups and preview strip; every scenario is made green in WU1. */
class WebLabelGroupsScenarioTest {
    // Opening
    @Test
    fun `the dialog opens with the group in use open`() = webTest {
        // TODO: 1. Open the Tile labels dialog
        // TODO: 2. It lists three groups: Numbers, Latin letters, Cyrillic letters
        // TODO: 3. Numbers is open
        // TODO: 4. Focus is on the marked scheme, `label-numbers`
    }

    @Test
    fun `the preview strip shows the marked scheme's first three labels`() = webTest {
        // TODO: 1. Open the dialog
        // TODO: 2. The strip reads `1 2 3`
    }

    @Test
    fun `opening sends one screen view and no group event`() = webTest {
        // TODO: 1. Open the dialog
        // TODO: 2. The `screen_labels_dialog` event is sent once
        // TODO: 3. Open another group
        // TODO: 4. No further event is sent
    }

    @Test
    fun `a Cyrillic letter pairs preference opens the Cyrillic group`() = webTest {
        // TODO: 1. Remember Cyrillic letter pairs
        // TODO: 2. Open the dialog
        // TODO: 3. Cyrillic letters is open, focus on Cyrillic letter pairs
        // TODO: 4. The strip reads `АА АБ АВ`
    }

    // Opening a group
    @Test
    fun `opening a group focuses its first option when nothing there is marked`() = webTest {
        // TODO: 1. Open the dialog
        // TODO: 2. Open Latin letters
        // TODO: 3. Latin letters is open and focus is on Letters
        // TODO: 4. The marked scheme is still Numbers
    }

    @Test
    fun `opening the open group again does nothing`() = webTest {
        // TODO: 1. Open the dialog
        // TODO: 2. Open the Numbers group, which is already open
        // TODO: 3. The dialog is as before
    }

    @Test
    fun `on 6x6 Letters is dimmed and focus lands on Letter pairs`() = webTest {
        // TODO: 1. Start a 6x6 game and open the dialog
        // TODO: 2. Open Latin letters
        // TODO: 3. Focus is on Letter pairs
        // TODO: 4. Letters carries `up to 5x5`
    }

    // The mark and the strip
    @Test
    fun `a mark in a closed group survives OK`() = webTest {
        // TODO: 1. Mark Letter pairs in Latin letters
        // TODO: 2. Open Numbers
        // TODO: 3. The strip still reads `AA AB AC`
        // TODO: 4. Press OK
        // TODO: 5. The board shows Letter pairs
    }

    @Test
    fun `Cancel drops a mark in another group`() = webTest {
        // TODO: 1. Mark Letter pairs in Latin letters
        // TODO: 2. Press Cancel
        // TODO: 3. The board keeps its labels
        // TODO: 4. The `dialog_cancelled` event is sent
    }

    @Test
    fun `the strip follows the mark`() = webTest {
        // TODO: 1. Mark Cyrillic letters: the strip reads `А Б В`
        // TODO: 2. Mark Pairs from 0: the strip reads `00 01 02`
    }

    @Test
    fun `the strip is the same on every board size`() = webTest {
        // TODO: 1. Mark Pairs on 3x3, 4x4 and 9x9
        // TODO: 2. The strip reads `11 12 13` each time
    }

    @Test
    fun `options show names only`() = webTest {
        // TODO: 1. Open the dialog
        // TODO: 2. No option carries a sample
    }

    // Layout and names
    @Test
    fun `each group stacks its schemes in order`() = webTest {
        // TODO: 1. Numbers lists Numbers, Pairs, Numbers from 0, Pairs from 0
        // TODO: 2. Latin letters and Cyrillic letters list the single scheme, then the paired one
    }

    @Test
    fun `German and Russian pages name the groups`() = webTest {
        // TODO: 1. Open the dialog on a German page: Zahlen, Lateinische Buchstaben, Kyrillische Buchstaben
        // TODO: 2. Open it on a Russian page: Числа, Латинские буквы, Кириллические буквы
        // TODO: 3. The scheme names are unchanged
    }

    // Other ways in
    @Test
    fun `a load that asks for other labels opens Numbers`() = webTest {
        // TODO: 1. Seed a hand-made 6x6 Letters save and load it
        // TODO: 2. The dialog opens with its message
        // TODO: 3. Numbers is open and focus is on Numbers
    }

    @Test
    fun `the tutorial's Labels opens the group in use and Cancel goes back`() = webTest {
        // TODO: 1. Start the tutorial and open Labels from the panel
        // TODO: 2. The group in use is open
        // TODO: 3. Press Cancel: focus returns to `tutorial-labels`
    }
}
