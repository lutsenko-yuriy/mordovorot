package viewmodel

/** How the player's game ended before the solution screen (GH-273): [gaveUp] or solved, after [moves] moves. */
data class PlayerResult(val gaveUp: Boolean, val moves: Int) {

    /** The `player_result` analytics value. */
    val analyticsName: String get() = if (gaveUp) "gave_up" else "solved"
}
