package web

import analytics.AnalyticsService
import viewmodel.GameViewModel
import viewmodel.KeyboardLayout
import viewmodel.SessionKeyboardLayout

/** The Controls dialog's flow (GH-171), like the terminal's `ControlsDialogController`, and the
 *  session's [mode]. */
internal class WebControlsDialog(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
    private val keyboardLayout: SessionKeyboardLayout,
    private val preference: PlayModePreference,
    startMode: PlayMode,
) {

    var mode = startMode
        private set

    /** `[ Controls 6 ]`, or 6 with [openedFrom] `shortcut`. Focus goes back to [opener], or to the
     *  board on entering keyboard mode. */
    fun open(openedFrom: String = "toolbar", opener: Focus = Focus.ControlsButton) = game.runFlow(opener) {
        viewModel.controlsDialogOpened(openedFrom)
        val chosen = modal.ask<WebDialog.Controls?>(dialog(mode, keyboardLayout.current))
        if (chosen == null) game.cancelled("controls") else apply(chosen)
    }

    /** A `Play with` option pressed: it becomes the marked one. */
    fun onMode(mode: PlayMode) {
        val shown = modal.shown as? WebDialog.Controls ?: return
        modal.update(dialog(mode, shown.layout))
        game.render(focus = Focus.ModeOption(mode))
    }

    /** A `Keyboard keys` option pressed: it becomes the marked one, and the key list follows. */
    fun onLayout(layout: KeyboardLayout) {
        val shown = modal.shown as? WebDialog.Controls ?: return
        modal.update(dialog(shown.mode, layout))
        game.render(focus = Focus.LayoutOption(layout))
    }

    /** The dialog's answer: OK with its marks, or Cancel. */
    fun onAnswered(ok: Boolean) {
        val shown = modal.shown as? WebDialog.Controls ?: return
        modal.answer<WebDialog.Controls, WebDialog.Controls?>(if (ok) shown else null)
    }

    /** OK: the layout, then the mode if the other one is marked. */
    private fun apply(chosen: WebDialog.Controls) {
        keyboardLayout.choose(chosen.layout)
        if (chosen.mode == mode) return
        analytics.track(
            "input_mode_switched",
            mapOf("from_mode" to mode.wireName, "to_mode" to chosen.mode.wireName, "trigger" to "controls_dialog", "result" to "success"),
        )
        mode = chosen.mode
        preference.save(mode)
        if (mode == PlayMode.KEYBOARD) game.closing = Focus.Board
    }

    private fun dialog(mode: PlayMode, layout: KeyboardLayout) = WebDialog.Controls(mode, layout, KeyHints.keyList(layout))
}
