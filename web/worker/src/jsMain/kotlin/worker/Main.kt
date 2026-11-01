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
            search = BackgroundMordieSearch(start.original, start.ruledOut, onSnapshot = { progress ->
                // elapsedMs is read only once found, so the page is woken only when N or found changes.
                if (last?.moves != progress.moves || last?.found != progress.found) {
                    last = progress
                    self.postMessage(SearchProtocol.encodeProgress(progress))
                }
            })
        }
    }
}
