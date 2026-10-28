package web

import viewmodel.SolutionStep
import web.drag.Axis
import web.drag.DragShift
import kotlin.math.abs

/** What a screen reader hears (GH-166), and the arrows' accessible names. Lines are 0-based here
 *  and counted from 1 in the words. A [newMove] is the move count after a shift that started a
 *  move, `null` when it continued one. */
internal object Announcements {

    /** `Row 2 left`, `Column 4 up`. */
    fun line(arrow: Arrow, line: Int): String = when (arrow) {
        Arrow.LEFT -> "Row ${line + 1} left"
        Arrow.RIGHT -> "Row ${line + 1} right"
        Arrow.UP -> "Column ${line + 1} up"
        Arrow.DOWN -> "Column ${line + 1} down"
    }

    fun shift(arrow: Arrow, line: Int, newMove: Int?): String = line(arrow, line) + movesSuffix(newMove)

    /** `Row 2 left 2`: how far the drag shifted its line. */
    fun drag(shift: DragShift, newMove: Int?): String {
        val arrow = when (shift.axis) {
            Axis.ROW -> if (shift.steps > 0) Arrow.RIGHT else Arrow.LEFT
            Axis.COLUMN -> if (shift.steps > 0) Arrow.DOWN else Arrow.UP
        }
        return "${line(arrow, shift.line)} ${abs(shift.steps)}" + movesSuffix(newMove)
    }

    fun solved(moves: Int) = "Solved in ${moves(moves)}"

    fun newGame(side: Int) = "New ${side}x$side game"

    /** A load; [solutionSteps] when it reopened the solution screen, with that many steps. */
    fun loaded(name: String, side: Int, moves: Int, solved: Boolean, solutionSteps: Int? = null) =
        "Loaded '$name', ${side}x$side, " + when {
            solutionSteps != null -> "solution, ${moves(solutionSteps)}"
            solved -> "solved in ${moves(moves)}"
            else -> moves(moves)
        }

    /** Entering the solution screen, with its number of [steps]. */
    fun solution(steps: Int) = "Solution, ${moves(steps)}"

    /** The solution screen's [current] step in words, or the start when `null`. */
    fun step(steps: List<SolutionStep>, current: Int?): String {
        if (current == null) return "Start, step 0 of ${steps.size}"
        val step = steps[current]
        val arrow = when {
            step.isRow -> if (step.forward) Arrow.RIGHT else Arrow.LEFT
            else -> if (step.forward) Arrow.DOWN else Arrow.UP
        }
        return "Step ${current + 1} of ${steps.size}: ${line(arrow, step.index)} ${step.count}"
    }

    /** `Row 1: 5, 2, 9. Row 2: … 3 tiles in place.`: [texts] row by row on a [side] board, [home]
     *  marking the tiles in place. */
    fun board(texts: List<String>, home: List<Boolean>, side: Int): String {
        val rows = texts.chunked(side).mapIndexed { row, line -> "Row ${row + 1}: ${line.joinToString(", ", transform = ::spoken)}." }
        val count = home.count { it }
        return rows.joinToString(" ") + " ${if (count == 1) "1 tile" else "$count tiles"} $IN_PLACE."
    }

    /** A tile [label] as said: letters in the phonetic alphabet (`BC` -> `bravo charlie`), so `B`
     *  and `D` don't sound alike; numbers as they are. */
    fun spoken(label: String): String =
        if (label.all { it in 'A'..'Z' }) label.map { PHONETIC[it - 'A'] }.joinToString(" ") else label

    private val PHONETIC = listOf(
        "alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel", "india", "juliet", "kilo", "lima", "mike",
        "november", "oscar", "papa", "quebec", "romeo", "sierra", "tango", "uniform", "victor", "whiskey", "x-ray", "yankee", "zulu",
    )

    /** What a tile in place adds: to Read board's count, and to the tile's own cell. */
    const val IN_PLACE = "in place"

    private fun movesSuffix(newMove: Int?) = newMove?.let { ", ${moves(it)}" } ?: ""

    private fun moves(n: Int) = if (n == 1) "1 move" else "$n moves"
}
