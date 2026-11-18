package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import viewmodel.TileLabelGroup
import viewmodel.TileLabels
import web.Focus
import web.WebDialog
import web.labelGroupSlideFrom
import web.labelWaveFrom
import web.previewWaveFrom

/** GH-343: a row of the Tile labels dialog that doesn't fit the board shows its limit in a bubble after a press. */
class WebLabelLimitScenarioTest {

    private val WebHarness.labels get() = state.dialog as WebDialog.Labels

    /** A 6x6 board with the Tile labels dialog open on the group of Letters, which stops at 5x5. */
    private fun WebHarness.onSixBySix(): WebHarness {
        game.onNew()
        game.onSizeChosen(6)
        game.labels.open()
        game.labels.onGroupOpened(TileLabelGroup.of(TileLabels.LETTERS))
        return this
    }

    @Test
    fun pressingARowThatDoesNotFitSetsTheTip() = webTest {
        val page = WebHarness(backgroundScope).startOn().onSixBySix()
        assertNull(page.labels.tip)

        page.game.labels.onSelected(TileLabels.LETTERS)

        assertEquals(TileLabels.LETTERS, page.labels.tip)
    }

    @Test
    fun pressingItPicksNothing() = webTest {
        val page = WebHarness(backgroundScope).startOn().onSixBySix()
        val marked = page.labels.selected
        val boardLabels = page.state.labels
        val events = page.analytics.events.size

        page.game.labels.onSelected(TileLabels.LETTERS)

        assertEquals(marked, page.labels.selected)
        assertEquals(boardLabels, page.state.labels)
        assertEquals(events, page.analytics.events.size)
        page.game.labels.onAnswered(ok = true)
        assertEquals(boardLabels, page.state.labels)
        assertNull(page.preferences.get("tile_labels"))
        assertEquals(0, page.props("tile_labels_changed").size)
    }

    @Test
    fun focusGoesToThatRow() = webTest {
        val page = WebHarness(backgroundScope).startOn().onSixBySix()

        page.game.labels.onSelected(TileLabels.LETTERS)

        assertEquals(Focus.Label(TileLabels.LETTERS), page.state.focus)
    }

    @Test
    fun pressingARowThatFitsClearsTheTip() = webTest {
        val page = WebHarness(backgroundScope).startOn().onSixBySix()
        val fitting = TileLabelGroup.of(TileLabels.LETTERS).schemes.first { it.fits(6) }
        page.game.labels.onSelected(TileLabels.LETTERS)

        page.game.labels.onSelected(fitting)

        assertNull(page.labels.tip)
        assertEquals(fitting, page.labels.selected)
    }

    @Test
    fun openingAnotherGroupClearsTheTip() = webTest {
        val page = WebHarness(backgroundScope).startOn().onSixBySix()
        page.game.labels.onSelected(TileLabels.LETTERS)

        page.game.labels.onGroupOpened(TileLabelGroup.NUMBERS)

        assertNull(page.labels.tip)
    }

    @Test
    fun closingTheDialogDropsTheTip() = webTest {
        val page = WebHarness(backgroundScope).startOn().onSixBySix()
        page.game.labels.onSelected(TileLabels.LETTERS)
        page.game.labels.onAnswered(ok = false)
        page.game.labels.open()
        assertNull(page.labels.tip)

        page.game.labels.onGroupOpened(TileLabelGroup.of(TileLabels.LETTERS))
        page.game.labels.onSelected(TileLabels.LETTERS)
        page.game.labels.onAnswered(ok = true)
        page.game.labels.open()

        assertNull(page.labels.tip)
    }

    @Test
    fun theTipStartsNoWaveOrSlide() = webTest {
        val page = WebHarness(backgroundScope).startOn().onSixBySix()
        val before = page.state

        page.game.labels.onSelected(TileLabels.LETTERS)

        val now = page.state
        assertNull(now.labelWaveFrom(before), "no board wave")
        assertNull(now.previewWaveFrom(before), "no preview wave")
        assertNull(now.labelGroupSlideFrom(before), "no group slide")
    }
}
