package viewmodel

/** Which [ShiftSource]s made the current game's shifts, for `board_solved.shift_keys` (GH-101)
 *  and `shift_input` (GH-110). One per session, so a mode switch keeps it; reset when a new
 *  game, load or repair starts. */
class GameShiftSources {
    private val keys = mutableSetOf<ShiftKey>()
    private val pointers = mutableSetOf<PointerShift>()

    fun record(source: ShiftSource) {
        when (source) {
            is ShiftKey -> keys += source
            is PointerShift -> pointers += source
        }
    }

    fun reset() {
        keys.clear()
        pointers.clear()
    }

    /** `shift_keys` (`arrows`/`hjkl`/`mixed`) when the [solvingSource] was a key, `shift_input`
     *  (`arrows`/`drag`/`mixed`) when it was a pointer, nothing for `null`. */
    fun propertiesFor(solvingSource: ShiftSource?): Map<String, String> = when (solvingSource) {
        is ShiftKey -> mapOf("shift_keys" to summary(keys, ShiftKey.ARROW to "arrows", ShiftKey.HJKL to "hjkl"))
        is PointerShift -> mapOf("shift_input" to summary(pointers, PointerShift.ARROW to "arrows", PointerShift.DRAG to "drag"))
        null -> emptyMap()
    }

    /** The one kind's name when only it was used, otherwise `mixed`. */
    private fun <T> summary(used: Set<T>, vararg names: Pair<T, String>): String =
        names.singleOrNull { (kind, _) -> used == setOf(kind) }?.second ?: "mixed"
}
