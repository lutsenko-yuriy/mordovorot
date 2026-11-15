package web

import kotlin.test.Test
import kotlin.test.assertTrue
import viewmodel.TileSelection

/** GH-287: only the outlines follow `selectionShown`, so a change to it alone keeps the board's element (VoiceOver, GH-171). */
class WebStateSelectionTest {

    private val shown = WebState(side = 3, tiles = (1..9).toList(), moves = 4, mordie = null, solved = false, status = null, dialog = null, computer = true, selection = TileSelection(1, 1), selectionShown = true)

    @Test
    fun `hiding the outlines keeps the board`() {
        assertTrue(shown.copy(selectionShown = false).keepsBoardFrom(shown))
    }
}
