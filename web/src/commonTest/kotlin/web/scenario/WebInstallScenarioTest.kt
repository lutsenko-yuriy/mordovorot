package web.scenario

import kotlin.test.Test

/**
 * GH-360 scenarios: the Install entry in the footer, through [WebHarness].
 *
 * Stubs (WU0): each test holds only its steps as TODO comments; WU1 adds `FakeAppInstall`, the
 * harness's install options and the driver code, and makes them green. Texts and analytics
 * properties come from the plan (issue #360) and `docs/ANALYTICS_EVENTS.md`.
 */
class WebInstallScenarioTest {

    @Test
    fun `no entry while the first-launch tutorial runs`() = webTest {
        // TODO: 1. Start with a browser that can install, no saves, tutorial on
        // TODO: 2. The footer has no install entry
        // TODO: 3. The `install_entry_shown` event is not sent
    }

    @Test
    fun `Skip shows the entry`() = webTest {
        // TODO: 1. Start as above
        // TODO: 2. Skip the tutorial
        // TODO: 3. The entry is in the footer
        // TODO: 4. The `install_entry_shown` event is sent once, with `method: browser_prompt`
    }

    @Test
    fun `finishing the tutorial shows the entry`() = webTest {
        // TODO: 1. Play the tutorial through its last step
        // TODO: 2. The entry is in the footer
        // TODO: 3. The `install_entry_shown` event is sent once
    }

    @Test
    fun `a player with saves sees the entry at once`() = webTest {
        // TODO: 1. Seed a save and start
        // TODO: 2. The entry is in the footer
        // TODO: 3. The `install_entry_shown` event is sent
    }

    @Test
    fun `a tutorial replay hides the entry and it comes back`() = webTest {
        // TODO: 1. Start with the entry showing
        // TODO: 2. Start a replay of the tutorial
        // TODO: 3. The entry is gone
        // TODO: 4. End the replay
        // TODO: 5. The entry is back
        // TODO: 6. The `install_entry_shown` event was sent only once
    }

    @Test
    fun `no entry in a browser that can't install`() = webTest {
        // TODO: 1. Start with no install signals
        // TODO: 2. The footer has no install entry
        // TODO: 3. No `install_*` event is sent
    }

    @Test
    fun `no entry in the installed app's own window`() = webTest {
        // TODO: 1. Start in standalone mode
        // TODO: 2. The footer has no install entry
        // TODO: 3. The `app_launched` event has `display_mode: standalone`
    }

    @Test
    fun `a browser launch reports display_mode browser`() = webTest {
        // TODO: 1. Start in a normal browser tab
        // TODO: 2. The `app_launched` event has `display_mode: browser`
    }

    @Test
    fun `the kill switch removes the entry`() = webTest {
        // TODO: 1. Start with the `install` switch off
        // TODO: 2. The footer has no install entry
        // TODO: 3. The page did not call `preventDefault()` on the browser's offer
    }

    @Test
    fun `the update notice takes the entry's place`() = webTest {
        // TODO: 1. Start with the entry showing
        // TODO: 2. A new version becomes ready
        // TODO: 3. The footer shows the update notice
        // TODO: 4. The entry is gone
    }

    @Test
    fun `the entry replaces the credit on a phone`() = webTest {
        // TODO: 1. Start on a phone layout
        // TODO: 2. The footer is the version plus the entry
        // TODO: 3. The credit links are not there
    }

    @Test
    fun `a click opens the browser's install prompt`() = webTest {
        // TODO: 1. Click the entry
        // TODO: 2. The browser's prompt is shown
        // TODO: 3. No dialog of ours opens
        // TODO: 4. The `install_entry_clicked` event is sent
    }

    @Test
    fun `an accepted prompt installs and hides the entry`() = webTest {
        // TODO: 1. Click the entry
        // TODO: 2. The browser reports accepted
        // TODO: 3. The `install_prompt_result` event is sent with `outcome: accepted`
        // TODO: 4. The `app_installed` event is sent with `via_entry: true`
        // TODO: 5. The entry is gone
    }

    @Test
    fun `a dismissed prompt hides the entry until the browser offers again`() = webTest {
        // TODO: 1. Click the entry
        // TODO: 2. The browser reports dismissed
        // TODO: 3. The `install_prompt_result` event is sent with `outcome: dismissed`
        // TODO: 4. The entry is gone
        // TODO: 5. The browser offers the install again
        // TODO: 6. The entry returns
    }

    @Test
    fun `an install from the browser menu hides the entry`() = webTest {
        // TODO: 1. Start with the entry showing
        // TODO: 2. The `appinstalled` event arrives with no click
        // TODO: 3. The entry is gone
        // TODO: 4. The `app_installed` event is sent with `via_entry: false`
    }

    @Test
    fun `Safari on a phone or tablet opens the steps`() = webTest {
        // TODO: 1. Start as Safari on an iPhone
        // TODO: 2. The entry shows, with `method: safari_steps`
        // TODO: 3. Click the entry
        // TODO: 4. The dialog shows the Share and Add to Home Screen steps and the saves line
        // TODO: 5. The `screen_install_dialog` view is sent with `device: phone_or_tablet`
    }

    @Test
    fun `Safari on a Mac opens the Mac steps`() = webTest {
        // TODO: 1. Start as Safari on a Mac
        // TODO: 2. Click the entry
        // TODO: 3. The dialog shows the File and Add to Dock steps
        // TODO: 4. The `screen_install_dialog` view is sent with `device: mac`
    }

    @Test
    fun `other iOS browsers show no entry`() = webTest {
        // TODO: 1. Start as Firefox on an iPhone
        // TODO: 2. The footer has no install entry
        // TODO: 3. Start as Chrome on an iPhone
        // TODO: 4. The footer has no install entry
    }

    @Test
    fun `Escape closes the steps and focus returns to the entry`() = webTest {
        // TODO: 1. Focus the entry and press Enter
        // TODO: 2. The dialog opens
        // TODO: 3. Press Escape
        // TODO: 4. The dialog closes
        // TODO: 5. Focus is on the entry
    }

    @Test
    fun `Close after a mouse click puts focus on the board`() = webTest {
        // TODO: 1. Click the entry with the mouse
        // TODO: 2. Click Close in the dialog
        // TODO: 3. Focus is on the board
        // TODO: 4. A board key works
    }

    @Test
    fun `the update notice appearing under the open dialog doesn't lose focus`() = webTest {
        // TODO: 1. Open the Safari dialog
        // TODO: 2. A new version becomes ready
        // TODO: 3. Close the dialog
        // TODO: 4. Focus is on Update, not the page body
    }
}
