package viewmodel

/** A save's name plus its board size (GH-44 WU4) - every save-listing surface (startup restore
 *  prompts, the mid-game Load dialog/command) shows both, so a player can tell at a glance
 *  what size loading a save will resize the board to (WU5). [squareSide] is
 *  `null` when it couldn't be determined (a corrupted or unreadable save) - callers show the
 *  name alone rather than failing the whole listing over one bad entry. */
data class SaveInfo(
    val name: String,
    val squareSide: Int?,
    /** `false` when no sequence of shifts can solve the saved board (GH-74). An unreadable save
     *  stays `true` - unmarked, same "degrade, don't fail" rule as a `null` [squareSide]. */
    val solvable: Boolean = true,
)

/** `"name (NxN)"`, or just `"name"` when [SaveInfo.squareSide] is unknown. */
fun SaveInfo.displayWithSize(): String = if (squareSide != null) "$name (${squareSide}x${squareSide})" else name

/** Appended by [display] to a save that can't be solved; `load` strips it back off (GH-74). */
const val UNSOLVABLE_MARK = "unsolvable"

/** [displayWithSize] plus the [UNSOLVABLE_MARK] when the save can't be solved (GH-74). */
fun SaveInfo.display(): String = if (solvable) displayWithSize() else "${displayWithSize()} $UNSOLVABLE_MARK"
