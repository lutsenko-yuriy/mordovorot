import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import storage.FilePreferenceRepository
import storage.FileSaveRepository
import testing.FakeMordieSearchFactory
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.DialogLayout
import view.tui.Direction
import view.tui.HitTarget
import view.tui.TerminalEvent
import view.tui.TerminalSize

/** GH-160 scenarios: 6x6 boards in the terminal, mouse and keyboard mode. Real BoardImpl,
 *  FileSaveRepository and FilePreferenceRepository over one temp dir, through a GameSession over a
 *  FakeTerminal on 80x24, as in TileLabelsScenarioTest. */
class SixBySixScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 24)
    private fun mouseLayout(side: Int) = BoardLayout(terminalSize, side, modeButtons = listOf(InputMode.KEYBOARD))

    private val sitsOut = "Mordie: 6x6 is too much for me, you're on your own!"
    private val dim = "\u001B[2m"

    /** 6x6, every tile out of place. */
    private val scrambled6 = IntArray(36) { 35 - it }

    /** 6x6, row 0 rotated right once - a left shift on row 0 solves it. */
    private val oneShiftFromSolved6 = intArrayOf(5, 0, 1, 2, 3, 4) + IntArray(30) { it + 6 }

    private val lettersWarning = "Letters don't fit a 6x6 board (26 letters, 36 tiles) - pick other labels."
    private val sixLettersSave = "six" to "6\n${scrambled6.joinToString(" ")}\n0\n\n\n\n\nletters\n"

    private sealed interface Step
    private class Event(val event: TerminalEvent) : Step
    private class Act(val block: () -> Unit) : Step

    private class Run(val terminal: FakeTerminal, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties - "input_method" }
        fun event(name: String) = events(name).single()
    }

    private lateinit var terminal: FakeTerminal
    private val snaps = mutableListOf<String>()
    private val factory = FakeMordieSearchFactory()

    private fun session(
        steps: List<Step>,
        mode: InputMode = InputMode.MOUSE,
        dir: Path = dir(),
        initial: IntArray = scrambled6,
    ): Run = runBlocking {
        val events = mutableListOf<TerminalEvent>()
        val acts = mutableMapOf<Int, () -> Unit>()
        for (step in steps) {
            when (step) {
                is Event -> events += step.event
                is Act -> acts.merge(events.size, step.block) { a, b -> { a(); b() } }
            }
        }
        val terminal = FakeTerminal(events, terminalSize, acts).also { this@SixBySixScenarioTest.terminal = it }
        val analytics = RecordingAnalyticsService()
        val side = generateSequence(3) { it + 1 }.first { it * it == initial.size }
        testGameSession(
            initialMode = mode,
            board = BoardImpl(side).apply { restoreState(initial) },
            saves = FileSaveRepository(dir),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
            mordie = Mordie(factory),
            preferences = FilePreferenceRepository(dir),
        ).run()
        Run(terminal, analytics)
    }

    private fun dir(preference: String? = null, vararg saves: Pair<String, String>): Path =
        createTempDirectory("mordovorot-gh160").also { dir ->
            preference?.let { dir.resolve("preferences").writeText("tile_labels=$it\n") }
            for ((name, content) in saves) dir.resolve("$name.save").writeText(content)
        }

    // --- steps ---

    private fun snap() = Act { snaps += terminal.frames.last() }
    private fun key(event: TerminalEvent) = Event(event)
    private fun click(p: Pair<Int, Int>?) = Event(TerminalEvent.MouseClick(p!!.first, p.second))

    private fun clickButton(dialog: DialogLayout, id: String): Step {
        val button = dialog.buttons().first { it.target == HitTarget.DialogButton(id) }
        return Event(TerminalEvent.MouseClick(button.x, dialog.buttonsRow()))
    }

    private val labelsButtons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel"))
    private fun labelsDialog(message: String? = null) =
        DialogLayout(Dialog(Dialog.Kind.LABELS, "Tile labels", message = message, listItems = List(6) { "" }, buttons = labelsButtons), terminalSize)

    private fun row(dialog: DialogLayout, index: Int) = Event(TerminalEvent.MouseClick(dialog.left + 2, dialog.listRowPosition(index)))

    private val sizeDialog = DialogLayout(
        Dialog(
            Dialog.Kind.SIZE, "New game size",
            buttons = (3..6).map { DialogButtonSpec(it.toString(), "${it}x$it") } + DialogButtonSpec("cancel", "Cancel"),
        ),
        terminalSize,
    )

    /** `[ New ]` on a [from]-sided board, then [to]. */
    private fun newGame(from: Int, to: Int) = listOf(click(mouseLayout(from).newButtonPosition()), clickButton(sizeDialog, "$to"))

    private fun save(side: Int, name: String): List<Step> =
        listOf(click(mouseLayout(side).saveButtonPosition())) + name.map { key(TerminalEvent.KeyPress(it)) } + key(TerminalEvent.Enter)

    /** `[ Load ]` on a [side]-sided board, a snap of the list, then the first of [listed]. */
    private fun load(side: Int, vararg listed: String): List<Step> {
        val dialog = DialogLayout(
            Dialog(
                Dialog.Kind.LOAD, "Load game", listItems = listed.toList(), selectedIndex = 0,
                buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
            ),
            terminalSize,
        )
        return listOf(click(mouseLayout(side).loadButtonPosition()), snap(), clickButton(dialog, "load"))
    }

    private fun shiftUp(count: Int) = List(count) { click(mouseLayout(6).upArrowPosition(0)) }

    // --- reading a frame ---

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
    private fun text(frame: String) = rows(frame).joinToString("\n")

    private fun tilesShown(frame: String, side: Int = 6): List<String> {
        val layout = BoardLayout(terminalSize, side)
        val (left, top) = layout.gridOrigin()
        return (0 until side).flatMap { row ->
            rows(frame)[top + 1 + 2 * row].substring(left, left + layout.gridWidth).split("│").filter { it.isNotBlank() }.map { it.trim() }
        }
    }

    private fun mordieLine(frame: String) = rows(frame)[mouseLayout(6).movesRow + 1].trim()

    /** The dialog's message, its wrapped lines joined back up. */
    private fun messageShown(frame: String, dialog: DialogLayout) =
        dialog.messageLines.indices.joinToString(" ") { rows(frame)[dialog.messageRowPosition(it)].substring(dialog.left, dialog.left + dialog.width).trim() }

    private fun preferenceIn(dir: Path) = dir.resolve("preferences").readText()

    // --- scenarios ---

    @Test
    fun `the size picker offers 6x6 and starts a 6x6 game`() {
        val run = session(
            listOf(click(mouseLayout(4).newButtonPosition()), snap(), clickButton(sizeDialog, "6"), snap()),
            initial = IntArray(16) { 15 - it },
        )

        assertTrue("[ 3x3 ] [ 4x4 ] [ 5x5 ] [ 6x6 ] [ Cancel ]" in text(snaps[0]), text(snaps[0]))
        assertEquals((1..36).map { "$it" }, tilesShown(snaps[1]).sortedBy { it.toInt() })
        assertEquals(mapOf("size" to 6, "trigger" to "toolbar"), run.event("new_game_size_selected"))
    }

    @Test
    fun `--size=6 launches straight into 6x6`() {
        val args = arrayOf("--size=6")
        val analytics = RecordingAnalyticsService()
        val size = resolveBoardSize(args)!!
        resolveInputMode(args, analytics, boardSize = size, hasInteractiveTerminal = { true })

        session(listOf(snap()), initial = BoardImpl(size).boardArray)

        assertEquals(6, analytics.events.single { it.name == "app_launched" }.properties["board_size"])
        assertEquals(36, tilesShown(snaps[0]).size)
    }

    @Test
    fun `Mordie sits 6x6 out - its line for shifts 0-4, then nothing, and no search`() {
        session(listOf(snap()) + shiftUp(4) + snap() + shiftUp(1) + snap() + shiftUp(20) + snap())

        assertEquals(sitsOut, mordieLine(snaps[0]))
        assertEquals(sitsOut, mordieLine(snaps[1]))
        assertEquals("", mordieLine(snaps[2]))
        assertEquals("", mordieLine(snaps[3]))
        assertTrue(factory.started.isEmpty())
        for (frame in snaps) assertFalse("Give Up" in text(frame), "Give Up hidden")
    }

    @Test
    fun `solving a 6x6 - Congratulations, no Mordie result`() {
        val run = session(listOf(click(mouseLayout(6).leftArrowPosition(0)), snap()), initial = oneShiftFromSolved6)

        val frame = text(snaps[0])
        assertTrue("Congratulations ✓" in frame, frame)
        assertFalse("Give Up" in frame || "Mordie's solution" in frame, frame)
        assertEquals(mapOf("move_count" to 1, "board_size" to 6, "tile_labels" to "numbers"), run.event("board_solved"))
    }

    @Test
    fun `a 6x6 save lists as 6x6 and loads with the sit-out line`() {
        val dir = dir()
        session(shiftUp(3) + snap() + save(6, "six") + newGame(6, 4) + load(4, "six (6x6)") + snap(), dir = dir)

        assertTrue("six (6x6)" in text(snaps[1]), text(snaps[1]))
        assertEquals(tilesShown(snaps[0]), tilesShown(snaps[2]))
        assertTrue("Moves: 1" in text(snaps[2]), text(snaps[2]))
        assertEquals(sitsOut, mordieLine(snaps[2]))
        assertEquals(listOf(16), factory.started.map { it.original.size }, "only the 4x4 game's search")
        assertTrue(dir.resolve("six.save").readLines().subList(3, 7).all { it.isEmpty() }, "no Mordie lines")
    }

    @Test
    fun `6x6 fits an 80x24 terminal in both modes`() {
        session(save(6, "fit") + snap())
        session(listOf(snap()), InputMode.KEYBOARD)

        for (frame in snaps) {
            assertEquals(24, rows(frame).size)
            assertEquals(sitsOut, mordieLine(frame))
            assertEquals(36, tilesShown(frame).size)
            assertFalse("[ Give Up ]" in text(frame) || "[Give Up F8]" in text(frame))
        }
        val mouse = text(snaps[0])
        assertTrue("[ New ]" in mouse && "[ Labels ]" in mouse && "Saved as 'fit'." in mouse, mouse)
        val keyboard = text(snaps[1])
        assertTrue("[New F9]" in keyboard && "[Labels F4]" in keyboard && "F4 Labels" in keyboard, keyboard)
    }

    @Test
    fun `on 6x6 Letters is dimmed, and a Letters preference shows Numbers`() {
        val dir = dir("letters")
        val dialog = labelsDialog()
        val run = session(
            listOf(snap(), click(mouseLayout(6).labelsButtonPosition()), snap(), row(dialog, 4), snap(), clickButton(dialog, "ok")) +
                newGame(6, 4) + snap(),
            dir = dir,
        )

        assertEquals((1..36).map { "$it" }, tilesShown(snaps[0]).sortedBy { it.toInt() })
        assertTrue("● Numbers" in text(snaps[1]) && "○ Letters          up to 5x5" in text(snaps[1]), text(snaps[1]))
        assertTrue("$dim○ Letters" in snaps[1])
        assertTrue("● Numbers" in text(snaps[2]) && "● Letters" !in text(snaps[2]), "the click marks nothing")
        assertTrue(run.events("tile_labels_changed").isEmpty())
        assertEquals("tile_labels=letters\n", preferenceIn(dir))
        assertTrue(tilesShown(snaps[3], side = 4).all { it.length == 1 && it[0] in 'A'..'P' }, "${tilesShown(snaps[3], side = 4)}")

        // Keyboard: four steps from Numbers skip Letters and land on Letter pairs, by arrow or Tab.
        for (step in listOf(TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Tab)) {
            val keys = dir("letters")
            session(listOf(key(TerminalEvent.FunctionKey(4))) + List(4) { key(step) } + key(TerminalEvent.Enter), InputMode.KEYBOARD, keys)
            assertEquals("tile_labels=letter_pairs\n", preferenceIn(keys), "$step")
        }
    }

    @Test
    fun `a 6x6 save with Letters warns, and the picked labels win`() {
        val dir = dir(null, sixLettersSave)
        val dialog = labelsDialog(lettersWarning)
        val run = session(
            load(4, "six (6x6)") + snap() + row(dialog, 2) + clickButton(dialog, "ok") + snap(),
            dir = dir,
            initial = IntArray(16) { 15 - it },
        )

        assertEquals(lettersWarning, messageShown(snaps[1], dialog))
        assertTrue("● Numbers" in text(snaps[1]), text(snaps[1]))
        assertTrue("$dim○ Letters" in snaps[1])
        assertEquals("letters_save", run.events("screen_labels_dialog").single()["opened_from"])
        assertEquals("66", tilesShown(snaps[2])[0])
        assertEquals("tile_labels=pairs\n", preferenceIn(dir))
        assertEquals("letters_unavailable", run.event("tile_labels_changed")["from_source"])
        assertTrue("Loaded 'six'." in text(snaps[2]), text(snaps[2]))
    }

    @Test
    fun `cancelling the Letters warning keeps Numbers, and the next save drops Letters`() {
        val dir = dir("pairs", sixLettersSave)
        val dialog = labelsDialog(lettersWarning)
        val run = session(
            load(4, "six (6x6)") + clickButton(dialog, "cancel") + snap() + click(mouseLayout(6).saveButtonPosition()) +
                key(TerminalEvent.Enter),
            dir = dir,
            initial = IntArray(16) { 15 - it },
        )

        assertEquals("36", tilesShown(snaps[1])[0])
        assertEquals(listOf<Any?>("labels"), run.events("dialog_cancelled").map { it["dialog"] })
        assertEquals("tile_labels=pairs\n", preferenceIn(dir))
        assertEquals("numbers", dir.resolve("six.save").readLines()[7])
    }
}
