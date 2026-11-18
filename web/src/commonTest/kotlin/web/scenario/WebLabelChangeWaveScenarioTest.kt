package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.TileLabels
import web.WebDialog
import web.WebState
import web.labelWaveFrom
import viewmodel.UnsolvableSaveChoice

/** GH-343: the label wave on every label change outside the Tile labels dialog, and the arriving half alone on a page load or another board size. Each scenario reads the frames the page drew. */
class WebLabelChangeWaveScenarioTest {

    private val LINKED_4X4 = intArrayOf(12, 1, 2, 3, 0, 5, 6, 7, 4, 9, 10, 11, 8, 13, 14, 15)

    /** The frames from index [from] on that start a board wave, each with its old texts. */
    private fun WebHarness.waveFrames(from: Int = 0): List<Pair<WebState, List<String?>>> =
        frames.drop(maxOf(from - 1, 0)).zipWithNext { before, now -> now.labelWaveFrom(before)?.let { now to it } }.filterNotNull()

    private fun WebHarness.waves(from: Int = 0) = waveFrames(from).map { it.second }

    private fun WebHarness.choose(labels: TileLabels) {
        game.labels.open()
        game.labels.onSelected(labels)
        game.labels.onAnswered(ok = true)
    }

    /** Loads a 4x4 save with [labels] into a running game. */
    private fun WebHarness.load(labels: String?, name: String = "p") {
        saves.save(name, rotatedRow(4), 4, 0, labels = labels)
        game.onLoad()
        game.onLoadChosen(name)
    }

    @Test
    fun aSharePromptOpeningWavesFromTheOldLabels() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val mark = page.frames.size

        page.changeLink(4, LINKED_4X4, "pairs")

