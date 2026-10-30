package web

/** How the page is played (GH-171). [wireName] is the stored preference's value, `app_launched.mode`
 *  and `input_mode_switched`'s. */
enum class PlayMode(val wireName: String, val title: String) {
    MOUSE("mouse", "Mouse"),
    KEYBOARD("keyboard", "Keyboard"),
    ;

    companion object {
        val DEFAULT = MOUSE

        fun fromWireName(name: String?): PlayMode? = entries.firstOrNull { it.wireName == name }
    }
}
