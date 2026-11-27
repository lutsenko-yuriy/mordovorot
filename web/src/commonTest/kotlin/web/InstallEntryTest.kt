package web

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-360: which entry the footer shows, as a table over [installEntry]. */
class InstallEntryTest {

    /** A page that would show the entry, one thing changed per case. */
    private fun entry(
        promptWaiting: Boolean = true,
        safari: InstallDevice? = null,
        standalone: Boolean = false,
        installed: Boolean = false,
        tutorialDone: Boolean = true,
        tutorialRunning: Boolean = false,
        updateReady: Boolean = false,
        enabled: Boolean = true,
    ) = installEntry(promptWaiting, safari, standalone, installed, tutorialDone, tutorialRunning, updateReady, enabled)

    @Test
    fun `a waiting prompt shows the browser's prompt entry`() {
        assertEquals(InstallMethod.BROWSER_PROMPT, entry())
    }

    @Test
    fun `Safari shows the steps entry`() {
        assertEquals(InstallMethod.SAFARI_STEPS, entry(promptWaiting = false, safari = InstallDevice.MAC))
        assertEquals(InstallMethod.SAFARI_STEPS, entry(promptWaiting = false, safari = InstallDevice.PHONE_OR_TABLET))
    }

    @Test
    fun `a browser with neither shows nothing, a spent prompt included`() {
        assertEquals(null, entry(promptWaiting = false))
    }

    @Test
    fun `the prompt wins when both are there`() {
        assertEquals(InstallMethod.BROWSER_PROMPT, entry(promptWaiting = true, safari = InstallDevice.MAC))
    }

    @Test
    fun `each reason to hide hides both entries`() {
        for (safari in listOf(null, InstallDevice.PHONE_OR_TABLET)) {
            val waiting = safari == null
            assertEquals(null, entry(waiting, safari, standalone = true), "standalone")
            assertEquals(null, entry(waiting, safari, installed = true), "installed")
            assertEquals(null, entry(waiting, safari, tutorialDone = false), "tutorial not done")
            assertEquals(null, entry(waiting, safari, tutorialRunning = true), "tutorial running, a replay too")
            assertEquals(null, entry(waiting, safari, updateReady = true), "update notice")
            assertEquals(null, entry(waiting, safari, enabled = false), "kill switch")
        }
    }
}
