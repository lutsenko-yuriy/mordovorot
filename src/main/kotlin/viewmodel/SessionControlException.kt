package viewmodel

/** Marker for exceptions that unwind `View.play()` as normal control flow (ending or rebuilding
 *  the session), not an error - so a catch-all can rethrow via this one type instead of naming
 *  each concrete subtype (GH-30 audit finding on PR #36). */
sealed class SessionControlException : Exception()
