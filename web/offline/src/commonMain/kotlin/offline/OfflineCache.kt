package offline

/** The offline worker's decisions, kept apart from the browser's APIs so the JVM and Node.js can test them (GH-262). */
object OfflineCache {
    const val NAME_PREFIX = "mordovorot-"

    /** What the page sends the waiting worker to take over; nothing else activates it. */
    const val SKIP_WAITING = "skip-waiting"

    private const val INDEX = "index.html"

    /** The cached file that answers [url], or null when the request goes to the network. */
    fun cacheKeyOf(url: String, scope: String, files: Set<String>): String? {
        if (!url.startsWith(scope)) return null
        val path = url.removePrefix(scope).substringBefore('#').substringBefore('?')
        val key = if (path.isEmpty()) INDEX else path
        return key.takeIf { it in files }
    }

    /** The caches of older versions; caches another app made are not ours to delete. */
    fun staleCaches(names: List<String>, current: String): List<String> =
        names.filter { it.startsWith(NAME_PREFIX) && it != current }
}
