package viewmodel

/** What a view draws on the solution screen (GH-126): the numbered [steps], the highlighted
 *  one ([current], `null` before the first Play) and where the replay is. */
data class SolutionState(val steps: List<String>, val current: Int?, val playback: Playback)

/** Where the solution screen's replay is; its button reads Play, Pause or Play again. */
enum class Playback { READY, PLAYING, PAUSED, ENDED }

/** The row above the toolbar (GH-126): `[ Give Up ]` dimmed or live, `[ Mordie's solution ]`
 *  after a win, or nothing. */
enum class GiveUpButton { DIMMED, GIVE_UP, MORDIES_SOLUTION, HIDDEN }
