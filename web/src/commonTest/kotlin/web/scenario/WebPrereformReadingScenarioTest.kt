package web.scenario

import kotlin.test.Test

/** GH-354: reading the pre-reform labels aloud, their alphabet hint, the tutorial and the events. Stubs: WU1 fills them in. */
class WebPrereformReadingScenarioTest {
    @Test
    fun `read board spells the new letters on 4x4`() {
        // TODO: 1. Choose the letters on 4x4
        // TODO: 2. Read board
        // TODO: 3. Row 3 says `Иван, Иван краткий, и десятеричное, Константин`
    }

    @Test
    fun `read board spells Ѣ Ѳ Ѵ on 6x6`() {
        // TODO: 1. Choose the letters on 6x6
        // TODO: 2. Read board
        // TODO: 3. `ять`, `фита` and `ижица` are said
    }

    @Test
    fun `a german page says the new letters the same`() {
        // TODO: 1. Switch to German and choose the letters
        // TODO: 2. Read board says the same words
    }

    @Test
    fun `a russian page says the new letters the same`() {
        // TODO: 1. Switch to Russian and choose the letters
        // TODO: 2. Read board says the same words
    }

    @Test
    fun `read board spells a pair`() {
        // TODO: 1. Choose Pairs
        // TODO: 2. Read board says `АБ` as `Анна Борис`
    }

    @Test
    fun `the hint is titled for the prereform alphabet`() {
        // TODO: 1. Choose the letters and press Hint
        // TODO: 2. The title reads `Pre-reform Russian alphabet`
    }

    @Test
    fun `the hint is titled in German and Russian`() {
        // TODO: 1. On a German page the title reads `Vorreformalphabet`
        // TODO: 2. On a Russian page it reads `Дореформенный алфавит`
    }

    @Test
    fun `the hint shows 36 letters, 16 in use on 4x4`() {
        // TODO: 1. Open the hint on 4x4
        // TODO: 2. It lists 36 letters
        // TODO: 3. 16 of them are outlined
    }

    @Test
    fun `on 6x6 all 36 letters are in use`() {
        // TODO: 1. Open the hint on 6x6
        // TODO: 2. All 36 letters are outlined
    }

    @Test
    fun `for pairs on 9x9 nine letters are in use`() {
        // TODO: 1. Choose Pairs on 9x9 and open the hint
        // TODO: 2. Nine letters are outlined
    }

    @Test
    fun `the hint is dimmed on 7x7 with prereform letters`() {
        // TODO: 1. Start a 7x7 game with the letters preference
        // TODO: 2. Hint is dimmed
    }

    @Test
    fun `a greyed Ѳ is said not on this board`() {
        // TODO: 1. Open the hint on 4x4
        // TODO: 2. Reading the `Ѳ` says `фита, not on this board`
    }

    @Test
    fun `tutorial step 2 names its tile М`() {
        // TODO: 1. Choose the letters and replay the tutorial
        // TODO: 2. Step 2 names its tile `М`
    }

    @Test
    fun `the tutorial's labels step opens the prereform group`() {
        // TODO: 1. In the tutorial press Labels
        // TODO: 2. The dialog opens with the prereform group open
    }

    @Test
    fun `choosing prereform letters sends tile_labels_changed`() {
        // TODO: 1. Choose the letters and press OK
        // TODO: 2. The `tile_labels_changed` event is sent with `to` set to `prereform_letters`
    }

    @Test
    fun `the 7x7 fallback is reported as letters_unavailable`() {
        // TODO: 1. Start a 7x7 game with the letters preference
        // TODO: 2. The `letters_unavailable` event is sent
    }
}
