package view

/** A rejected console command (GH-67): [message] is shown followed by the help hint, and
 *  [reason]/[command] feed `console_command_rejected`. [command] is the canonical name of a known
 *  command, never the raw typed verb - free text may carry PII. Extends
 *  [IllegalArgumentException] so callers expecting the old "Incorrect input" type still match. */
internal class ConsoleCommandException(
    message: String,
    val reason: String,
    val command: String? = null,
) : IllegalArgumentException(message)
