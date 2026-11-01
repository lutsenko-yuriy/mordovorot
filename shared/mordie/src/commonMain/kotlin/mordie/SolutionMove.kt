package mordie

/** One move of a solution: rotate a row right (or a column down) by [amount] cells. */
data class SolutionMove(val isRow: Boolean, val index: Int, val amount: Int)
