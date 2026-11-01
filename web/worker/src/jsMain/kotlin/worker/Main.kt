package worker

import mordie.BackgroundMordieSearch
import mordie.SearchProgress
import mordie.SearchProtocol
import org.w3c.dom.DedicatedWorkerGlobalScope

private external val self: DedicatedWorkerGlobalScope

/** The worker's entry point (GH-218): searches for the page that sent the start message. */
fun main() {
    var search: BackgroundMordieSearch? = null
    self.onmessage = { event ->
        val message = event.data as String
        if (search == null && SearchProtocol.kindOf(message) == SearchProtocol.Kind.START) {
            val start = SearchProtocol.decodeStart(message)
            var last: SearchProgress? = null
            search = BackgroundMordieSearch(start.original, start.ruledOut, start.best, onSnapshot = { progress ->
                // elapsedMs is read only once found, so the page is woken only when something else changes.
                if (last?.copy(elapsedMs = 0) != progress.copy(elapsedMs = 0)) {
                    last = progress
                    self.postMessage(SearchProtocol.encodeProgress(progress))
                }
            })
        }
    }
}
