package view

import board_model.BoardSize

/** What a console command takes after its verb. [missing] and [label] word the errors, so help
 *  text and error messages share one source (GH-67). */
internal enum class ArgKind(val missing: String, val label: String) {
    NONE("", ""),
    ROW("row number", "Row"),
    COLUMN("column number", "Column"),
    SIZE("board size", "Size"),
    NAME("name", "Name"),
    OPTIONAL_COMMAND("", "");

    fun usage(side: Int): String = when (this) {
        NONE -> ""
        ROW -> "<row 1-$side>"
        COLUMN -> "<column 1-$side>"
        SIZE -> "<${BoardSize.MIN}-${BoardSize.MAX}>"
        NAME -> "<name>"
        OPTIONAL_COMMAND -> "[command]"
    }
}

internal data class ConsoleCommand(val names: List<String>, val arg: ArgKind, val description: String) {
    val name: String get() = names.first()
}

/** The console command table (GH-67): the single source of `help` output and the usage lines
 *  in error messages. Pure - no I/O. */
internal object ConsoleCommands {

    const val HELP_HINT = "Enter help to list all the commands"

    private const val USAGE_COLUMN = 21

    val all: List<ConsoleCommand> = listOf(
        ConsoleCommand(listOf("left"), ArgKind.ROW, "shift a row left"),
        ConsoleCommand(listOf("right"), ArgKind.ROW, "shift a row right"),
        ConsoleCommand(listOf("up"), ArgKind.COLUMN, "shift a column up"),
        ConsoleCommand(listOf("down"), ArgKind.COLUMN, "shift a column down"),
        ConsoleCommand(listOf("reset"), ArgKind.NONE, "reshuffle the board at its current size"),
        ConsoleCommand(listOf("size"), ArgKind.SIZE, "start a new game at that board size"),
        ConsoleCommand(listOf("save"), ArgKind.NAME, "save the game under that name"),
        ConsoleCommand(listOf("load"), ArgKind.NAME, "load a saved game"),
        ConsoleCommand(listOf("mouse"), ArgKind.NONE, "switch to the mouse-driven TUI"),
        ConsoleCommand(listOf("keyboard"), ArgKind.NONE, "switch to the keyboard-only TUI"),
        ConsoleCommand(listOf("exit", "quit"), ArgKind.NONE, "quit, offering to save first"),
        ConsoleCommand(listOf("help"), ArgKind.OPTIONAL_COMMAND, "list all commands, or show one"),
    )

    fun find(verb: String): ConsoleCommand? = verb.lowercase().let { v -> all.firstOrNull { v in it.names } }

    /** [verb] as typed, so `quit now` reports `Usage: quit`. */
    fun usage(verb: String, side: Int): String {
        val arg = find(verb)?.arg?.usage(side).orEmpty()
        return if (arg.isEmpty()) verb else "$verb $arg"
    }

    fun helpLine(command: ConsoleCommand, side: Int): String {
        val arg = command.arg.usage(side)
        val usage = command.names.joinToString(", ") + if (arg.isEmpty()) "" else " $arg"
        return "  ${usage.padEnd(USAGE_COLUMN)}- ${command.description}"
    }

    fun helpText(side: Int): String =
        (listOf("Commands:") + all.map { helpLine(it, side) }).joinToString(System.lineSeparator())
}
