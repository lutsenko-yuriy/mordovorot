package web

import mordie.MordieSearch
import mordie.MordieSearchFactory
import mordie.SearchProgress
import mordie.SearchProtocol
import mordie.SolutionMove

/** The page's end of a Web Worker, so [WorkerMordieSearch] needs no DOM (GH-218). */
interface SearchPort {
    /** Set by [WorkerMordieSearch] before it sends anything. */
    var onMessage: (String) -> Unit

    fun send(message: String)

    /** Stops the worker at once, even mid-slice. */
    fun terminate()
}

/**
 * A [MordieSearch] that runs in a Web Worker, so the page's timers aren't delayed by the search
 * and a hidden tab keeps searching (GH-218). One worker per search: [cancel] terminates it.
 */
class WorkerMordieSearch private constructor(private val port: SearchPort, ruledOut: Int, best: List<SolutionMove>?) : MordieSearch {

    // A kept solution is the answer until the worker reports a better one (GH-224).
    private var latest = best?.let { SearchProgress(it.size, found = true, elapsedMs = 0, solution = it, ruledOut = ruledOut, provenShortest = false) }
        ?: SearchProgress(moves = ruledOut, found = false, elapsedMs = 0)
    private var frozen = false

    init {
        port.onMessage = { message ->
            // Once proven shortest the result is final; once cancelled a late message is stale.
            if (!frozen && !latest.provenShortest) {
                latest = SearchProtocol.decodeProgress(message)
                if (latest.provenShortest) port.terminate()
            }
        }
    }

    override fun progress(): SearchProgress = latest

    override fun cancel() {
        frozen = true
        port.terminate()
    }

    /** Opens a [SearchPort] per search; the browser passes one over `Worker("mordie-worker.js")`. */
    class Factory(private val openPort: () -> SearchPort) : MordieSearchFactory {
        override fun start(original: IntArray, ruledOut: Int, best: List<SolutionMove>?): MordieSearch {
            val port = openPort()
            return WorkerMordieSearch(port, ruledOut, best).also { port.send(SearchProtocol.encodeStart(original, ruledOut, best)) }
        }
    }
}
