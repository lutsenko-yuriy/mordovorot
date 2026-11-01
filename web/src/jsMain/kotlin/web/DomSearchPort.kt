package web

import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker

/** A [SearchPort] over the Web Worker built from `:web:worker` (GH-218). */
class DomSearchPort : SearchPort {
    // A URL relative to the page, so the harness's variant folders work too.
    private val worker = Worker("mordie-worker.js")

    override var onMessage: (String) -> Unit = {}
        set(value) {
            field = value
            worker.onmessage = { event -> value((event as MessageEvent).data as String) }
        }

    override fun send(message: String) = worker.postMessage(message)

    override fun terminate() = worker.terminate()
}
