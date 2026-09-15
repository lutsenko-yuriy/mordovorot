package viewmodel

/** A save's name plus its board size (GH-44 WU4) - every save-listing surface (startup restore
 *  prompts, the mid-game Load dialog/command) shows both, so a player can tell at a glance
 *  whether a save will hit the size-mismatch rejection before choosing it. [squareSide] is
 *  `null` when it couldn't be determined (a corrupted or unreadable save) - callers show the
 *  name alone rather than failing the whole listing over one bad entry. */
data class SaveInfo(val name: String, val squareSide: Int?)

/** `"name (NxN)"`, or just `"name"` when [SaveInfo.squareSide] is unknown. */
fun SaveInfo.display(): String = if (squareSide != null) "$name (${squareSide}x$squareSide)" else name
