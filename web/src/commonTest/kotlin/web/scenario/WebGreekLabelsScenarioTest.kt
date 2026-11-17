package web.scenario

import kotlin.test.Test

/** GH-322: the two Greek tile-label schemes; every scenario is made green in WU1. */
class WebGreekLabelsScenarioTest {
    // The board
    @Test
    fun `greek letters relabel a 4x4 board`() = webTest {
        // TODO: 1. Choose Greek letters on a 4x4 board
        // TODO: 2. The solved first row reads `Α Β Γ Δ`, U+0391 to U+0394
    }

    @Test
    fun `the 25th greek label is the greek question mark`() = webTest {
        // TODO: 1. Start a 5x5 game and choose Greek letters
        // TODO: 2. The tile of the 25th label shows U+037E
        // TODO: 3. It is not the semicolon U+003B
    }

    @Test
    fun `greek letter pairs fit a 9x9 board`() = webTest {
        // TODO: 1. Start a 9x9 game and choose Greek letter pairs
        // TODO: 2. The tiles read `ΑΑ` to `ΙΙ`
    }

    @Test
    fun `greek letters are dimmed on 6x6`() = webTest {
        // TODO: 1. Choose Greek letters, then start a 6x6 game
        // TODO: 2. The board shows Numbers
        // TODO: 3. Open the dialog: the Greek letters option reads `up to 5x5`
    }

    @Test
    fun `the greek preference survives a 6x6 game`() = webTest {
        // TODO: 1. Choose Greek letters, then play a 6x6 game
        // TODO: 2. Start a 5x5 game
        // TODO: 3. The board shows Greek letters again
    }

    // The dialog
    @Test
    fun `the dialog lists Greek letters fifth`() = webTest {
        // TODO: 1. Open the dialog
        // TODO: 2. The groups are Numbers, Numbers from 0, Latin letters, Cyrillic letters, Greek letters
        // TODO: 3. Open Greek letters: its rows read `Letters` and `Pairs`
        // TODO: 4. Its header's focus key is `label-group-greek`
    }

    @Test
    fun `a greek preference opens the greek group`() = webTest {
        // TODO: 1. Remember Greek letters
        // TODO: 2. Open the dialog
        // TODO: 3. Greek letters is open and focus is on `label-greek_letters`
    }

    @Test
    fun `the preview strip shows greek labels`() = webTest {
        // TODO: 1. Mark Greek letters: the strip reads `Α Β Γ`
        // TODO: 2. Mark Greek letter pairs: the strip reads `ΑΑ ΑΒ ΑΓ`
    }

    // Saves and links
    @Test
    fun `a save keeps greek letter pairs`() = webTest {
        // TODO: 1. Choose Greek letter pairs and save the game
        // TODO: 2. Choose Pairs
        // TODO: 3. Load the save
        // TODO: 4. The board shows Greek letter pairs again
    }

    @Test
    fun `a 5x5 save keeps the greek question mark`() = webTest {
        // TODO: 1. Start a 5x5 game, choose Greek letters and save
        // TODO: 2. Load the save
        // TODO: 3. The tile of the 25th label still shows U+037E
    }

    @Test
    fun `an old 6x6 save with greek letters opens the dialog with the greek message`() = webTest {
        // TODO: 1. Seed a hand-made 6x6 save with the labels `greek_letters` and load it
        // TODO: 2. The dialog opens with `Greek letters don't fit a 6x6 board (25 labels, 36 tiles). Please pick other labels.`
        // TODO: 3. The `screen_labels_dialog` event says `letters_save` and current labels `numbers`
    }

    @Test
    fun `a share link carries the greek labels`() = webTest {
        // TODO: 1. Choose Greek letters, open Share and include the labels
        // TODO: 2. The link ends in `l=greek_letters`
        // TODO: 3. Open the link on another page
        // TODO: 4. It asks `Use the sender's labels (Greek letters)?`
    }

    // Reading aloud and texts
    @Test
    fun `read board spells greek labels`() = webTest {
        // TODO: 1. Choose Greek letters and press Read board
        // TODO: 2. The first row is said as `gamma, alpha, beta, ...`
    }

    @Test
    fun `read board says the greek question mark`() = webTest {
        // TODO: 1. Start a 5x5 game, choose Greek letters and press Read board
        // TODO: 2. The 25th tile is said as `Greek question mark`
    }

    @Test
    fun `a german page says the greek question mark in german`() = webTest {
        // TODO: 1. Open a German page and choose Greek letters on 5x5
        // TODO: 2. Read board says `griechisches Fragezeichen`
        // TODO: 3. A Russian page says `греческий вопросительный знак`
    }

    @Test
    fun `a german page names the greek group and rows`() = webTest {
        // TODO: 1. Open the dialog on a German page
        // TODO: 2. The group reads `Griechische Buchstaben`
        // TODO: 3. Its rows read `Buchstaben` and `Paare`
    }

    @Test
    fun `a russian page names the greek group and rows`() = webTest {
        // TODO: 1. Open the dialog on a Russian page
        // TODO: 2. The group reads `Греческие буквы`
        // TODO: 3. Its rows read `Буквы` and `Пары`
    }

    @Test
    fun `the share toggle names greek letter pairs in full`() = webTest {
        // TODO: 1. Choose Greek letter pairs and open Share
        // TODO: 2. The toggle reads `Include my tile labels (Greek letter pairs)`
    }

    // The tutorial
    @Test
    fun `tutorial step 2 names its tile with the greek letter`() = webTest {
        // TODO: 1. Remember Greek letters and start the tutorial
        // TODO: 2. Go to step 2
        // TODO: 3. The text names its tile with `Ξ`
    }

    @Test
    fun `the tutorial's labels step opens the greek group`() = webTest {
        // TODO: 1. Remember Greek letters and start the tutorial
        // TODO: 2. Open Labels from the panel: Greek letters is open
        // TODO: 3. Press Cancel: focus returns to `tutorial-labels`
    }

    // Events
    @Test
    fun `choosing greek labels sends tile_labels_changed`() = webTest {
        // TODO: 1. Choose Greek letters
        // TODO: 2. The `tile_labels_changed` event says `to: greek_letters`
    }

    @Test
    fun `the 6x6 greek fallback is reported as letters_unavailable`() = webTest {
        // TODO: 1. Load a 6x6 save with Greek letters and choose Numbers
        // TODO: 2. The `tile_labels_changed` event says `from_source: letters_unavailable`
    }
}
