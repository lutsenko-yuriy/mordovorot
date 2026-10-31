package web.a11y

/** A control the page can focus: [key] is its `data-focus`. [fallsBack]: focus goes back to it, or
 *  passes on as the page's fallback says when it's gone or disabled, without scrolling; otherwise
 *  it gets focus if it's on the page, else none. */
interface FocusTarget {
    val key: String
    val fallsBack: Boolean get() = false
}
