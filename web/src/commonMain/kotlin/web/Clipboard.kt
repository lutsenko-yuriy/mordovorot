package web

/** The browser's clipboard (GH-243), which may refuse. */
fun interface Clipboard {
    /** Puts [text] on the clipboard; [done] gets whether it got there. */
    fun copy(text: String, done: (Boolean) -> Unit)
}
