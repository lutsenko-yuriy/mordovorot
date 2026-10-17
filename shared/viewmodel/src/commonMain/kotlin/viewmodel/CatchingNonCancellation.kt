package viewmodel

import kotlinx.coroutines.CancellationException

/** Runs [block], passing any ordinary [Exception] to [onError]; [CancellationException] is always
 *  rethrown. The shared flows' catch-all (GH-41): no session-control exception passes through
 *  them, so it doesn't need to know about one. Inline, so [block] may suspend. */
internal inline fun <T> catchingNonCancellation(onError: (Exception) -> T, block: () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(e)
    }
