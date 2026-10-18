package storage

/** One step of Mordie's saved solution (GH-126): rotate row (or column) [index] right (or down)
 *  by [amount] cells. Mirrors `mordie.SolutionMove`, so this module imports nothing from Mordie. */
data class SavedMove(val isRow: Boolean, val index: Int, val amount: Int)
