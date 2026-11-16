package web.tutorial

import texts.texts
import viewmodel.TileLabels
import web.drag.Axis

/** What Mordie says in the tutorial's bubble (GH-288), after `Mordie:`, worded by `:shared:texts` (GH-274). A phone drags only, so its texts leave the arrows out. */
object TutorialText {

    /** The text of guided step [step] (1 to 5); a tile is named by [labels]. */
    fun step(step: Int, computer: Boolean, labels: TileLabels): String = with(texts().tutorial) {
        when (step) {
            1 -> step1(computer)
            2 -> step2(labels.label(TutorialScript.WRAPPING_TILE, TutorialScript.SIDE), computer)
            3 -> step3(computer)
            4 -> step4(computer)
            else -> step5
        }
    }

    /** Step 3 after one of its two tiles, at [moves] moves. */
    fun again(moves: Int) = texts().tutorial.again(moves)

    /** A shift that isn't the asked one, on the asked line's axis or not. */
    fun blocked(asked: Axis) = with(texts().tutorial) { if (asked == Axis.ROW) blockedRow else blockedColumn }
}