        assertEquals(WebDialog.SharedLabels(TileLabels.PAIRS), page.state.dialog)
        val wave = page.waves(mark).single()
        assertEquals(16, wave.size)
        assertTrue(wave.all { it?.toIntOrNull() != null })
        assertEquals(TileLabels.PAIRS, page.state.labels)
    }

    @Test
    fun noWavesTheBoardBackToThePlayersLabels() = webTest {
        for (escape in listOf(false, true)) {
            val page = WebHarness(backgroundScope).startOn()
            page.changeLink(4, LINKED_4X4, "pairs")
            val shown = page.state.tileTexts
            val mark = page.frames.size

            if (escape) page.game.onEscape() else page.game.share.onSharedLabelsAnswered(false)

            assertEquals(listOf(shown), page.waves(mark), "escape $escape")
            assertEquals(TileLabels.NUMBERS, page.state.labels)
        }
    }

    @Test
    fun noAtStartupWavesBackFromTheSendersLabels() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, LINKED_4X4, "pairs")
        val shown = page.state.tileTexts
        val mark = page.frames.size

        page.game.share.onSharedLabelsAnswered(false)

        assertEquals(listOf(shown), page.waves(mark))
        assertEquals(TileLabels.NUMBERS, page.state.labels)
    }

    @Test
    fun aFittingLabelsDialogAtStartupWavesOnAPick() = webTest {
        val page = WebHarness(backgroundScope)
        page.saves.save("six", rotatedRow(6), 6, 0, labels = "letters")
        page.start()
        page.game.onLoadChosen("six")
        assertIs<WebDialog.Labels>(page.state.dialog)
        val shown = page.state.tileTexts
        val mark = page.frames.size

        page.game.labels.onSelected(TileLabels.CYRILLIC_LETTER_PAIRS)

        assertEquals(listOf(shown), page.waves(mark))
    }

    @Test
    fun yesStartsNoWave() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.changeLink(4, LINKED_4X4, "pairs")
        val mark = page.frames.size

        page.game.share.onSharedLabelsAnswered(true)

        assertTrue(page.waves(mark).isEmpty())
        assertEquals(TileLabels.PAIRS, page.state.labels)
        assertNull(page.preferences.get("tile_labels"))
    }

    @Test
    fun aLinkWithTheLabelsAlreadyShownStartsNoWave() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.PAIRS)
        val mark = page.frames.size

        page.changeLink(4, LINKED_4X4, "pairs")

        assertNull(page.state.dialog)
        assertTrue(page.waves(mark).isEmpty())
    }

    @Test
    fun aLinkAtStartupArrivesOnly() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, LINKED_4X4, "pairs")

        val wave = page.waves().single()
        assertEquals(16, wave.size)
        assertTrue(wave.all { it == null })
        assertEquals(TileLabels.PAIRS, page.state.labels)
    }

    @Test
    fun aLinkToAnotherSizeArrivesOnly() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val mark = page.frames.size

        page.changeLink(5, rotatedRow(5), "pairs")

        val wave = page.waves(mark).single()
        assertEquals(25, wave.size)
        assertTrue(wave.all { it == null })
    }

    @Test
    fun aNewGameBackToThePreferenceWaves() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.load("letters")
        assertEquals(TileLabels.LETTERS, page.state.labels)
        val letters = page.state.tileTexts
        val mark = page.frames.size

        page.game.onNew()
        page.game.onSizeChosen(4)

        assertEquals(listOf(letters), page.waves(mark))
        assertEquals(TileLabels.NUMBERS, page.state.labels)
    }

    @Test
    fun aNewGameInTheSameSchemeDoesNotWave() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val mark = page.frames.size

        page.game.onNew()
        page.game.onSizeChosen(4)

        assertTrue(page.waves(mark).isEmpty())
    }

    @Test
    fun aNewGameOfAnotherSizeArrivesOnly() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.LETTERS)
        val mark = page.frames.size

        page.game.onNew()
        page.game.onSizeChosen(6)

        assertEquals(TileLabels.NUMBERS, page.state.labels)
        val wave = page.waves(mark).single()
        assertEquals(36, wave.size)
        assertTrue(wave.all { it == null })
    }

    @Test
    fun aToolbarLoadWithOtherLabelsWaves() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val numbers = page.state.tileTexts
        val mark = page.frames.size

        page.load("letters")

        assertEquals(listOf(numbers), page.waves(mark))
        assertEquals(TileLabels.LETTERS, page.state.labels)
    }

    @Test
    fun aToolbarLoadWithTheSameLabelsDoesNotWave() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val mark = page.frames.size

        page.load(null)

        assertTrue(page.waves(mark).isEmpty())
    }

    @Test
    fun repairAndLoadWavesLikeALoad() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("odd", UNSOLVABLE_3X3, 3, 0, labels = "pairs")
        val mark = page.frames.size

        page.game.onLoad()
        page.game.onLoadChosen("odd")
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR)

        assertEquals(TileLabels.PAIRS, page.state.labels)
        val waves = page.waves(mark)
        assertEquals(1, waves.size)
        assertTrue(waves.single().all { it == null })
        assertEquals(9, waves.single().size)
    }

    @Test
    fun aSaveRestoredAtStartupArrivesOnly() = webTest {
        val page = WebHarness(backgroundScope)
        page.saves.save("p", rotatedRow(4), 4, 0, labels = "letters")
        page.start()
        assertIs<WebDialog.Load>(page.state.dialog)

        page.game.onLoadChosen("p")

        val wave = page.waves().single()
        assertTrue(wave.all { it == null })
        assertEquals(TileLabels.LETTERS, page.state.labels)
    }

    @Test
    fun aPlainStartupStartsNoWave() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        assertTrue(page.waves().isEmpty())
    }

    @Test
    fun theTutorialReplayWithTheSameLabelsStartsNoWave() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.choose(TileLabels.LETTERS)
        val mark = page.frames.size

        page.game.tutorial.onReplay("button")

        assertTrue(page.waves(mark).isEmpty())
    }

    @Test
    fun skipThenRestoreWavesFromTheBoardItLeaves() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).start()
        // Saved in another tab since the page opened.
        page.saves.save("p", rotatedRow(4), 4, 0, labels = "letters")
        page.game.tutorial.onSkip()
        assertIs<WebDialog.Load>(page.state.dialog)
        val mark = page.frames.size

        page.game.onLoadChosen("p")

        // The page is open, so this is no page load: the labels leave as well as arrive.
        val (frame, _) = page.waveFrames(mark).single()
        assertEquals(false, frame.opening)
        assertEquals(TileLabels.LETTERS, page.state.labels)
    }

    @Test
    fun analyticsSendNothingNew() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.changeLink(4, LINKED_4X4, "pairs")
        page.game.share.onSharedLabelsAnswered(false)
        page.load("letters")
        page.game.onNew()
        page.game.onSizeChosen(4)

        assertTrue(page.analytics.events.none { "wave" in it.name || "animat" in it.name })
    }
}
