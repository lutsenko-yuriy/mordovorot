package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.TileLabelGroup
import viewmodel.TileLabels
import web.Focus
import web.WebDialog
import web.WebState
import web.labelGroupSlideFrom
import web.labelWaveFrom
import web.previewWaveFrom

/** GH-342: the label wave on the board and the preview strip, and the group slide in the Tile labels dialog. Each scenario reads the frames the page drew. */
class WebLabelWaveScenarioTest {

    /** The two latest frames, the one before and the one after the last action. */
    private fun WebHarness.lastTwo(): Pair<WebState, WebState> = frames.takeLast(2).let { it[0] to it[1] }

    /** Every board wave the frames from index [from] on start, each as the old texts. */
    private fun WebHarness.waves(from: Int = 0) =
        frames.drop(maxOf(from - 1, 0)).zipWithNext { before, now -> now.labelWaveFrom(before) }.filterNotNull()

    private fun WebHarness.slides(from: Int = 0) =
        frames.drop(maxOf(from - 1, 0)).zipWithNext { before, now -> now.labelGroupSlideFrom(before) }.filterNotNull()

    private fun WebHarness.openLabels() = game.labels.open()

    private fun WebHarness.pick(labels: TileLabels) = game.labels.onSelected(labels)

    private fun letters(state: WebState) = state.tiles.map { ('A' + it - 1).toString() }

    // The board wave
    @Test
    fun pickingARowStartsABoardWaveFromTheOldLabels() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()

        page.pick(TileLabels.LETTERS)

