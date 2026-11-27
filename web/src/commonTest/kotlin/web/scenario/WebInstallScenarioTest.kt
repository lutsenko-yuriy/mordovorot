package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import texts.texts
import web.Focus
import web.FooterPart
import web.InstallDevice
import web.Layout
import web.WebDialog
import web.fallbackFocus

/**
 * GH-360 scenarios: the Install entry in the footer, through [WebHarness] and a scripted [FakeAppInstall]. Texts and analytics
 * properties come from the plan (issue #360) and `docs/ANALYTICS_EVENTS.md`.
 */
class WebInstallScenarioTest {

    /** Chrome, with the install prompt already offered. */
    private fun chrome(offered: Boolean = true, standalone: Boolean = false) = FakeAppInstall(CHROME_DESKTOP, standalone, offered)

    /** A started page with no startup dialog in the way, and the events of the start kept. */
    private fun TestScope.ready(install: FakeAppInstall, layout: Layout = Layout.WIDE, installOn: Boolean = true): WebHarness =
        WebHarness(backgroundScope, layout = layout, install = install, installOn = installOn).start().also {
            if (it.state.dialog is WebDialog.Size) it.game.onSizeChosen(null)
        }

    private fun WebHarness.hasEntry() = state.footer.any { it is FooterPart.InstallLink }

    private fun WebHarness.footerText() = state.footer.joinToString("") { it.text }

    private fun WebHarness.installEvents() = analytics.events.filter { it.name.startsWith("install_") || it.name == "app_installed" || it.name == "screen_install_dialog" }

