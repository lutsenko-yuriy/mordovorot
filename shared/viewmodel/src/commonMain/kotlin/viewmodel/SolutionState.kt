package viewmodel

/** What a view draws on the solution screen (GH-126): the numbered [steps], the highlighted
 *  one ([current], `null` at the start), and whether Prev ([atStart]) or Next ([atEnd]) has
 *  nowhere to go. */
data class SolutionState(val steps: List<String>, val current: Int?, val atStart: Boolean, val atEnd: Boolean)

/** The row above the toolbar (GH-126): `[ Give Up ]` dimmed or live, `[ Mordie's solution ]`
 *  after a win, or nothing. */
enum class GiveUpButton { DIMMED, GIVE_UP, MORDIES_SOLUTION, HIDDEN }
