package view

import presenter.UiRequest

/**
 * Created by yurich on 08.12.16.
 */
interface View {
    fun displayBoard(boardState: IntArray, squareSide: Int)

    /** Still on the interface (unlike the four prompt methods below, which left it in GH-42
     *  WU2) because [presenter.ModeSwitcherImpl] calls it directly, outside any [UiRequest].
     *  The presenter-facing use of `showMessage` goes through [presenter.PresenterImpl.ask]
     *  instead, which raises a [UiRequest.ShowMessage] each implementation's own request
     *  handler answers by calling this same method - see [view.ViewImpl] / [view.tui.TuiView]. */
    suspend fun showMessage(message: String)

    suspend fun processCommand()

    suspend fun play()
}