package web.tutorial

import viewmodel.TileLabels
import web.drag.Axis

/** What Mordie says in the tutorial's bubble (GH-288), after `Mordie:`. A phone drags only, so its texts leave the arrows out. */
object TutorialText {

    /** The text of guided step [step] (1 to 5); a tile is named by [labels]. */
    fun step(step: Int, computer: Boolean, labels: TileLabels): String = when (step) {
        1 -> "Hi there! I'm Mordie. Let's play a short game. Drag the top row one tile left" + or(computer, "click its ◀") + "."
        2 -> {
            val tile = labels.label(TutorialScript.WRAPPING_TILE, TutorialScript.SIDE)
            "Lines go round: see tile $tile at the top of the second column? " +
                (if (computer) "Shift that column one tile up, by dragging or with ▲, and" else "Drag that column one tile up, and") +
                " $tile comes back in at the bottom."
        }
        3 -> "A move is one row or column shifted, however far. Drag the third row two tiles right" +
            (if (computer) ", or click its ▶ twice" else "") + ": still 1 move."
        4 -> "Last one: drag the fourth column one tile down" + or(computer, "click its ▼") + ". That solves it!"
        else -> "Can you beat my results?"
    }

    /** Step 3 after one of its two tiles, at [moves] moves. */
    fun again(moves: Int) = "Once more! It's the same row, so Moves stays at $moves."

    /** A shift that isn't the asked one, on the asked line's axis or not. */
    fun blocked(asked: Axis) = "You can do that later. For now shift that ${if (asked == Axis.ROW) "row" else "column"} please!"

    private fun or(computer: Boolean, arrows: String) = if (computer) ", or $arrows" else ""
}
