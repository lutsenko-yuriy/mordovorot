package viewmodel

/** The session's keyboard layout (GH-170): the preference, read once, or [KeyboardLayout.DEFAULT].
 *  One per session, like [SessionTileLabels], so a mode switch keeps it. */
class SessionKeyboardLayout(private val preference: KeyboardLayoutPreference) {

    var current: KeyboardLayout = preference.load() ?: KeyboardLayout.DEFAULT
        private set

    /** The Controls dialog's OK. A failed preference write still applies [to] for this session. */
    fun choose(to: KeyboardLayout) {
        if (to == current) return
        current = to
        preference.save(to)
    }
}
