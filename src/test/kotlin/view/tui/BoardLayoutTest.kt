package view.tui

import kotlin.test.Test

/**
 * Scenario stubs for GH-3's board geometry: given a terminal size and the board's square
 * side, computing where the 16 shift arrows and 3 toolbar buttons land, and hit-testing a
 * click coordinate against them. Pure geometry, no terminal or presenter involved. Filled
 * in by `implement` during WU3, once `BoardLayout`/`HitTarget` exist.
 */
class BoardLayoutTest {

    @Test
    fun `hit-tests each of the 16 shift arrows to its correct 0-based row or column`() {
        // TODO: Build a BoardLayout for a 4x4 board on a terminal large enough to fit it
        // TODO: For each of the 4 rows, hit-test the left-arrow coordinate -> HitTarget.ShiftLeft(row)
        // TODO: For each of the 4 rows, hit-test the right-arrow coordinate -> HitTarget.ShiftRight(row)
        // TODO: For each of the 4 columns, hit-test the up-arrow coordinate -> HitTarget.ShiftUp(col)
        // TODO: For each of the 4 columns, hit-test the down-arrow coordinate -> HitTarget.ShiftDown(col)
    }

    @Test
    fun `hit-tests each toolbar button`() {
        // TODO: Hit-test the Save button's coordinate -> HitTarget.ToolbarSave
        // TODO: Hit-test the Load button's coordinate -> HitTarget.ToolbarLoad
        // TODO: Hit-test the Exit button's coordinate -> HitTarget.ToolbarExit
    }

    @Test
    fun `a click inside a tile cell returns Nothing`() {
        // TODO: Hit-test a coordinate inside one of the 16 tile cells (not an arrow or toolbar)
        // TODO: Verify HitTarget.Nothing
    }

    @Test
    fun `off-board coordinates return Nothing`() {
        // TODO: Hit-test a coordinate well outside the rendered frame
        // TODO: Verify HitTarget.Nothing
    }

    @Test
    fun `arrow regions return Nothing when arrows are disabled`() {
        // TODO: Build a BoardLayout with arrowsEnabled = false
        // TODO: Hit-test a coordinate that would normally be a shift arrow
        // TODO: Verify HitTarget.Nothing instead of a Shift* target
    }

    @Test
    fun `a terminal too small to fit the board still produces a layout without throwing`() {
        // TODO: Build a BoardLayout for a terminal size smaller than the board needs
        // TODO: Verify layout construction and hitTest calls do not throw
    }
}
