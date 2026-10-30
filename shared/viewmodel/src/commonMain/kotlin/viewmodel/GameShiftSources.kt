package viewmodel

/** Which [ShiftSource]s made the current game's shifts, for `board_solved.shift_keys` (GH-101)
 *  and `shift_input` (GH-110). One per session, so a mode switch keeps it; reset when a new
 *  game, load or repair starts. */
class GameShiftSources {
    private val keys = mutableSetOf<ShiftKey>()
    private val inputs = mutableSetOf<String>()

    fun record(source: ShiftSource) {
        when (source) {
            is ShiftKey -> keys += source
            is PointerShift -> inputs += POINTER_NAMES.getValue(source)
            is KeyShift -> {
                inputs += KEYS
                source.key?.let { keys += it }
            }
        }
    }

    fun reset() {
        keys.clear()
        inputs.clear()
    }

    /** `shift_keys` (`arrows`/`hjkl`/`mixed`) when the [solvingSource] was a terminal key, `shift_input`
     *  (`arrows`/`drag`/`keys`/`mixed`) when it was on the web, with `shift_keys` too for a right-handed
     *  web key (GH-171), nothing for `null`. */
    fun propertiesFor(solvingSource: ShiftSource?): Map<String, String> = when (solvingSource) {
        is ShiftKey -> mapOf(SHIFT_KEYS to keysSummary())
        is PointerShift -> mapOf(SHIFT_INPUT to inputSummary())
        is KeyShift -> mapOf(SHIFT_INPUT to inputSummary()) + if (solvingSource.key != null) mapOf(SHIFT_KEYS to keysSummary()) else emptyMap()
        null -> emptyMap()
    }

    private fun keysSummary() = when (keys.singleOrNull()) {
        ShiftKey.ARROW -> "arrows"
        ShiftKey.HJKL -> "hjkl"
        null -> MIXED
    }

    private fun inputSummary() = inputs.singleOrNull() ?: MIXED

    private companion object {
        const val SHIFT_KEYS = "shift_keys"
        const val SHIFT_INPUT = "shift_input"
        const val KEYS = "keys"
        const val MIXED = "mixed"
        val POINTER_NAMES = mapOf(PointerShift.ARROW to "arrows", PointerShift.DRAG to "drag")
    }
}
