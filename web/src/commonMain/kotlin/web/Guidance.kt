package web

import web.drag.Axis

/** What the tutorial's panel shows (GH-288): Mordie's [text] for guided [step] (1 to 5), the [ways] to do the asked shift on a computer (GH-290),
 *  whether `[ Controls ]` shows, and the asked line. */
data class Guidance(val step: Int, val text: String, val ways: List<String> = emptyList(), val controls: Boolean, val highlight: TutorialHighlight?)

/** The line a guided step asks to shift, and on a computer the [arrow] to click; the highlight of steps 1 to 4. */
data class TutorialHighlight(val axis: Axis, val line: Int, val arrow: Arrow?)
