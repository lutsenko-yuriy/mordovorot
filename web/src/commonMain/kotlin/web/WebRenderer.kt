package web

/** Draws each [WebState] the game publishes - the DOM on the page, a recorder in tests. */
fun interface WebRenderer {
    fun render(state: WebState)

    /** Scrolls the solution's steps by [lines], up when negative (GH-171). */
    fun scrollSteps(lines: Int) {}
}