        val (before, now) = page.lastTwo()
        assertEquals(before.tileTexts, now.labelWaveFrom(before))
        assertEquals(before.tiles.map { it.toString() }, now.labelWaveFrom(before))
        assertEquals(letters(now), now.tileTexts)
    }

    @Test
    fun pickingARowStartsAPreviewWave() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()

        page.pick(TileLabels.LETTERS)

        val (before, now) = page.lastTwo()
        assertEquals(listOf("1", "2", "3"), now.previewWaveFrom(before))
        assertEquals(listOf("A", "B", "C"), (now.dialog as WebDialog.Labels).preview)
    }

    @Test
    fun unchangedLabelsDoNotWave() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()
        val mark = page.frames.size

        page.pick(TileLabels.NUMBERS)

        assertTrue(page.waves(mark).isEmpty())
        val (before, now) = page.lastTwo()
        assertNull(now.previewWaveFrom(before))
    }

    @Test
    fun rapidPicksEachWaveFromThePreviousTarget() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()

        for (labels in listOf(TileLabels.LETTERS, TileLabels.LETTER_PAIRS, TileLabels.CYRILLIC_LETTERS)) {
            val before = page.state
            page.pick(labels)
            assertEquals(before.tileTexts, page.state.labelWaveFrom(before), labels.name)
        }

        assertEquals(TileLabels.CYRILLIC_LETTERS, page.state.labels)
        assertEquals(page.state.tiles.map { "АБВГДЕЖЗИКЛМНОПР"[it - 1].toString() }, page.state.tileTexts)
    }

    @Test
    fun theBoardWaveTakesTheSameTimeOnEverySize() = webTest {
        for (side in listOf(3, 6)) {
            val page = WebHarness(backgroundScope).startOn()
            page.game.onNew()
            page.game.onSizeChosen(side)
            page.openLabels()

            page.pick(TileLabels.PAIRS)

            val (before, now) = page.lastTwo()
            assertEquals(side * side, now.labelWaveFrom(before)?.size, "side $side")
        }
    }

    // Cancel and OK
    @Test
    fun cancelWavesTheBoardBack() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val numbers = page.state.tileTexts
        page.openLabels()
        page.pick(TileLabels.LETTERS)
        val mark = page.frames.size

        page.game.labels.onAnswered(ok = false)

        assertEquals(listOf(letters(page.state)), page.waves(mark))
        assertEquals(numbers, page.state.tileTexts)
        assertNull(page.state.dialog)
    }

    @Test
    fun escapeWavesTheBoardBack() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val numbers = page.state.tileTexts
        page.openLabels()
        page.pick(TileLabels.LETTERS)
        val mark = page.frames.size

        page.game.onEscape()

        assertEquals(listOf(letters(page.state)), page.waves(mark))
        assertEquals(numbers, page.state.tileTexts)
    }

    @Test
    fun okStartsNoWaveAndNeverShowsTheOldLabels() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()
        page.pick(TileLabels.LETTERS)
        val mark = page.frames.size

        page.game.labels.onAnswered(ok = true)

        assertTrue(page.waves(mark).isEmpty())
        assertNull(page.state.dialog)
        assertTrue(page.frames.drop(mark - 1).all { it.labels == TileLabels.LETTERS })
    }

    // The groups
    @Test
    fun openingAnotherGroupStartsASlideFromTheOpenOne() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()
        val mark = page.frames.size

        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)

        assertEquals(listOf(TileLabelGroup.NUMBERS), page.slides(mark))
        assertTrue(page.waves(mark).isEmpty())
    }

    @Test
    fun pressingTheOpenGroupStartsNoSlide() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()
        val frames = page.frames.size

        page.game.labels.onGroupOpened(TileLabelGroup.NUMBERS)

        assertEquals(frames, page.frames.size)
        assertTrue(page.slides(frames).isEmpty())
    }

    @Test
    fun openingTheDialogAnimatesNothing() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val mark = page.frames.size

        page.openLabels()

        assertTrue(page.waves(mark).isEmpty())
        assertTrue(page.slides(mark).isEmpty())
        val (before, now) = page.lastTwo()
        assertNull(now.previewWaveFrom(before))
    }

    // Interruption
    @Test
    fun aGroupPressDuringAWaveLeavesTheWaveTargetsInPlace() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()
        page.pick(TileLabels.LETTERS)
        val mark = page.frames.size

        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)

        assertEquals(listOf(TileLabelGroup.NUMBERS), page.slides(mark))
        assertTrue(page.waves(mark).isEmpty())
        assertEquals(letters(page.state), page.state.tileTexts)
    }

    // Where it does not play
    @Test
    fun theLoadPickerThatOpensTheSameDialogWaves() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.saves.save("six", rotatedRow(6), 6, 0, labels = "letters")
        page.game.onLoad()
        page.game.onLoadChosen("six")
        assertNotNull(page.state.dialog as? WebDialog.Labels)

        page.pick(TileLabels.CYRILLIC_LETTER_PAIRS)

        val (before, now) = page.lastTwo()
        assertEquals(before.tileTexts, now.labelWaveFrom(before))
    }

    // What does not change
    @Test
    fun analyticsSendsNothingNew() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.openLabels()
        page.pick(TileLabels.LETTERS)
        page.pick(TileLabels.LETTER_PAIRS)
        page.pick(TileLabels.CYRILLIC_LETTERS)
        page.game.labels.onGroupOpened(TileLabelGroup.GREEK)
        page.game.labels.onGroupOpened(TileLabelGroup.LATIN)
        page.game.labels.onAnswered(ok = false)

        assertEquals(listOf("screen_labels_dialog", "dialog_cancelled"), page.analytics.events.map { it.name })
    }

    @Test
    fun focusAndKeysAreUnchanged() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.openLabels()

        page.pick(TileLabels.LETTERS)
        assertEquals(Focus.Label(TileLabels.LETTERS).key, page.state.focus?.key)
        page.game.labels.onGroupOpened(TileLabelGroup.GREEK)
        assertEquals(Focus.Label(TileLabels.GREEK_LETTERS).key, page.state.focus?.key)
        page.game.labels.onAnswered(ok = false)

        assertEquals(Focus.LabelsButton.key, page.state.focus?.key)
    }
}
