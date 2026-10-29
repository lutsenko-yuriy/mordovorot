import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import storage.FileSaveRepository
import testing.FakeMordieSearchFactory
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.switchModeKeys
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.Dialog
import view.tui.Direction
import view.tui.DialogButtonSpec
import view.tui.DialogLayout
import view.tui.HitTarget
import view.tui.TerminalEvent
import view.tui.TerminalSize
import viewmodel.GiveUpButton
import viewmodel.SolutionState

/** GH-126 scenarios: giving up and Mordie's solution screen in the terminal. Real BoardImpl +
 *  FileSaveRepository (temp dir) through a GameSession over a FakeTerminal, with
 *  `Mordie(FakeMordieSearchFactory())` scripting the search and its solution, like
 *  [MordieScenarioTest]. The replay is stepped by hand with Prev / Next / Restart. */
class GiveUpScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val layout = BoardLayout(terminalSize, 4)

    /** 4x4 two moves from solved: row 0 right once, then column 1 down twice. */
    private val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val afterFirstStep = intArrayOf(0, 9, 2, 3, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
    private val afterOneColumnShift = intArrayOf(0, 5, 2, 3, 4, 9, 6, 7, 8, 13, 10, 11, 12, 1, 14, 15)
    private val solved = IntArray(16) { it }
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
    private val steps = listOf("1. Row 1 → ×1", "2. Col 2 ↓ ×2")
    private val solutionLine = "Mordie: Here's how I did it in 2 moves!"

    /** Twelve one-shift steps - more than a 4x4's nine step rows. Never checked against a board. */
    private val longPath = List(12) { SolutionMove(isRow = it % 2 == 0, index = it % 4, amount = 1) }

    private val factory = FakeMordieSearchFactory()

    /** One step of a scripted session: a terminal event, or an [Act] run between events. */
    private sealed interface Step
    private class Event(val event: TerminalEvent) : Step
    private class Act(val block: () -> Unit) : Step

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
        fun event(name: String) = events(name).single()
        val lastFrame get() = terminal.frames.last()
    }

    /** The terminal of the session running now, for [snap]. */
    private lateinit var terminal: FakeTerminal

    private val snaps = mutableListOf<String>()

    private fun session(
        steps: List<Step>,
        mode: InputMode = InputMode.MOUSE,
        savesDir: Path = savesDir(),
        sizeChosenAtLaunch: Boolean = true,
    ): Run = runBlocking {
        val events = mutableListOf<TerminalEvent>()
        val acts = mutableMapOf<Int, () -> Unit>()
        for (step in steps) {
            when (step) {
                is Event -> events += step.event
                is Act -> acts.merge(events.size, step.block) { a, b -> { a(); b() } }
            }
        }
        val terminal = FakeTerminal(events, terminalSize, acts).also { this@GiveUpScenarioTest.terminal = it }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(original) }
        testGameSession(
            initialMode = mode,
            board = board,
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = sizeChosenAtLaunch,
            terminalFactory = { terminal },
            mordie = Mordie(factory),
        ).run()
        Run(terminal, board, analytics)
    }

    private fun savesDir(vararg files: Pair<String, String>): Path = createTempDirectory("mordovorot-gh126").also { dir ->
        for ((name, content) in files) dir.resolve("$name.save").writeText(content)
    }

    // --- steps ---

    private fun snap() = Act { snaps += terminal.frames.last() }
    private fun click(p: Pair<Int, Int>?) = Event(TerminalEvent.MouseClick(p!!.first, p.second))
    private fun key(event: TerminalEvent) = Event(event)
    private fun keys(vararg events: TerminalEvent) = events.map(::key)

    /** [events], then a [snap]. */
    private fun step(vararg events: TerminalEvent): List<Step> = keys(*events) + snap()

    private fun searchFinds(solution: List<SolutionMove> = path) =
        Act { factory.latest.progress = SearchProgress(solution.size, found = true, elapsedMs = 0, solution = solution) }

    /** Mordie finds [solution], then five shifts of column 3 (one move) reveal it. */
    private fun reveal(solution: List<SolutionMove> = path): List<Step> = listOf(searchFinds(solution)) + List(5) { click(layout.upArrowPosition(3)) }

    private fun giveUpButton(button: GiveUpButton = GiveUpButton.GIVE_UP) =
        click(BoardLayout(terminalSize, 4, giveUp = button).giveUpButtonPosition())

    private val confirm = Dialog(
        Dialog.Kind.GIVE_UP, "Give up and see Mordie's solution?",
        buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("cancel", "No")),
    )

    private fun clickButton(dialog: Dialog, id: String): Step {
        val dialogLayout = DialogLayout(dialog, terminalSize)
        val button = dialogLayout.buttons().first { it.target == HitTarget.DialogButton(id) }
        return Event(TerminalEvent.MouseClick(button.x, dialogLayout.buttonsRow()))
    }

    /** Reveal, then Give Up and Yes. */
    private fun givenUp(solution: List<SolutionMove> = path) = reveal(solution) + giveUpButton() + clickButton(confirm, "yes")

    /** Prev / Next / Restart - their positions don't depend on which are live. */
    private val replayButtons = BoardLayout(terminalSize, 4, solution = SolutionState(emptyList(), null, atStart = false, atEnd = false, moves = emptyList()))
        .replayButtons().map { click(it.x to it.y) }
    private val prev = replayButtons[0]
    private val next = replayButtons[1]
    private val restart = replayButtons[2]

    /** Toolbar Save, type [name], Enter. */
    private fun save(name: String): List<Step> =
        listOf(click(layout.saveButtonPosition())) + name.map { key(TerminalEvent.KeyPress(it)) } + key(TerminalEvent.Enter)

    /** Toolbar Load, then the dialog's Load button with the first of [listed] selected. */
    private fun load(vararg listed: String): List<Step> {
        val dialog = Dialog(
            Dialog.Kind.LOAD, "Load game", listItems = listed.toList(), selectedIndex = 0,
            buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
        )
        return listOf(click(layout.loadButtonPosition()), clickButton(dialog, "load"))
    }

    /** Toolbar New, then the size dialog's 4x4. */
    private fun newGame(): List<Step> {
        val dialog = Dialog(
            Dialog.Kind.SIZE, "New game size",
            buttons = (3..5).map { DialogButtonSpec(it.toString(), "${it}x$it") } + DialogButtonSpec("cancel", "Cancel"),
        )
        return listOf(click(layout.newButtonPosition()), clickButton(dialog, "4"))
    }

    // --- reading a frame ---

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")

    private fun movesShown(frame: String) = rows(frame)[layout.movesRow].trim()

    private fun mordieShown(frame: String) =
        rows(frame).subList(layout.mordieRow, layout.mordieRow + layout.mordieRows).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    private fun giveUpRowShown(frame: String) = rows(frame)[layout.giveUpRow].trim()

    private fun dimmed(frame: String, text: String) = "\u001B[2m$text\u001B[22m" in frame

    private fun stepsShown(frame: String) =
        rows(frame).subList(layout.stepListTop, layout.stepListTop + layout.stepRows).map { it.drop(layout.stepListX).trim() }.filter { it.isNotEmpty() }

    private fun highlighted(frame: String) = Regex("\u001B\\[7m(.*?)\u001B\\[27m").findAll(frame).map { it.groupValues[1] }.toList()

    /** The Prev / Next / Restart row, on the down-arrows row. */
    private fun replayRowShown(frame: String) = rows(frame)[layout.downArrowPosition(0).second].trim()
    private val replayRow = "[ ◀ Prev ] [ Next ▶ ] [ ↻ Restart ]"

    private fun arrowsShown(frame: String) = layout.leftArrowPosition(0).let { (x, y) -> rows(frame)[y][x] == '◀' }

    /** The tiles drawn in the grid, 0-based like the board. */
    private fun tilesShown(frame: String): List<Int> {
        val (left, top) = layout.gridOrigin()
        return (0 until 4).flatMap { row ->
            rows(frame)[top + 1 + 2 * row].substring(left, left + layout.gridWidth).split("│").filter { it.isNotBlank() }.map { it.trim().toInt() - 1 }
        }
    }

    // --- scenarios ---

    @Test
    fun `Give Up is dimmed and inert until Mordie finds its solution`() {
        val run = session(listOf(snap(), giveUpButton(), snap()) + reveal() + snap())

        assertEquals("[ Give Up ]", giveUpRowShown(snaps[0]))
        assertTrue(dimmed(snaps[0], "[ Give Up ]"))
        assertFalse("Give up and see" in snaps[1])
        assertTrue(run.events("screen_give_up_dialog").isEmpty())
        assertTrue(mordieShown(snaps[2]).startsWith("Mordie: Found it!"))
        assertEquals("[ Give Up ]", giveUpRowShown(snaps[2]))
        assertFalse(dimmed(snaps[2], "[ Give Up ]"))
    }

    @Test
    fun `No keeps the game and Yes opens the solution screen`() {
        val run = session(
            reveal() + click(layout.leftArrowPosition(2)) + giveUpButton() + snap() + clickButton(confirm, "cancel") + snap() +
                giveUpButton() + clickButton(confirm, "yes") + snap() + click(layout.leftArrowPosition(0)) +
                click(layout.gridOrigin().let { (x, y) -> x + 2 to y + 1 }) + snap(),
        )

        assertTrue("Give up and see Mordie's solution?" in snaps[0])
        assertEquals(listOf(4, 2), run.events("screen_give_up_dialog").first().let { listOf(it["board_size"], it["move_count"]) })
        assertEquals("Moves: 2", movesShown(snaps[1]))
        assertFalse("Give up and see" in snaps[1])
        assertEquals("give_up", run.event("dialog_cancelled")["dialog"])

        for (frame in listOf(snaps[2], snaps[3])) {
            assertEquals(original.toList(), tilesShown(frame))
            assertEquals("", movesShown(frame))
            assertEquals(solutionLine, mordieShown(frame))
            assertEquals(steps, stepsShown(frame))
            assertEquals(replayRow, replayRowShown(frame))
            assertTrue(dimmed(frame, "[ ◀ Prev ]") && dimmed(frame, "[ ↻ Restart ]") && !dimmed(frame, "[ Next ▶ ]"))
            assertEquals("", giveUpRowShown(frame), "no Give Up on the solution screen")
            assertFalse(arrowsShown(frame))
        }
        assertContentEquals(original, run.board.boardArray)
        assertEquals(0, run.board.moveCount)

        val givenUp = run.event("game_given_up")
        assertEquals(2, givenUp["move_count"])
        assertEquals(2, givenUp["solution_length"])
        assertEquals(1, givenUp["moves_since_solution_found"])
        assertEquals(1, givenUp["shifts_since_solution_found"])
        assertEquals("give_up", run.event("screen_solution")["opened_from"])
    }

    @Test
    fun `Next, Prev and Restart step through Mordie's solution one shift at a time`() {
        val run = session(
            givenUp() + next + snap() + next + snap() + prev + snap() + next + next + snap() + next + snap() + restart + snap(),
        )

        assertEquals(afterFirstStep.toList(), tilesShown(snaps[0]))
        assertEquals(listOf(steps[0]), highlighted(snaps[0]))
        assertFalse(dimmed(snaps[0], "[ ◀ Prev ]") || dimmed(snaps[0], "[ ↻ Restart ]"))
        assertEquals(false, run.event("solution_replay_started")["restart"])

        assertEquals(afterOneColumnShift.toList(), tilesShown(snaps[1]))
        assertEquals(listOf(steps[1]), highlighted(snaps[1]))
        assertEquals(afterFirstStep.toList(), tilesShown(snaps[2]))
        assertEquals(listOf(steps[0]), highlighted(snaps[2]))

        // The end: Next is dimmed and does nothing.
        assertEquals(solved.toList(), tilesShown(snaps[3]))
        assertEquals(listOf(steps[1]), highlighted(snaps[3]))
        assertTrue(dimmed(snaps[3], "[ Next ▶ ]"))
        assertEquals(snaps[3], snaps[4])
        assertEquals(true, run.event("solution_replay_finished")["stepped_back"])

        assertEquals(original.toList(), tilesShown(snaps[5]))
        assertTrue(highlighted(snaps[5]).isEmpty())
        assertContentEquals(original, run.board.boardArray)
        assertTrue(run.events("board_solved").isEmpty())
    }

    @Test
    fun `a long step list is zero-padded, follows the current step and scrolls with the wheel`() {
        val wheel = { up: Boolean -> key(TerminalEvent.Wheel(up)) }
        session(
            givenUp(longPath) + snap() + List(10) { next } + snap() + wheel(true) + snap() +
                List(5) { wheel(false) } + snap(),
        )

        assertEquals((1..9).map { "%02d. ".format(it) }, stepsShown(snaps[0]).map { it.take(4) })
        assertEquals(1, stepsShown(snaps[0]).map { it.length }.toSet().size)
        assertEquals("02.", stepsShown(snaps[1]).first().take(3))
        assertEquals("10.", highlighted(snaps[1]).single().take(3))
        assertEquals("01.", stepsShown(snaps[2]).first().take(3))
        assertEquals("04.", stepsShown(snaps[3]).first().take(3))
    }

    @Test
    fun `keyboard mode - 5 gives up, arrows A D Space Enter step, R restarts, W and S scroll the steps`() {
        val run = session(
            keys(TerminalEvent.KeyPress('5')) + snap() + searchFinds(longPath) + keys(*Array(5) { TerminalEvent.KeyPress('h') }) +
                keys(TerminalEvent.KeyPress('5'), TerminalEvent.Escape, TerminalEvent.KeyPress('5'), TerminalEvent.Enter) + snap() +
                step(TerminalEvent.Arrow(Direction.RIGHT), TerminalEvent.KeyPress('d'), TerminalEvent.KeyPress(' '), TerminalEvent.Enter) +
                step(TerminalEvent.Arrow(Direction.LEFT), TerminalEvent.KeyPress('a')) +
                step(TerminalEvent.KeyPress('r')) +
                step(TerminalEvent.KeyPress('s'), TerminalEvent.Arrow(Direction.DOWN)) +
                step(TerminalEvent.KeyPress('w'), TerminalEvent.Arrow(Direction.UP)),
            mode = InputMode.KEYBOARD,
        )

        assertTrue("5 Give Up" in rows(snaps[0]).joinToString("\n"))
        assertFalse("Give up and see" in snaps[0])
        assertEquals("give_up", run.event("dialog_cancelled")["dialog"])
        assertEquals("", giveUpRowShown(snaps[1]))
        assertEquals(replayRow, replayRowShown(snaps[1]))
        assertTrue(rows(snaps[1]).any { "←/→ A/D H/L  step" in it })
        assertEquals("04.", highlighted(snaps[2]).single().take(3))
        assertEquals("02.", highlighted(snaps[3]).single().take(3))
        assertTrue(highlighted(snaps[4]).isEmpty())
        assertEquals("03.", stepsShown(snaps[5]).first().take(3))
        assertEquals("01.", stepsShown(snaps[6]).first().take(3))
    }

    @Test
    fun `after a win, Mordie's solution opens the screen without a confirmation`() {
        val solve = listOf(click(layout.rightArrowPosition(0)), click(layout.downArrowPosition(1)), click(layout.downArrowPosition(1)))
        val run = session(listOf(searchFinds()) + solve + snap() + giveUpButton(GiveUpButton.MORDIES_SOLUTION) + snap())

        assertEquals("[ Mordie's solution ]", giveUpRowShown(snaps[0]))
        assertFalse("Give up and see" in snaps[1])
        assertEquals(solutionLine, mordieShown(snaps[1]))
        assertEquals(original.toList(), tilesShown(snaps[1]))
        assertEquals("after_win", run.event("screen_solution")["opened_from"])
        assertTrue(run.events("game_given_up").isEmpty())
    }

    @Test
    fun `after You beat me, the Give Up row is empty`() {
        val solve = listOf(click(layout.rightArrowPosition(0)), click(layout.downArrowPosition(1)), click(layout.downArrowPosition(1)))
        val searching = Act { factory.latest.progress = SearchProgress(0, found = false, elapsedMs = 0) }
        val run = session(listOf(searching) + solve + snap() + giveUpButton() + giveUpButton(GiveUpButton.MORDIES_SOLUTION) + snap())

        assertTrue(rows(snaps[0])[layout.titleRow].trim().startsWith("You beat me!"))
        assertEquals("", giveUpRowShown(snaps[0]))
        assertEquals(snaps[0], snaps[1])
        assertTrue(run.events("screen_give_up_dialog").isEmpty() && run.events("screen_solution").isEmpty())
    }

    @Test
    fun `a solution-screen save reopens the screen via Load and startup restore`() {
        val dir = savesDir()
        val run = session(
            givenUp() + next + next + save("s") + newGame() + snap() + load("s (4x4)") + snap(),
            savesDir = dir,
        )

        assertEquals("Moves: 0", movesShown(snaps[0]))
        assertTrue(arrowsShown(snaps[0]))
        assertEquals(original.toList(), tilesShown(snaps[1]))
        assertEquals(replayRow, replayRowShown(snaps[1]))
        assertTrue(highlighted(snaps[1]).isEmpty())
        assertEquals(listOf("give_up", "load"), run.events("screen_solution").map { it["opened_from"] })

        snaps.clear()
        val restored = session(keys(TerminalEvent.Tab, TerminalEvent.Enter) + snap(), mode = InputMode.KEYBOARD, savesDir = dir, sizeChosenAtLaunch = false)

        assertEquals(steps, stepsShown(snaps[0]))
        assertEquals(original.toList(), tilesShown(snaps[0]))
        assertEquals("startup_prompt", restored.event("screen_solution")["opened_from"])
    }

    @Test
    fun `Load of a regular save leaves the solution screen`() {
        val plain = "4\n${original.joinToString(" ")}\n3\n"
        val run = session(givenUp() + load("plain (4x4)") + snap() + click(layout.leftArrowPosition(2)), savesDir = savesDir("plain" to plain))

        assertEquals("Moves: 3", movesShown(snaps[0]))
        assertTrue(arrowsShown(snaps[0]))
        assertTrue(stepsShown(snaps[0]).isEmpty())
        assertEquals(4, run.board.moveCount)
    }

    @Test
    fun `switching input mode keeps the solution screen and the replay's place`() {
        session(givenUp() + next + switchModeKeys.map(::key) + snap() + key(TerminalEvent.Arrow(Direction.RIGHT)) + snap())

        assertTrue("[Controls 6]" in rows(snaps[0]).joinToString("\n"))
        assertEquals(afterFirstStep.toList(), tilesShown(snaps[0]))
        assertEquals(listOf(steps[0]), highlighted(snaps[0]))
        assertEquals(replayRow, replayRowShown(snaps[0]))
        assertEquals(afterOneColumnShift.toList(), tilesShown(snaps[1]))
    }

    @Test
    fun `a save from before GH-126 with Mordie's solution found keeps Give Up dimmed`() {
        val tiles = original.joinToString(" ")
        val dir = savesDir("old" to "4\n$tiles\n0\n$tiles\n2 found\n")
        val solve = listOf(click(layout.rightArrowPosition(0)), click(layout.downArrowPosition(1)), click(layout.downArrowPosition(1)))

        val run = session(load("old (4x4)") + snap() + giveUpButton() + solve + snap(), savesDir = dir)

        assertTrue(mordieShown(snaps[0]).startsWith("Mordie: Found it!"))
        assertTrue(dimmed(snaps[0], "[ Give Up ]"))
        assertTrue(run.events("screen_give_up_dialog").isEmpty())
        assertContentEquals(solved, run.board.boardArray)
        assertEquals("", giveUpRowShown(snaps[1]))
    }
}
