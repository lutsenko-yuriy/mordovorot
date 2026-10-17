package viewmodel

/** Which kind of key made a keyboard-mode shift (GH-101); `null` for a mouse click. */
enum class ShiftKey { ARROW, HJKL }

/** Which [ShiftKey]s made the current game's shifts, for `board_solved.shift_keys` (GH-101).
 *  One per session, so a mode switch keeps it; reset when a new game, load or repair starts. */
class GameShiftKeys {
    private val used = mutableSetOf<ShiftKey>()

    fun record(key: ShiftKey) {
        used += key
    }

    fun reset() = used.clear()

    /** `arrows`, `hjkl`, `mixed`, or `null` when no key was recorded. */
    val value: String?
        get() = when (used) {
            setOf(ShiftKey.ARROW) -> "arrows"
            setOf(ShiftKey.HJKL) -> "hjkl"
            emptySet<ShiftKey>() -> null
            else -> "mixed"
        }
}
