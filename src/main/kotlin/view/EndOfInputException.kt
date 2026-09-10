package view

/**
 * Thrown by [View.processCommand] when there is no more input to read (stdin
 * closed/exhausted). Deliberately distinct from [java.io.EOFException]: later
 * GH-6 work reads save files inside `processCommand()` (`load <name>`), and
 * file APIs (`DataInputStream`, `ObjectInputStream`, `java.util.zip`, ...)
 * throw `EOFException` on a truncated file by contract. If `PresenterImpl`
 * treated any `EOFException` as "stop the game", a corrupt save file would
 * silently exit the whole session instead of surfacing a recoverable error.
 */
class EndOfInputException(cause: Throwable? = null) : Exception(cause)
