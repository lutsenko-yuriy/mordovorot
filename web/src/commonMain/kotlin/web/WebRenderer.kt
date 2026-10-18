package web

/** Draws each [WebState] the game publishes - the DOM on the page, a recorder in tests. */
fun interface WebRenderer {
    fun render(state: WebState)
}
