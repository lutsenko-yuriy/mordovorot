package offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfflineCacheTest {

    private val scope = "https://lutsenko-yuriy.github.io/mordovorot/"
    private val files = setOf("index.html", "web.js", "styles.css", "mordie-worker.js")

    private fun key(url: String) = OfflineCache.cacheKeyOf(url, scope, files)

    @Test
    fun `the scope's root is the index page`() {
        assertEquals("index.html", key(scope))
    }

    @Test
    fun `index html is the index page`() {
        assertEquals("index.html", key("${scope}index.html"))
    }

    @Test
    fun `a query or a fragment does not change the page`() {
        assertEquals("index.html", key("$scope?board=abc"))
        assertEquals("index.html", key("${scope}index.html?x=1#top"))
        assertEquals("web.js", key("${scope}web.js?v=2"))
    }

    @Test
    fun `a listed file maps to itself`() {
        assertEquals("web.js", key("${scope}web.js"))
        assertEquals("mordie-worker.js", key("${scope}mordie-worker.js"))
    }

    @Test
    fun `the source map and other unlisted paths go to the network`() {
        assertNull(key("${scope}web.js.map"))
        assertNull(key("${scope}nothing.png"))
        assertNull(key("${scope}sub/web.js"))
    }

    @Test
    fun `the worker's own file is not served from the cache`() {
        assertNull(key("${scope}offline-worker.js"))
    }

    @Test
    fun `another origin or another path goes to the network`() {
        assertNull(key("https://example.com/mordovorot/web.js"))
        assertNull(key("https://lutsenko-yuriy.github.io/other/web.js"))
        assertNull(key("https://lutsenko-yuriy.github.io/mordovorot"))
    }

    @Test
    fun `an index page missing from the list is not invented`() {
        assertNull(OfflineCache.cacheKeyOf(scope, scope, setOf("web.js")))
    }

    @Test
    fun `stale caches are the older mordovorot ones`() {
        val names = listOf("mordovorot-0.40.0-aaaaaaaaaaaa", "mordovorot-0.40.1-bbbbbbbbbbbb")

        assertEquals(listOf("mordovorot-0.40.0-aaaaaaaaaaaa"), OfflineCache.staleCaches(names, "mordovorot-0.40.1-bbbbbbbbbbbb"))
    }

    @Test
    fun `caches with other names are left alone`() {
        val names = listOf("workbox-precache", "other-site", "mordovorot-0.40.0-aaaaaaaaaaaa")

        assertEquals(listOf("mordovorot-0.40.0-aaaaaaaaaaaa"), OfflineCache.staleCaches(names, "mordovorot-0.41.0-cccccccccccc"))
    }

    @Test
    fun `nothing is stale when only the current cache exists`() {
        assertEquals(emptyList(), OfflineCache.staleCaches(listOf("mordovorot-0.40.0-aaaaaaaaaaaa"), "mordovorot-0.40.0-aaaaaaaaaaaa"))
    }
}
