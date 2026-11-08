package web

import org.w3c.dom.Window

/** [AppUpdates] over `navigator.serviceWorker` (GH-262, ADR-0006). Does nothing where there are no service workers
 *  (an insecure origin, some private windows) or when registering fails, as on the dev server, which has no worker file.
 *  Every rejected promise, an update check while offline included, is dropped. */
internal class DomAppUpdates(private val window: Window) : AppUpdates {

    private var registration: dynamic = null

    private val container: dynamic
        get() {
            val found = window.navigator.asDynamic().serviceWorker
            return if (found == null || jsTypeOf(found) == "undefined") null else found
        }

    override fun start(onReady: () -> Unit) {
        val container = container ?: return
        container.register(OFFLINE_WORKER).then(
            { found: dynamic ->
                registration = found
                // A worker that finished installing on an earlier visit and has waited ever since.
                reportIfWaiting(found, onReady)
                found.addEventListener("updatefound", {
                    val installing = found.installing
                    installing.addEventListener("statechange", {
                        if (installing.state == "installed") reportIfWaiting(found, onReady)
                    })
                })
            },
            { _: dynamic -> },
        )
    }

    /** Ready is a waiting worker with an active one running the page: on a first visit the worker installs, and nothing is new. */
    private fun reportIfWaiting(found: dynamic, onReady: () -> Unit) {
        if (found.waiting != null && found.active != null) onReady()
    }

    override fun check() {
        registration?.update()?.then({ _: dynamic -> }, { _: dynamic -> })
    }

    override fun apply() {
        val waiting = registration?.waiting
        if (waiting == null || waiting == undefined) {
            // Another tab already switched: this page only has to load it.
            window.location.reload()
            return
        }
        container.addEventListener("controllerchange", { window.location.reload() })
        waiting.postMessage(SKIP_WAITING)
    }

    private companion object {
        /** What `offline.OfflineCache.SKIP_WAITING` in `:web:offline` listens for. */
        const val SKIP_WAITING = "skip-waiting"
    }
}
