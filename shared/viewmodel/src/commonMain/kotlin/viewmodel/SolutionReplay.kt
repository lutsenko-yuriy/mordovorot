package viewmodel

/** Mordie's [steps] replayed on their own copy of the [original] arrangement (GH-126), so the
 *  game's board, move count and Mordie never see them. Knows nothing about time: each view runs
 *  the clock, calling [shift] or [move] while [playback] is [Playback.PLAYING]. */
internal class SolutionReplay(private val original: IntArray, private val side: Int, val steps: List<SolutionStep>) {

    init {
        require(steps.isNotEmpty()) { "A replay needs at least one step" }
    }

    private var board = original.copyOf()

    val tiles: IntArray get() = board.copyOf()

    /** The highlighted step: `null` before the first Play; a finished step stays highlighted
     *  until the next one starts. */
    var step: Int? = null
        private set

    /** Shifts of [step] done so far. */
    private var shiftsDone = 0

    var playback = Playback.READY
        private set

    /** Whether this run, since Play or Play again, was paused. */
    var pausedThisRun = false
        private set

    /** Play, Pause, resume, or Play again from the original arrangement once ended. */
    fun playPause() {
        when (playback) {
            Playback.READY, Playback.PAUSED -> {
                if (step == null) step = 0
                playback = Playback.PLAYING
            }
            Playback.PLAYING -> {
                playback = Playback.PAUSED
                pausedThisRun = true
            }
            Playback.ENDED -> {
                board = original.copyOf()
                step = 0
                shiftsDone = 0
                pausedThisRun = false
                playback = Playback.PLAYING
            }
        }
    }

    /** One shift while playing; returns whether it finished a step. The last one ends the replay. */
    fun shift(): Boolean {
        if (playback != Playback.PLAYING) return false
        var current = step!!
        if (shiftsDone == steps[current].count) {
            current++
            step = current
            shiftsDone = 0
        }
        rotate(steps[current])
        shiftsDone++
        val finished = shiftsDone == steps[current].count
        if (finished && current == steps.lastIndex) playback = Playback.ENDED
        return finished
    }

    /** The rest of the step while playing - all of the next one, or what a pause mid-step
     *  left - returned as the shifts it made; `null` when not playing. */
    fun move(): SolutionStep? {
        if (playback != Playback.PLAYING) return null
        var shifts = 0
        do shifts++ while (!shift())
        return steps[step!!].copy(count = shifts)
    }

    private fun rotate(step: SolutionStep) {
        val cells = List(side) { j -> if (step.isRow) step.index * side + j else j * side + step.index }
        val values = cells.map { board[it] }
        cells.forEachIndexed { j, cell ->
            board[cell] = values[if (step.forward) (j - 1 + side) % side else (j + 1) % side]
        }
    }
}
