package web

/** The session's last input (GH-171), as `input_method`: an event a dialog's answer fires later
 *  still gets the press that answered it. */
class PressedWith {
    private var keyboard = false

    val inputMethod: String get() = if (keyboard) "keyboard" else "rich_ui"

    fun key() {
        keyboard = true
    }

    fun pointer() {
        keyboard = false
    }
}
