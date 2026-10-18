package viewmodel

/** Mordie's [steps] replayed on their own copy of the [original] arrangement (GH-126), so the
 *  game's board, move count and Mordie never see them. Stepped by hand, one shift at a time:
 *  [next], [previous] and [restart]. */
internal class SolutionReplay(private val original: IntArray, private val side: Int, val steps: List<SolutionStep>) {

    init {
        require(steps.isNotEmpty()) { "A replay needs at least one step" }
    }

    /** Every shift of every step, in order: its step's index and the one-shift step itself. */
    private val shifts = steps.flatMapIndexed { index, step -> List(step.count) { index to step.copy(count = 1) } }

    private var board = original.copyOf()

    /** Shifts applied so far. */
    private var position = 0

    val tiles: IntArray get() = board.copyOf()

    /** The step of the last shift applied; `null` at the start. */
    val step: Int? get() = shifts.getOrNull(position - 1)?.first

    val atStart: Boolean get() = position == 0
    val atEnd: Boolean get() = position == shifts.size

    /** Runs since the screen opened; a run begins with a [next] off the start. For analytics,
     *  kept here rather than in the use case so a mode switch's rebuilt viewModel keeps them. */
    var runs = 0

    /** Whether [previous] was used during this run, and whether it has reached the end. */
    var steppedBack = false
    var finished = false

    /** Applies the next shift and returns it; `null` at the end. */
    fun next(): SolutionStep? {
        if (atEnd) return null
        val shift = shifts[position++].second
        rotate(shift)
        return shift
    }

    /** Undoes the last shift and returns the shift that did so; `null` at the start. */
    fun previous(): SolutionStep? {
        if (atStart) return null
        val back = shifts[--position].second.let { it.copy(forward = !it.forward) }
        rotate(back)
        return back
    }

    fun restart() {
        board = original.copyOf()
        position = 0
    }

    private fun rotate(step: SolutionStep) {
        val cells = List(side) { j -> if (step.isRow) step.index * side + j else j * side + step.index }
        val values = cells.map { board[it] }
        cells.forEachIndexed { j, cell ->
            board[cell] = values[if (step.forward) (j - 1 + side) % side else (j + 1) % side]
        }
    }
}
