package view.tui

import kotlin.test.Test

/**
 * Scenario stubs for GH-3's board interaction loop: `TuiView` dispatching mouse clicks on
 * the board to the presenter and repainting. Driven via `FakeTerminal` + `FakePresenter` -
 * no real terminal or presenter logic involved. Filled in by `implement` during WU3, once
 * `TuiView` exists.
 */
class TuiViewBoardTest {

    @Test
    fun `clicking a row's left or right arrow calls shiftLeft or shiftRight with the 0-based row`() {
        // TODO: Script FakeTerminal to emit a MouseClick on row 1's left-arrow coordinate
        // TODO: Run TuiView.play() one iteration
        // TODO: Verify FakePresenter recorded shiftLeft(1)
        // TODO: Repeat for a right-arrow click -> shiftRight(row)
    }

    @Test
    fun `clicking a column's up or down arrow calls shiftUp or shiftDown with the 0-based column`() {
        // TODO: Script FakeTerminal to emit a MouseClick on column 2's up-arrow coordinate
        // TODO: Run TuiView.play() one iteration
        // TODO: Verify FakePresenter recorded shiftUp(2)
        // TODO: Repeat for a down-arrow click -> shiftDown(col)
    }

    @Test
    fun `the board repaints after every click`() {
        // TODO: Script two successive arrow clicks
        // TODO: Verify FakeTerminal captured a new frame write after each one
    }

    @Test
    fun `a click on dead space makes no presenter call`() {
        // TODO: Script FakeTerminal to emit a MouseClick inside a tile cell
        // TODO: Run TuiView.play() one iteration
        // TODO: Verify FakePresenter recorded no calls
    }

    @Test
    fun `EndOfInput from the terminal ends the loop cleanly`() {
        // TODO: Script FakeTerminal to emit TerminalEvent.EndOfInput
        // TODO: Run TuiView.play()
        // TODO: Verify it returns normally, without throwing
    }
}
