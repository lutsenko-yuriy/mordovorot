package web

import viewmodel.GameViewModel
import viewmodel.KeyboardLayout
import viewmodel.SessionKeyboardLayout

/** The Controls dialog's flow (GH-171): the keyboard layout (GH-287: there is no mode to pick). */
internal class WebControlsDialog(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val modal: WebModal,
    private val keyboardLayout: SessionKeyboardLayout,
) {

    val layout get() = keyboardLayout.current

    /** `[ Controls 6 ]`, or 6 with [openedFrom] `shortcut`. Focus goes back to [opener]. */
    fun open(openedFrom: String = "toolbar", opener: Focus = Focus.ControlsButton) = game.runFlow(opener) {
        viewModel.controlsDialogOpened(openedFrom)
        val chosen = modal.ask<WebDialog.Controls?>(dialog(layout))
        if (chosen == null) game.cancelled("controls") else keyboardLayout.choose(chosen.layout)
    }

    /** A `Keyboard keys` option pressed: it becomes the marked one, and the key list follows. */
    fun onLayout(layout: KeyboardLayout) {
        modal.shown as? WebDialog.Controls ?: return
        modal.update(dialog(layout))
        game.render(focus = Focus.LayoutOption(layout))
    }

    /** The dialog's answer: OK with its mark, or Cancel. */
    fun onAnswered(ok: Boolean) {
        val shown = modal.shown as? WebDialog.Controls ?: return
        modal.answer<WebDialog.Controls, WebDialog.Controls?>(if (ok) shown else null)
    }

    private fun dialog(layout: KeyboardLayout) = WebDialog.Controls(layout, KeyHints.keyList(layout))
}
