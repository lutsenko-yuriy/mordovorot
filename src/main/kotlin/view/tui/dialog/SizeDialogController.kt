package view.tui.dialog

import analytics.AnalyticsService
import board_model.BoardSize
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.UiRequest
import viewmodel.ViewModel

/** The new-game size picker (GH-44, GH-78): the toolbar New / F9 and the viewModel's
 *  [UiRequest.ChooseBoardSize]. */
internal class SizeDialogController(
    private val screen: TuiScreen,
    private val analytics: AnalyticsService,
    private val viewModel: ViewModel,
) {

    /** GH-44 WU3: the `[ New ]` toolbar button - opens the same size picker
     *  [UiRequest.ChooseBoardSize]'s startup handler does, but calls [ViewModel.newGame]
     *  directly instead of returning through `ask` (this isn't answering a raised request). */
    fun onToolbar() {
        runSizeDialog("toolbar")?.let { viewModel.newGame(it, trigger = "toolbar") }
    }

    /** Runs the size-picker dialog's own blocking loop: a button per valid side, plus Cancel -
     *  buttons-only, like the Exit dialog, so no text field or list to manage.
     *  Shared by the startup [UiRequest.ChooseBoardSize] handler and (GH-44 WU3) the toolbar
     *  `[ New ]` button/F9 - [openedFrom] is `"toolbar"` or a [UiRequest.ChooseBoardSize.origin]. Returns the chosen side,
     *  or `null` on Cancel/Escape/EOF - deliberately returns a value instead of calling
     *  `viewModel.newGame` itself, so the startup call site stays outside the deadlock-prone
     *  "View calls a request-raising viewModel method from its own request handler" shape (see
     *  [viewmodel.UiRequestChannel.ask]'s KDoc). */
    fun runSizeDialog(openedFrom: String): Int? {
        analytics.track("screen_size_dialog", mapOf("opened_from" to openedFrom))
        val dialog = Dialog(
            kind = Dialog.Kind.SIZE,
            title = "New game size",
            buttons = (BoardSize.MIN..BoardSize.MAX).map { DialogButtonSpec(it.toString(), "${it}x$it") } +
                DialogButtonSpec("cancel", "Cancel"),
        )
        return screen.runModal<Int?>("size", cancelled = null, dialog = { dialog }) { action ->
            val target = (action as? InputAction.Activate)?.target
            (target as? HitTarget.DialogButton)?.id?.toIntOrNull()?.let { return it }
        }
    }
}
