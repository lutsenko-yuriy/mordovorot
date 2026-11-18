package viewmodel

/** The slot an autosave writes (GH-338): one per tile label scheme and board size, named like `Autosave_letter_pairs_9x9`
 *  in every language. A hand-typed Save name equal to a slot overwrites it. */
object AutosaveSlot {
    private const val PREFIX = "Autosave_"

    fun nameFor(labels: TileLabels, side: Int): String = "$PREFIX${labels.wireName}_${side}x$side"

    fun isAutosave(name: String): Boolean = name.startsWith(PREFIX)
}

/** What [GameViewModel.autosave] did: nothing to save, wrote the slot, or could not write it. */
enum class AutosaveResult { SKIPPED, SAVED, FAILED }
