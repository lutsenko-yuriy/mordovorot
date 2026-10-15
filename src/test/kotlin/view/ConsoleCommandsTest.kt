package view

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ConsoleCommandsTest {

    private val nl = System.lineSeparator()

    @Test
    fun `help text matches the approved 4x4 mockup`() {
        val expected = listOf(
            "Commands:",
            "  left <row 1-4>       - shift a row left",
            "  right <row 1-4>      - shift a row right",
            "  up <column 1-4>      - shift a column up",
            "  down <column 1-4>    - shift a column down",
            "  reset                - reshuffle the board at its current size",
            "  size <3-5>           - start a new game at that board size",
            "  save <name>          - save the game under that name",
            "  load <name>          - load a saved game",
            "  mouse                - switch to the mouse-driven TUI",
            "  keyboard             - switch to the keyboard-only TUI",
            "  exit, quit           - quit, offering to save first",
            "  help [command]       - list all commands, or show one",
        ).joinToString(nl)

        assertEquals(expected, ConsoleCommands.helpText(4))
    }

    @Test
    fun `row and column ranges follow the board side, the size range does not`() {
        val help5 = ConsoleCommands.helpText(5)
        assertEquals(true, help5.contains("  left <row 1-5>       - shift a row left"))
        assertEquals(true, help5.contains("  down <column 1-5>    - shift a column down"))
        assertEquals(true, help5.contains("  size <3-5>           -"))
        assertEquals(true, ConsoleCommands.helpText(3).contains("up <column 1-3>"))
    }

    @Test
    fun `find resolves aliases case-insensitively and rejects unknown verbs`() {
        assertSame(ConsoleCommands.find("exit"), ConsoleCommands.find("quit"))
        assertEquals("left", ConsoleCommands.find("LEFT")?.name)
        assertNull(ConsoleCommands.find("lft"))
    }

    @Test
    fun `usage uses the verb as typed`() {
        assertEquals("quit", ConsoleCommands.usage("quit", 4))
        assertEquals("left <row 1-4>", ConsoleCommands.usage("left", 4))
        assertEquals("help [command]", ConsoleCommands.usage("help", 4))
    }

    @Test
    fun `help line lists every alias`() {
        assertEquals(
            "  exit, quit           - quit, offering to save first",
            ConsoleCommands.helpLine(ConsoleCommands.find("quit")!!, 4),
        )
    }
}
