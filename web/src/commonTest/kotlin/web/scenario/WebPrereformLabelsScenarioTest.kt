package web.scenario

import kotlin.test.Test

/** GH-354: the pre-reform Russian tile labels on the board, in the dialog, in saves and in links. Stubs: WU1 fills them in. */
class WebPrereformLabelsScenarioTest {
    @Test
    fun `prereform letters relabel a 4x4 board`() {
        // TODO: 1. Start a 4x4 game
        // TODO: 2. Choose Pre-reform Russian letters
        // TODO: 3. The first row reads `А Б В Г`
    }

    @Test
    fun `the 10th and 11th labels are Й and the Cyrillic І`() {
        // TODO: 1. Choose the letters on 4x4
        // TODO: 2. Tile 10 reads `Й`
        // TODO: 3. Tile 11 is U+0406, not Latin `I`
    }

    @Test
    fun `a 6x6 board shows the three old letters`() {
        // TODO: 1. Start a 6x6 game and choose the letters
        // TODO: 2. Tile 31 is `Ѣ`
        // TODO: 3. Tiles 35 and 36 are `Ѳ` and `Ѵ`
    }

    @Test
    fun `prereform letter pairs fit a 9x9 board`() {
        // TODO: 1. Start a 9x9 game and choose Pairs
        // TODO: 2. The first tile reads `АА`
        // TODO: 3. The last tile reads `ИИ`
    }

    @Test
    fun `prereform letters are dimmed on 7x7`() {
        // TODO: 1. Start a 7x7 game and open the dialog
        // TODO: 2. The Letters row is dimmed
        // TODO: 3. Pressing it shows `up to 6x6`
    }

    @Test
    fun `the prereform preference survives a 7x7 game`() {
        // TODO: 1. Choose the letters
        // TODO: 2. A 7x7 game shows Numbers
        // TODO: 3. A new 4x4 game shows the letters again
    }

    @Test
    fun `the dialog lists the prereform group sixth`() {
        // TODO: 1. Open the dialog
        // TODO: 2. There are six groups, the new one last
        // TODO: 3. Its rows read `Letters` and `Pairs`
        // TODO: 4. Its header key is `label-group-prereform`
    }

    @Test
    fun `a prereform preference opens the prereform group`() {
        // TODO: 1. Choose the letters, then reopen the dialog
        // TODO: 2. The group is open
        // TODO: 3. Focus is on its marked row
    }

    @Test
    fun `the preview strip shows prereform labels`() {
        // TODO: 1. Choose Letters: the strip reads `А Б В`
        // TODO: 2. Choose Pairs: the strip reads `АА АБ АВ`
    }

    @Test
    fun `a save keeps prereform letter pairs`() {
        // TODO: 1. Choose Pairs on 5x5, then save
        // TODO: 2. Load it in a new session
        // TODO: 3. The labels are still Pairs
    }

    @Test
    fun `a 6x6 save keeps Ѳ and Ѵ`() {
        // TODO: 1. Choose the letters on 6x6, then save
        // TODO: 2. Load it
        // TODO: 3. Tiles 35 and 36 still read `Ѳ` and `Ѵ`
    }

    @Test
    fun `a 7x7 save with prereform letters opens the dialog with its own message`() {
        // TODO: 1. Make a 7x7 save by hand with the letters
        // TODO: 2. Load it
        // TODO: 3. The Tile labels dialog opens with `Pre-reform Russian letters don't fit a 7x7 board (36 labels, 49 tiles). Please pick other labels.`
        // TODO: 4. Numbers are marked
        // TODO: 5. The `screen_labels_dialog` event says `letters_save`
    }

    @Test
    fun `a share link carries the prereform labels`() {
        // TODO: 1. Share with labels on
        // TODO: 2. The link has `l=prereform_letters`
        // TODO: 3. Opening it asks `Use the sender's labels (Pre-reform Russian letters)?`
    }

    @Test
    fun `a 7x7 link with prereform letters opens as a plain board`() {
        // TODO: 1. Open a 7x7 link with `l=prereform_letters`
        // TODO: 2. It opens with Numbers and no labels prompt
    }

    @Test
    fun `the autosave slot is named for the scheme`() {
        // TODO: 1. Choose the letters on 6x6
        // TODO: 2. Leave the page
        // TODO: 3. The slot is `Autosave_prereform_letters_6x6`
    }

    @Test
    fun `a german page names the prereform group and rows`() {
        // TODO: 1. Switch to German and open the dialog
        // TODO: 2. The group reads `Russisch vor 1918`
        // TODO: 3. The rows read `Buchstaben` and `Paare`
    }

    @Test
    fun `a russian page names the prereform group and rows`() {
        // TODO: 1. Switch to Russian and open the dialog
        // TODO: 2. The group reads `Дореформенный алфавит`
        // TODO: 3. The rows read `Буквы` and `Пары`
    }

    @Test
    fun `the share toggle names prereform letter pairs in full`() {
        // TODO: 1. Choose Pairs and open Share
        // TODO: 2. The toggle reads `Include my tile labels (Pre-reform Russian letter pairs)`
    }
}
