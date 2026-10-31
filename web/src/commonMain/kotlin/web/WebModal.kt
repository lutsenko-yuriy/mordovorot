package web

import kotlinx.coroutines.CompletableDeferred

/** How a dialog was cancelled, as `dialog_cancelled.via` (GH-163). */
internal enum class CancelVia(val wire: String) { BUTTON("button"), ESCAPE("escape") }

/** [WebGame]'s modal dialog: at most one is open, and [ask] waits for its answer. [render] draws
 *  the page with the dialog shown, focusing its default control, then without it once answered. */
internal class WebModal(private val render: (focus: Focus?) -> Unit) {

    private class OpenDialog<T>(var shown: WebDialog) {
        val answer = CompletableDeferred<T>()
        var via = CancelVia.BUTTON
    }

    private var open: OpenDialog<*>? = null

    /** How the last dialog closed. Read right after [ask] returns: an answer may resume the asking
     *  flow only after the call that gave it has returned, so a flag around that call would be gone. */
    var closedVia = CancelVia.BUTTON
        private set

    /** The open dialog as the page shows it; `null` when none is. */
    val shown: WebDialog? get() = open?.shown

    /** Redraws the open dialog as [dialog], e.g. the Save name as typed. */
    fun update(dialog: WebDialog) {
        open?.shown = dialog
    }

    /** Gives the open dialog's Cancel answer, or No's; Can't be solved has neither. */
    fun escape() {
        val dialog = open ?: return
        val cancel = when (dialog.shown) {
            is WebDialog.Unsolvable -> return
            WebDialog.GiveUp -> false
            is WebDialog.Size, is WebDialog.Save, is WebDialog.Load, is WebDialog.Labels, is WebDialog.Controls -> null
        }
        dialog.via = CancelVia.ESCAPE
        answer(cancel) { true }
    }

    suspend fun <T> ask(shown: WebDialog): T {
        val dialog = OpenDialog<T>(shown)
        open = dialog
        render(defaultFocus(shown))
        try {
            return dialog.answer.await()
        } finally {
            closedVia = dialog.via
            open = null
            render(null)
        }
    }

    /** Completes the open dialog when it is a [D]; any other answer is stale and ignored. */
    inline fun <reified D : WebDialog, T> answer(value: T) = answer(value) { it is D }

    fun <T> answer(value: T, matches: (WebDialog) -> Boolean) {
        val dialog = open?.takeIf { matches(it.shown) } ?: return
        @Suppress("UNCHECKED_CAST")
        (dialog as OpenDialog<T>).answer.complete(value)
    }
}