    // When it shows
    @Test
    fun `no entry while the first-launch tutorial runs`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true, install = chrome()).start()

        assertTrue(page.game.tutorial.running)
        assertFalse(page.hasEntry())
        assertTrue(page.props("install_entry_shown").isEmpty())
    }

    @Test
    fun `Skip shows the entry`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true, install = chrome()).start()

        page.game.tutorial.onSkip()

        assertTrue(page.hasEntry())
        assertEquals(listOf(mapOf("method" to "browser_prompt")), page.ownProps("install_entry_shown"))
    }

    @Test
    fun `finishing the tutorial shows the entry`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true, install = chrome()).start()
        page.playSteps(4)
        assertFalse(page.hasEntry(), "the last step is still to come")

        waitPause()

        assertTrue(page.hasEntry())
        assertEquals(1, page.props("install_entry_shown").size)
    }

    @Test
    fun `a player with saves sees the entry at once`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true, install = chrome()).seed("foo", solved(4)).start()

        assertFalse(page.game.tutorial.running)
        assertTrue(page.hasEntry())
        assertEquals(1, page.props("install_entry_shown").size)
    }

    @Test
    fun `a tutorial replay hides the entry and it comes back`() = webTest {
        val page = ready(chrome())
        assertTrue(page.hasEntry())

        page.game.tutorial.onReplay("button")
        assertTrue(page.game.tutorial.running)
        assertFalse(page.hasEntry())

        page.game.tutorial.onSkip()
        assertTrue(page.hasEntry())
        assertEquals(1, page.props("install_entry_shown").size)
    }

    @Test
    fun `no entry in a browser that can't install`() = webTest {
        val page = ready(FakeAppInstall())

        assertFalse(page.hasEntry())
        assertEquals(emptyList(), page.installEvents())
    }

    @Test
    fun `no entry in the installed app's own window`() = webTest {
        val page = ready(chrome(standalone = true))

        assertFalse(page.hasEntry())
        assertEquals("standalone", page.ownProps("app_launched").single()["display_mode"])
    }

    @Test
    fun `a browser launch reports display_mode browser`() = webTest {
        val page = ready(chrome())

        assertEquals("browser", page.ownProps("app_launched").single()["display_mode"])
    }

    @Test
    fun `the kill switch removes the entry`() = webTest {
        val install = chrome()
        val page = ready(install, installOn = false)

        assertFalse(page.hasEntry())
        // Not listening, so the page never takes the browser's offer over with preventDefault().
        assertFalse(install.listening)
        assertEquals(emptyList(), page.installEvents())

        assertFalse(ready(FakeAppInstall(SAFARI_IPHONE), installOn = false).hasEntry(), "Safari's steps too")
    }

    @Test
    fun `the update notice takes the entry's place`() = webTest {
        val page = ready(chrome())
        assertTrue(page.hasEntry())

        page.updates.ready()

        val notice = texts().page.footerUpdateNotice("9.9.9") + texts().page.footerUpdate
        assertEquals(notice, page.footerText())
        assertFalse(page.hasEntry())
        assertEquals(1, page.props("install_entry_shown").size)
    }

    @Test
    fun `the entry replaces the credit on a phone`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_IPHONE), layout = Layout.TALL)

        val page0 = texts().page
        assertEquals("${page0.footerVersion("9.9.9")}${page0.footerInstall}", page.state.footer.filterNot { it.wideOnly }.joinToString("") { it.text })
        assertTrue(page.state.footer.filter { it is FooterPart.Link }.all { it.wideOnly })
        assertEquals(
            "v9.9.9 · ${page0.footerCreditLead}Cary Huang (carykh) · ${page0.footerOriginal} · loopover.xyz · ${page0.footerInstall}",
            page.footerText(),
        )
    }

    // The browser's prompt
    @Test
    fun `a click opens the browser's install prompt`() = webTest {
        val install = chrome()
        val page = ready(install)

        page.game.install.onInstall(byPointer = true)

        assertTrue(install.promptOpen)
        assertEquals(1, install.prompts)
        assertNull(page.state.dialog)
        assertEquals(listOf(mapOf("method" to "browser_prompt")), page.ownProps("install_entry_clicked"))
        assertEquals("rich_ui", page.props("install_entry_clicked").single()["input_method"])
        assertTrue(page.props("install_prompt_result").isEmpty(), "not until the prompt is answered")
    }

    @Test
    fun `an accepted prompt installs and hides the entry`() = webTest {
        val install = chrome()
        val page = ready(install)
        page.game.install.onInstall(byPointer = false)

        install.answer(true)
        install.installedByBrowser()

        assertEquals(listOf(mapOf("outcome" to "accepted")), page.ownProps("install_prompt_result"))
        assertEquals(listOf(mapOf("via_entry" to true)), page.ownProps("app_installed"))
        assertFalse(page.hasEntry())
    }

    @Test
    fun `appinstalled before the prompt's answer still counts as through the entry`() = webTest {
        val install = chrome()
        val page = ready(install)
        page.game.install.onInstall(byPointer = false)

        install.installedByBrowser()
        install.answer(true)

        assertEquals(listOf(mapOf("via_entry" to true)), page.ownProps("app_installed"))
        assertFalse(page.hasEntry())
    }

    @Test
    fun `a dismissed prompt hides the entry until the browser offers again`() = webTest {
        val install = chrome()
        val page = ready(install)
        page.game.install.onInstall(byPointer = false)

        install.answer(false)

        assertEquals(listOf(mapOf("outcome" to "dismissed")), page.ownProps("install_prompt_result"))
        assertTrue(page.props("app_installed").isEmpty())
        assertFalse(page.hasEntry())

        install.offer()

        assertTrue(page.hasEntry())
        assertEquals(1, page.props("install_entry_shown").size)
    }

    @Test
    fun `a prompt that couldn't show reports nothing and is spent`() = webTest {
        val install = chrome()
        val page = ready(install)
        page.game.install.onInstall(byPointer = false)

        install.answer(null)

        assertTrue(page.props("install_prompt_result").isEmpty())
        assertFalse(page.hasEntry())
        assertTrue(page.game.idle)
    }

    @Test
    fun `an install from the browser menu hides the entry`() = webTest {
        val install = chrome()
        val page = ready(install)
        assertTrue(page.hasEntry())

        install.installedByBrowser()

        assertFalse(page.hasEntry())
        assertEquals(listOf(mapOf("via_entry" to false)), page.ownProps("app_installed"))
    }

    @Test
    fun `the browser's offer that came before the page was up shows the entry at once`() = webTest {
        val install = chrome(offered = false)
        install.offer()

        val page = ready(install)

        assertTrue(page.hasEntry())
    }

    // Safari
    @Test
    fun `Safari on a phone or tablet opens the steps`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_IPHONE))
        assertEquals(listOf(mapOf("method" to "safari_steps")), page.ownProps("install_entry_shown"))

        page.game.install.onInstall(byPointer = false)

        assertEquals(WebDialog.Install(InstallDevice.PHONE_OR_TABLET), page.state.dialog)
        assertEquals(listOf(mapOf("device" to "phone_or_tablet")), page.ownProps("screen_install_dialog"))
        assertEquals(listOf(mapOf("method" to "safari_steps")), page.ownProps("install_entry_clicked"))
        assertTrue(page.props("install_prompt_result").isEmpty())
    }

    @Test
    fun `Safari on a Mac opens the Mac steps`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_MAC))

        page.game.install.onInstall(byPointer = false)

        assertEquals(WebDialog.Install(InstallDevice.MAC), page.state.dialog)
        assertEquals(listOf(mapOf("device" to "mac")), page.ownProps("screen_install_dialog"))
    }

    @Test
    fun `other iOS browsers show no entry`() = webTest {
        for (signals in listOf(FIREFOX_IPHONE, CHROME_IPHONE)) {
            val page = ready(FakeAppInstall(signals))

            assertFalse(page.hasEntry(), signals.userAgent)
            assertEquals(emptyList(), page.installEvents())
        }
    }

    @Test
    fun `Escape closes the steps and focus returns to the entry`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_MAC))
        val cancelledBefore = page.props("dialog_cancelled").size
        page.game.install.onInstall(byPointer = false)

        page.game.onEscape()

        assertNull(page.state.dialog)
        assertEquals(Focus.InstallLink, page.state.focus)
        assertEquals(cancelledBefore, page.props("dialog_cancelled").size, "nothing is cancelled")
    }

    @Test
    fun `Escape after a mouse click returns to the entry too`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_MAC))
        page.game.install.onInstall(byPointer = true)

        page.game.onEscape()

        assertEquals(Focus.InstallLink, page.state.focus)
    }

    @Test
    fun `Close by a key keeps focus on the entry`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_MAC))
        page.game.install.onInstall(byPointer = false)

        page.game.install.onClosed()

        assertNull(page.state.dialog)
        assertEquals(Focus.InstallLink, page.state.focus)
    }

    @Test
    fun `Close after a mouse click puts focus on the board`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_MAC))
        page.game.install.onInstall(byPointer = true)

        page.game.install.onClosed()

        assertNull(page.state.dialog)
        assertEquals(Focus.Board, page.state.focus)
        assertTrue(page.state.boardFocusable)
    }

    @Test
    fun `on touch a tap on the entry keeps focus on it`() = webTest {
        val page = WebHarness(backgroundScope, touch = true, install = FakeAppInstall(SAFARI_IPHONE)).start().also { it.game.onSizeChosen(null) }
        page.game.install.onInstall(byPointer = true)

        page.game.install.onClosed()

        assertEquals(Focus.InstallLink, page.state.focus)
    }

    @Test
    fun `the update notice appearing under the open dialog doesn't lose focus`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_MAC))
        page.game.install.onInstall(byPointer = false)

        page.updates.ready()
        page.game.install.onClosed()

        assertFalse(page.hasEntry())
        val focus = checkNotNull(page.state.focus)
        assertEquals(Focus.InstallLink, focus)
        assertEquals(Focus.UpdateLink, fallbackFocus(focus.key, page.state))
    }

    @Test
    fun `a click while a dialog is open does nothing`() = webTest {
        val page = ready(FakeAppInstall(SAFARI_MAC))
        page.game.onNew()
        assertTrue(page.state.dialog is WebDialog.Size)

        page.game.install.onInstall(byPointer = true)

        assertTrue(page.state.dialog is WebDialog.Size)
        assertTrue(page.props("install_entry_clicked").isEmpty())
    }
}
