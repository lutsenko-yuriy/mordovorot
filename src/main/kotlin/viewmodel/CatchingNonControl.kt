package viewmodel

import kotlinx.coroutines.CancellationException

/** Runs [block], passing any ordinary [Exception] to [onError]; [SessionControlException] and
 *  [CancellationException] are always rethrown. Inline, so [block] may suspend. */
inline fun <T> catchingNonControl(onError: (Exception) -> T, block: () -> T): T =
    try {
        block()
    } catch (e: SessionControlException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(e)
    }
