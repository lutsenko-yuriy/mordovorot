package web

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-111: the page's hostname, bucketed for `app_launched.host`. */
class HostTest {

    @Test
    fun `the public site, in any case, is PUBLIC_SITE`() {
        assertEquals(Host.PUBLIC_SITE, hostOf("lutsenko-yuriy.github.io"))
        assertEquals(Host.PUBLIC_SITE, hostOf("Lutsenko-Yuriy.GitHub.io"))
    }

    @Test
    fun `loopback names and addresses are LOCALHOST`() {
        for (hostname in listOf("localhost", "LOCALHOST", "127.0.0.1", "::1", "[::1]")) {
            assertEquals(Host.LOCALHOST, hostOf(hostname), hostname)
        }
    }

    @Test
    fun `everything else is OTHER`() {
        for (hostname in listOf("someone.github.io", "lutsenko-yuriy.github.io.evil.com", "192.168.1.5", "")) {
            assertEquals(Host.OTHER, hostOf(hostname), hostname)
        }
    }
}
