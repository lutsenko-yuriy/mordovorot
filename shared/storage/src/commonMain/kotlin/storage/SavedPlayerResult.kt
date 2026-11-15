package storage

/** How the player's game ended before the solution screen (GH-273): [gaveUp] or solved, after [moves]
 *  moves. Mirrors `viewmodel.PlayerResult`, which storage can't import. */
data class SavedPlayerResult(val gaveUp: Boolean, val moves: Int)
