package viewmodel

import kotlinx.coroutines.CancellationException

/** Runs [block], routing any ordinary [Exception] to [onError] while always rethrowing
 *  [SessionControlException] and [CancellationException] - the catch-all every call site needs,
 *  in one place so none can forget a rethrow (GH-65). Inline, so [block] may suspend. */
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
