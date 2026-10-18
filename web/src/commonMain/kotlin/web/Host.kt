package web

/** Where the page is served from, as `app_launched.host` reports it (GH-111). Only the bucket
 *  leaves [hostOf], never the hostname itself. */
enum class Host { PUBLIC_SITE, LOCALHOST, OTHER }

private const val PUBLIC_HOSTNAME = "lutsenko-yuriy.github.io"
private val LOOPBACK = setOf("localhost", "127.0.0.1", "::1", "[::1]")

fun hostOf(hostname: String): Host = when (hostname.lowercase()) {
    PUBLIC_HOSTNAME -> Host.PUBLIC_SITE
    in LOOPBACK -> Host.LOCALHOST
    else -> Host.OTHER
}
