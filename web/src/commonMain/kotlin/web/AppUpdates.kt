package web

/** The worker the page registers (GH-262): a relative URL, as the public site's sub-path requires (GH-111).
 *  Its name is fixed for good: an old worker only ever re-checks its own URL. */
internal const val OFFLINE_WORKER = "offline-worker.js"

/** The page's seam to the offline worker (GH-262), like [PageAddress] and [Clipboard]. */
interface AppUpdates {
    /** Registers the worker. [onFound] is called when a new version starts downloading (a version already waiting
     *  counts as found at once), [onReady] each time one is fully cached and waiting. Does nothing where there
     *  are no workers. */
    fun start(onFound: () -> Unit, onReady: () -> Unit)

    /** One update check. Offline, or with nothing new, it does nothing. */
    fun check()

    /** Switches to the waiting version and reloads, or just reloads when another tab already did. */
    fun apply()
}

/** What made a version check, as `update_available.check_trigger` (GH-262). */
internal enum class CheckTrigger(val wire: String) {
    LOAD("load"),
    TAB_RETURN("tab_return"),
    GAME_END("game_end"),
}
