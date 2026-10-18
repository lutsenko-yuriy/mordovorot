package web

import kotlin.test.Test

/** GH-111 scenarios: the footer's wording and links. */
class FooterTest {

    @Test
    fun `footer reads version and Loopover credit`() {
        // TODO: build the footer for version 1.2.3
        // TODO: verify its parts' text joined is exactly `v1.2.3 · Based on Loopover by Cary Huang (carykh) · original · loopover.xyz`
    }

    @Test
    fun `footer links point to the original and loopover xyz`() {
        // TODO: build the footer for any version
        // TODO: verify exactly two links: original -> https://openprocessing.org/@carykh/580366, loopover.xyz -> https://loopover.xyz
    }
}
