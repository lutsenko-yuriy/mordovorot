package view

/**
 * Thrown by [View.processCommand] when there's no more input to read. Kept
 * distinct from [java.io.EOFException] so a truncated save-file read (GH-6's
 * `load`) isn't mistaken for "stop the game".
 */
class EndOfInputException(cause: Throwable? = null) : Exception(cause)
