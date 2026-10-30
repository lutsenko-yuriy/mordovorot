package web

/** The toolbar's buttons and their digits (ADR-0003). */
enum class ToolbarButton(val digit: Int) { NEW(1), SAVE(2), LOAD(3), LABELS(4), GIVE_UP_ROW(5), CONTROLS(6) }

/** A key pressed outside a dialog. [modified] is with Ctrl, Alt or Meta, or part of an IME composition. */
data class KeyPress(val key: String, val modified: Boolean = false, val repeat: Boolean = false)

/** What a key does on the page (GH-171). */
sealed interface PageKey {
    data class Digit(val button: ToolbarButton) : PageKey
}

internal object WebKeys {

    fun pageKey(press: KeyPress, mode: PlayMode, controlsShown: Boolean): PageKey? {
        if (press.modified || press.repeat) return null
        return ToolbarButton.entries.firstOrNull { it.digit.toString() == press.key && live(it, mode, controlsShown) }?.let { PageKey.Digit(it) }
    }

    /** Whether [button]'s digit works, and so shows: in keyboard mode all do, in mouse mode only Controls'. */
    fun live(button: ToolbarButton, mode: PlayMode, controlsShown: Boolean): Boolean =
        if (button == ToolbarButton.CONTROLS) controlsShown else mode == PlayMode.KEYBOARD
}
