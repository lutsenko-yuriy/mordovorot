package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-262: the page's Update posts the message the offline worker waits for. They are in two modules, so the strings are
 *  two copies, and a rename on one side would leave Update posting to nobody. */
class SkipWaitingMessageTest {
    private fun constant(path: String): String? =
        Regex("""const val SKIP_WAITING = "([^"]+)"""").find(File(path).readText())?.groupValues?.get(1)

    @Test
    fun `the page and the worker agree on the skip-waiting message`() {
        val page = constant("src/jsMain/kotlin/web/DomAppUpdates.kt")
        val worker = constant("offline/src/commonMain/kotlin/offline/OfflineCache.kt")

        assertEquals("skip-waiting", worker)
        assertEquals(worker, page)
    }
}
