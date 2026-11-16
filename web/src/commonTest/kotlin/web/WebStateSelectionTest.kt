package web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import viewmodel.TileSelection
import web.drag.Axis

/** GH-287: only the outlines follow `selectionShown`, so a change to it alone keeps the board's element (VoiceOver, GH-171). */
class WebStateSelectionTest {

    private val shown = WebState(side = 3, tiles = (1..9).toList(), moves = 4, mordie = null, solved = false, status = null, dialog = null, computer = true, selection = TileSelection(1, 1), selectionShown = true)

    @Test
    fun `hiding the outlines keeps the board`() {
        assertTrue(shown.copy(selectionShown = false).keepsBoardFrom(shown))
    }

    private val guided = shown.copy(guidance = Guidance(1, "Hi", controls = true, highlight = TutorialHighlight(Axis.ROW, 0, Arrow.LEFT)))

    /** GH-288: the tutorial's highlight is built with the board, so it is rebuilt when the panel comes or goes, and kept from one step to the next. */
    @Test
    fun `the panel coming or going rebuilds the board, a step change keeps it`() {
        assertFalse(guided.keepsBoardFrom(shown))
        assertFalse(shown.keepsBoardFrom(guided))
        assertTrue(guided.copy(guidance = Guidance(2, "Up", controls = true, highlight = TutorialHighlight(Axis.COLUMN, 1, Arrow.UP))).keepsBoardFrom(guided))
    }
}
