package offline

import kotlin.js.Promise
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array

// dynamic: the standard library's service worker declarations lack what this needs.
private external val self: dynamic

private val scope: String get() = self.registration.scope as String

private fun inCurrentCache(): dynamic {
    val options: dynamic = js("({})")
    options.cacheName = OfflineManifest.CACHE
    return options
}

private fun promise(value: dynamic): Promise<dynamic> = value.unsafeCast<Promise<dynamic>>()

private fun Promise<dynamic>.andThen(next: (dynamic) -> dynamic): Promise<dynamic> = then(next)

private fun urlOf(path: String) = scope + path

/** The worker's entry point (GH-262): caches the whole game on install and answers from the cache. */
fun main() {
    self.addEventListener("install", { event: dynamic -> event.waitUntil(install()) })
    // No clients.claim(): a page that loaded from the network keeps its network files.
    self.addEventListener("activate", { event: dynamic -> event.waitUntil(deleteStaleCaches()) })
    self.addEventListener("fetch", { event: dynamic -> answer(event) })
    self.addEventListener("message", { event: dynamic ->
        if (event.data == OfflineCache.SKIP_WAITING) self.skipWaiting()
    })
}

/** Caches every listed file, or fails: a version is installed only when all of it is here. */
private fun install(): Promise<dynamic> =
    promise(self.caches.open(OfflineManifest.CACHE)).andThen { cache ->
        val stored = OfflineManifest.FILES.map { (path, hash) ->
            fetchVerified(path, hash).andThen { response -> cache.put(urlOf(path), response) }
        }
        Promise.all(stored.toTypedArray())
    }

/** The file from the network, never from the HTTP cache or a CDN copy older than its hash. */
private fun fetchVerified(path: String, hash: String): Promise<dynamic> =
    promise(self.fetch(urlOf(path), js("({cache: 'reload'})"))).andThen { response ->
        check(response.ok as Boolean) { "$path: HTTP ${response.status}" }
        promise(response.clone().arrayBuffer()).andThen { bytes ->
            promise(self.crypto.subtle.digest("SHA-256", bytes)).andThen { digest ->
                val actual = hex(Uint8Array(digest.unsafeCast<ArrayBuffer>()))
                check(actual == hash) { "$path: expected $hash, got $actual" }
                response
            }
        }
    }

private fun hex(bytes: Uint8Array): String =
    (0 until bytes.length).joinToString("") { (bytes.asDynamic()[it] as Int).toString(16).padStart(2, '0') }

private fun deleteStaleCaches(): Promise<dynamic> =
    promise(self.caches.keys()).andThen { names ->
        val stale = OfflineCache.staleCaches((names as Array<String>).toList(), OfflineManifest.CACHE)
        Promise.all(stale.map { name -> promise(self.caches.delete(name)) }.toTypedArray())
    }

private fun answer(event: dynamic) {
    if (event.request.method != "GET") return
    val key = OfflineCache.cacheKeyOf(event.request.url as String, scope, OfflineManifest.FILES.keys) ?: return
    event.respondWith(
        promise(self.caches.match(urlOf(key), inCurrentCache())).andThen { cached -> cached ?: self.fetch(event.request) },
    )
}
