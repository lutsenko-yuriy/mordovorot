package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** GH-360: which browsers get the Safari steps dialog, over real user agents. */
class InstallDetectionTest {

    private fun device(agent: String, appleTouch: Boolean = false, promptCapable: Boolean = false) =
        safariDevice(InstallSignals(agent, appleTouch, promptCapable))

    private val iphoneSafari = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1"
    private val ipadDesktopMode = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Safari/605.1.15"
    private fun mac(version: Int) = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/$version.1 Safari/605.1.15"

    @Test
    fun `Safari on an iPhone and an iPad gets the phone and tablet steps`() {
        assertEquals(InstallDevice.PHONE_OR_TABLET, device(iphoneSafari, appleTouch = true))
        assertEquals(InstallDevice.PHONE_OR_TABLET, device("Mozilla/5.0 (iPad; CPU OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1", appleTouch = true))
    }

    @Test
    fun `an iPad in desktop mode says Macintosh but has touch, so it gets the phone and tablet steps`() {
        assertEquals(InstallDevice.PHONE_OR_TABLET, device(ipadDesktopMode, appleTouch = true))
    }

    @Test
    fun `Safari 17 and later on a Mac gets the Mac steps`() {
        assertEquals(InstallDevice.MAC, device(mac(17)))
        assertEquals(InstallDevice.MAC, device(mac(18)))
    }

    @Test
    fun `Safari 16 on a Mac has no Add to Dock`() {
        assertNull(device(mac(16)))
    }

    @Test
    fun `a Safari user agent with no version gets nothing on a Mac`() {
        assertNull(device("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Safari/605.1.15"))
    }

    @Test
    fun `other browsers on a Mac get nothing`() {
        val webkit = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)"
        assertNull(device("$webkit Chrome/130.0.0.0 Safari/537.36"))
        assertNull(device("$webkit Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0"))
        assertNull(device("$webkit Chrome/130.0.0.0 Safari/537.36 OPR/115.0.0.0"))
        assertNull(device("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:131.0) Gecko/20100101 Firefox/131.0"))
    }

    @Test
    fun `other browsers on an iPhone get nothing`() {
        val webkit = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko)"
        assertNull(device("$webkit CriOS/130.0.6723.90 Mobile/15E148 Safari/604.1", appleTouch = true))
        assertNull(device("$webkit FxiOS/131.0 Mobile/15E148 Safari/605.1.15", appleTouch = true))
        assertNull(device("$webkit EdgiOS/130.2849.80 Version/18.0 Mobile/15E148 Safari/605.1.15", appleTouch = true))
        assertNull(device("$webkit Version/18.1 Mobile/15E148 Safari/604.1 OPT/5.1.0", appleTouch = true))
        assertNull(device("$webkit Mobile/15E148 Safari/604.1 OPiOS/16.0.15.124050", appleTouch = true))
        assertNull(device("$webkit Version/18.1 Mobile/15E148 Safari/604.1 DuckDuckGo/7", appleTouch = true))
        assertNull(device("$webkit Mobile/15E148 Safari/604.1 GSA/330.0.0.0", appleTouch = true))
        assertNull(device("$webkit Version/17.0 YaBrowser/24.1.4.783.10 Mobile/15E148 Safari/605.1", appleTouch = true))
    }

    @Test
    fun `an in-app view, which has no Safari token, gets nothing`() {
        assertNull(device("Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 [FBAN/FBIOS;FBAV/480.0.0]", appleTouch = true))
        assertNull(device("Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Instagram 350.0.0", appleTouch = true))
    }

    @Test
    fun `Firefox on a computer and Chrome on Android get nothing`() {
        assertNull(device("Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0"))
        assertNull(device("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"))
    }

    @Test
    fun `a browser that can fire beforeinstallprompt is never Safari, whatever its user agent says`() {
        assertNull(device(iphoneSafari, appleTouch = true, promptCapable = true))
        assertNull(device(mac(18), promptCapable = true))
    }
}
