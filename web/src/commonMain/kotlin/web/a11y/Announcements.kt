package web.a11y

import texts.texts
import viewmodel.Direction
import viewmodel.SolutionStep
import viewmodel.TileLabels

/** What a screen reader hears (GH-166), and the arrows' accessible names. Lines are 0-based here
 *  and counted from 1 in the words. A [newMove] is the move count after a shift that started a
 *  move, `null` when it continued one. */
internal object Announcements {

    /** `Row 2 left`, `Column 4 up`. */
    fun line(direction: Direction, line: Int): String = with(texts().reader) {
        when (direction) {
            Direction.LEFT -> rowLeft(line + 1)
            Direction.RIGHT -> rowRight(line + 1)
            Direction.UP -> columnUp(line + 1)
            Direction.DOWN -> columnDown(line + 1)
        }
    }

    fun shift(direction: Direction, line: Int, newMove: Int?): String = withMoves(line(direction, line), newMove)

    /** `Row 2 left 2`: how far the drag shifted its line. */
    fun drag(direction: Direction, line: Int, distance: Int, newMove: Int?): String =
        withMoves(texts().reader.dragged(line(direction, line), distance), newMove)

    fun solved(moves: Int) = texts().reader.solved(moves)

    fun newGame(side: Int) = texts().reader.newGame(side)

    /** A load; [solutionSteps] when it reopened the solution screen, with that many steps. */
    fun loaded(name: String, side: Int, moves: Int, solved: Boolean, solutionSteps: Int? = null) = with(texts().reader) {
        when {
            solutionSteps != null -> loadedSolution(name, side, solutionSteps)
            solved -> loadedSolved(name, side, moves)
            else -> loadedPlain(name, side, moves)
        }
    }

    /** Entering the solution screen, with its number of [steps]. */
    fun solution(steps: Int) = texts().reader.solution(steps)

    /** The solution screen's [current] step in words, or the start when `null`. */
    fun step(steps: List<SolutionStep>, current: Int?): String {
        if (current == null) return texts().reader.stepStart(steps.size)
        val step = steps[current]
        val direction = when {
            step.isRow -> if (step.forward) Direction.RIGHT else Direction.LEFT
            else -> if (step.forward) Direction.DOWN else Direction.UP
        }
        return texts().reader.stepOf(current + 1, steps.size, line(direction, step.index), step.count)
    }

    /** The solution's board as a reader names it on taking focus: `Board, step 3 of 12: Column 3 up 2`. */
    fun boardStep(steps: List<SolutionStep>, current: Int?) = texts().reader.boardStep(step(steps, current))

    /** [words] for a shift, then the [tile] now under the selection: `Row 2 left, 5 moves. Now charlie`. */
    fun withNow(words: String, tile: String) = texts().reader.withNow(words, spoken(tile))

    /** `Row 1: 5, 2, 9. Row 2: … 3 tiles in place.`: [texts] row by row on a [side] board, [home]
     *  marking the tiles in place. */
    fun board(texts: List<String>, home: List<Boolean>, side: Int): String = with(reader()) {
        val rows = texts.chunked(side).mapIndexed { row, line -> boardRow(row + 1, line.joinToString(", ", transform = ::spoken)) }
        rows.joinToString(" ") + " " + tilesInPlace(home.count { it })
    }

    /** A tile [label] as said: letters in the phonetic alphabet (`BC` -> `bravo charlie`), so `B`
     *  and `D` don't sound alike; Cyrillic letters by the Russian spelling words (`АБ` -> `Анна Борис`, GH-313); numbers as they are. */
    fun spoken(label: String): String = when {
        label.all { it in 'A'..'Z' } -> label.map { texts().reader.phonetic[it - 'A'] }.joinToString(" ")
        label.all { it in TileLabels.CYRILLIC_ALPHABET } -> label.map { texts().reader.cyrillicPhonetic[TileLabels.CYRILLIC_ALPHABET.indexOf(it)] }.joinToString(" ")
        else -> label
    }

    /** What a tile in place adds: to Read board's count, and to the tile's own cell. */
    val IN_PLACE: String get() = texts().reader.inPlace

    /** A function of its own, since [board]'s `texts` parameter hides the accessor. */
    private fun reader() = texts().reader

    private fun withMoves(words: String, newMove: Int?) = newMove?.let { texts().reader.withMoves(words, it) } ?: words
}
