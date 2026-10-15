import kotlin.test.Test

/** GH-67 scenarios: console `help` command, startup hint and specific error messages. Real
 *  BoardImpl + ViewModelImpl + FileSaveRepository (temp dir) driven through the console view via
 *  GameSession, like [SolvedConsoleScenarioTest]; 4x4 with the size chosen at launch. */
class ConsoleHelpScenarioTest {

    @Test
    fun `startup prints the help hint right before the first board`() {
        // TODO: 1. Run a session with no input (immediate EOF)
        // TODO: 2. Assert output starts with "Enter help to list all the commands", then the first board
        // TODO: 3. Assert output does not contain "Commands:"
    }

    @Test
    fun `help lists every command in place of the board`() {
        // TODO: 1. Input "help", then "left 1"
        // TODO: 2. Assert the output after the first board is exactly the approved 4x4 command list, with no board after it
        // TODO: 3. Assert the board and "Moves: 1" are printed after "left 1"
        // TODO: 4. Assert help_command_used fired with scope=all
    }

    @Test
    fun `help with a command shows only that command's line`() {
        // TODO: 1. Input "help left", then "help quit"
        // TODO: 2. Assert "help left" prints only "  left <row 1-4>       - shift a row left"
        // TODO: 3. Assert "help quit" prints only the "exit, quit" line
        // TODO: 4. Assert help_command_used fired twice: scope=command with command=left, then command=exit
    }

    @Test
    fun `help with an unknown topic shows the full list`() {
        // TODO: 1. Input "help lft"
        // TODO: 2. Assert the output is the full list, with no "Unknown command" line
        // TODO: 3. Assert help_command_used fired with scope=unknown_topic, no command property, and "lft" in no property
    }

    @Test
    fun `unknown command shows the error and hint without the board`() {
        // TODO: 1. Input "lft 1"
        // TODO: 2. Assert "Unknown command: lft" then the hint are printed, with no board after them
        // TODO: 3. Assert console_command_rejected fired with reason=unknown_command, no command property, and "lft" in no property
    }

    @Test
    fun `empty line shows Empty command and the hint`() {
        // TODO: 1. Input an empty line
        // TODO: 2. Assert "Empty command" then the hint are printed, with no board after them
        // TODO: 3. Assert console_command_rejected fired with reason=empty
    }

    @Test
    fun `misused known commands show the problem, the usage line and the hint`() {
        // TODO: 1. Input "left", "left x", "left 9", "reset now", "size 9", one per line
        // TODO: 2. Assert in order, each followed by the hint and no board:
        //          "Missing row number for left. Usage: left <row 1-4>"
        //          "Row must be a number for left. Usage: left <row 1-4>"
        //          "Row 9 is out of range for left. Usage: left <row 1-4>"
        //          "Too many arguments for reset. Usage: reset"
        //          "Size 9 is out of range for size. Usage: size <3-5>"
        // TODO: 3. Assert the board is unchanged, at "Moves: 0"
        // TODO: 4. Assert console_command_rejected fired five times: reasons missing_argument, not_a_number,
        //          out_of_range, too_many_arguments, out_of_range; commands left, left, left, reset, size
    }

    @Test
    fun `usage ranges follow the current board size`() {
        // TODO: 1. Input "size 5", "help left", then "left 9"
        // TODO: 2. Assert "help left" prints "left <row 1-5>"
        // TODO: 3. Assert the error is "Row 9 is out of range for left. Usage: left <row 1-5>"
    }

    @Test
    fun `loading an unknown save keeps its existing message without the hint`() {
        // TODO: 1. Input "load nope"
        // TODO: 2. Assert the existing "no save named" message is printed without the hint
        // TODO: 3. Assert console_command_rejected did not fire
    }
}
