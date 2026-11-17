package web.scenario

import kotlin.test.Test

/** GH-313: the two Cyrillic tile-label schemes; every scenario is made green in WU2. */
class WebCyrillicLabelsScenarioTest {
    @Test
    fun `cyrillic letters relabel a 4x4 board`() = webTest {
        // TODO: 1. Start a 4x4 game
        // TODO: 2. Choose Cyrillic letters in the Labels dialog
        // TODO: 3. Confirm with OK
        // TODO: 4. The first row reads `А Б В Г`
    }

    @Test
    fun `the labels dialog lists eight rows, the Cyrillic ones last`() = webTest {
        // TODO: 1. Open the Labels dialog
        // TODO: 2. It lists eight rows
        // TODO: 3. The two new rows come after Letter pairs
        // TODO: 4. The `label-cyrillic_letters` focus key exists
    }

    @Test
    fun `Cyrillic letters on 6x6 fall back to numbers`() = webTest {
        // TODO: 1. Remember Cyrillic letters
        // TODO: 2. Start a 6x6 game
        // TODO: 3. Numbers show
        // TODO: 4. The dialog row is dimmed with `up to 5x5`
    }

    @Test
    fun `the Cyrillic preference survives a 6x6 game`() = webTest {
        // TODO: 1. Remember Cyrillic letters
        // TODO: 2. Play a 6x6 game, then start a 5x5 one
        // TODO: 3. The 5x5 board shows Cyrillic letters again
    }

    @Test
    fun `Cyrillic letter pairs fit a 9x9 board`() = webTest {
        // TODO: 1. Choose Cyrillic letter pairs on 9x9
        // TODO: 2. The first row starts `АА`
        // TODO: 3. It ends `АИ`
        // TODO: 4. The last tile of the first column reads `ИА`
    }

    @Test
    fun `a save keeps Cyrillic letter pairs`() = webTest {
        // TODO: 1. Choose Cyrillic letter pairs
        // TODO: 2. Save the game
        // TODO: 3. Start a new game with other labels
        // TODO: 4. Load the save
        // TODO: 5. The Cyrillic pairs show again
    }

    @Test
    fun `an old 6x6 save with Cyrillic letters opens the dialog with the Cyrillic message`() = webTest {
        // TODO: 1. Seed a hand-made 6x6 save with Cyrillic letters
        // TODO: 2. Load it
        // TODO: 3. The dialog shows the Cyrillic message (28 letters, 36 tiles)
        // TODO: 4. The `letters_save` source is sent
    }

    @Test
    fun `a share link carries the Cyrillic labels`() = webTest {
        // TODO: 1. Share a 4x4 board with Cyrillic letters
        // TODO: 2. The link ends with `l=cyrillic_letters`
        // TODO: 3. Open it as a receiver
        // TODO: 4. The prompt reads `Use the sender's labels (Cyrillic letters)?`
    }

    @Test
    fun `Read board spells Cyrillic labels`() = webTest {
        // TODO: 1. Choose Cyrillic letters on an English page
        // TODO: 2. Press Read board
        // TODO: 3. It says `Анна Борис …`
    }

    @Test
    fun `a German page names the Cyrillic rows in German`() = webTest {
        // TODO: 1. Open the page with `lang=de`
        // TODO: 2. Open the Labels dialog
        // TODO: 3. The rows read `Kyrillische Buchstaben` and `Kyrillische Buchstabenpaare`
    }

    @Test
    fun `tutorial step 2 names its tile with the Cyrillic letter`() = webTest {
        // TODO: 1. Choose Cyrillic letters
        // TODO: 2. Open the tutorial at step 2
        // TODO: 3. The tile is named `О`
    }

    @Test
    fun `choosing Cyrillic labels sends tile_labels_changed`() = webTest {
        // TODO: 1. Choose Cyrillic letters
        // TODO: 2. The `tile_labels_changed` event is sent
        // TODO: 3. The event's scheme is `cyrillic_letters`
    }

    @Test
    fun `the 6x6 fallback is reported as letters_unavailable`() = webTest {
        // TODO: 1. Remember Cyrillic letters
        // TODO: 2. Start a 6x6 game
        // TODO: 3. The `letters_unavailable` source is sent
    }
}
