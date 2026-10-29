import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import storage.FilePreferenceRepository
import storage.FileSaveRepository
import testing.FakeMordieSearchFactory
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.switchModeKeys
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.DialogLayout
import view.tui.Direction
import view.tui.HitTarget
import view.tui.TerminalEvent
import view.tui.TerminalSize
import viewmodel.GiveUpButton

/** GH-139 scenarios: tile labels in the terminal, mouse and keyboard mode. Real BoardImpl,
 *  FileSaveRepository and FilePreferenceRepository over one temp dir, through a GameSession over a
 *  FakeTerminal. */
class TileLabelsScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val mouseLayout = BoardLayout(terminalSize, 4)
    private val keyboardLayout = BoardLayout(terminalSize, 4, toolbarShortcuts = true)

    /** 4x4 with tile 6 at row 2, column 1 (index 9): `7` in Numbers, `23` in Pairs, `G` in Letters. */
    private val scrambled = IntArray(16) { 15 - it }
    private val tile6 = 9

    /** 4x4, row 0 rotated right once - a left shift on row 0 solves it. */
    private val oneShiftFromSolved = intArrayOf(3, 0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    /** The dialog's rows on a 4x4, unmarked. */
    private val dialogRows = listOf(
        "Numbers          1  2  3  4",
        "Numbers from 0   0  1  2  3",
        "Pairs            11 12 13 14",
        "Pairs from 0     00 01 02 03",
        "Letters          A  B  C  D",
        "Letter pairs     AA AB AC AD",
    )
    private val numbers = 0
    private val pairs = 2
    private val letters = 4

    private sealed interface Step
    private class Event(val event: TerminalEvent) : Step
    private class Act(val block: () -> Unit) : Step

    private class Run(val terminal: FakeTerminal, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
        fun event(name: String) = events(name).single()
        val lastFrame get() = terminal.frames.last()
    }

    private lateinit var terminal: FakeTerminal
    private val snaps = mutableListOf<String>()
    private val factory = FakeMordieSearchFactory()

    private fun session(
        steps: List<Step>,
        mode: InputMode = InputMode.MOUSE,
        dir: Path = createTempDirectory("mordovorot-gh139"),
        initial: IntArray = scrambled,
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
        val terminal = FakeTerminal(events, terminalSize, acts).also { this@TileLabelsScenarioTest.terminal = it }
        val analytics = RecordingAnalyticsService()
        testGameSession(
            initialMode = mode,
            board = BoardImpl(initial.size.let { if (it == 9) 3 else 4 }).apply { restoreState(initial) },
            saves = FileSaveRepository(dir),
            analytics = analytics,
            sizeChosenAtLaunch = sizeChosenAtLaunch,
            terminalFactory = { terminal },
            mordie = Mordie(factory),
            preferences = FilePreferenceRepository(dir),
        ).run()
        Run(terminal, analytics)
    }

    private fun dir(preference: String? = null, vararg saves: Pair<String, String>): Path =
        createTempDirectory("mordovorot-gh139").also { dir ->
            preference?.let { dir.resolve("preferences").writeText("tile_labels=$it\n") }
            for ((name, content) in saves) dir.resolve("$name.save").writeText(content)
        }

    // --- steps ---

    private fun snap() = Act { snaps += terminal.frames.last() }
    private fun key(event: TerminalEvent) = Event(event)
    private fun click(p: Pair<Int, Int>?) = Event(TerminalEvent.MouseClick(p!!.first, p.second))

    /** The labels dialog's geometry: its box is the fixed dialog width, whatever its rows say. */
    private val labelsDialog = DialogLayout(
        Dialog(
            Dialog.Kind.LABELS, "Tile labels", listItems = dialogRows,
            buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
        ),
        terminalSize,
    )

    private fun clickButton(dialog: DialogLayout, id: String): Step {
        val button = dialog.buttons().first { it.target == HitTarget.DialogButton(id) }
        return Event(TerminalEvent.MouseClick(button.x, dialog.buttonsRow()))
    }

    private val openLabels = click(mouseLayout.labelsButtonPosition())
    private fun row(index: Int) = Event(TerminalEvent.MouseClick(labelsDialog.left + 2, labelsDialog.listRowPosition(index)))
    private val ok = clickButton(labelsDialog, "ok")
    private val cancel = clickButton(labelsDialog, "cancel")

    /** Mouse: open the dialog, mark row [index], OK. */
    private fun choose(index: Int) = listOf(openLabels, row(index), ok)

    /** Keyboard: F4 (focus starts on the marked row [from]), arrows to [to], Enter. */
    private fun chooseByKeys(from: Int, to: Int): List<Step> {
        val arrow = if (to > from) Direction.DOWN else Direction.UP
        return listOf(key(TerminalEvent.KeyPress('4'))) + List(kotlin.math.abs(to - from)) { key(TerminalEvent.Arrow(arrow)) } +
            key(TerminalEvent.Enter)
    }

    private fun save(name: String): List<Step> =
        listOf(click(mouseLayout.saveButtonPosition())) + name.map { key(TerminalEvent.KeyPress(it)) } + key(TerminalEvent.Enter)

    private fun load(vararg listed: String): List<Step> {
        val dialog = DialogLayout(
            Dialog(
                Dialog.Kind.LOAD, "Load game", listItems = listed.toList(), selectedIndex = 0,
                buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
            ),
            terminalSize,
        )
        return listOf(click(mouseLayout.loadButtonPosition()), clickButton(dialog, "load"))
    }

    private fun newGame(): List<Step> {
        val dialog = DialogLayout(
            Dialog(
                Dialog.Kind.SIZE, "New game size",
                buttons = (3..5).map { DialogButtonSpec(it.toString(), "${it}x$it") } + DialogButtonSpec("cancel", "Cancel"),
            ),
            terminalSize,
        )
        return listOf(click(mouseLayout.newButtonPosition()), clickButton(dialog, "4"))
    }

    // --- reading a frame ---

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
    private fun text(frame: String) = rows(frame).joinToString("\n")
    private fun secondRowShown(frame: String, layout: BoardLayout) = rows(frame)[layout.modeRow].trim()

    /** The tiles' text in the grid, row by row. */
    private fun tilesShown(frame: String, side: Int = 4): List<String> {
        val layout = BoardLayout(terminalSize, side)
        val (left, top) = layout.gridOrigin()
        return (0 until side).flatMap { row ->
            rows(frame)[top + 1 + 2 * row].substring(left, left + layout.gridWidth).split("│").filter { it.isNotBlank() }.map { it.trim() }
        }
    }

    private fun stepsShown(frame: String) =
        rows(frame).subList(mouseLayout.stepListTop, mouseLayout.stepListTop + mouseLayout.stepRows)
            .map { it.drop(mouseLayout.stepListX).trim() }.filter { it.isNotEmpty() }

    private fun dialogShown(frame: String, marked: Int): Boolean =
        "Tile labels" in frame && dialogRows.withIndex().all { (i, row) -> "${if (i == marked) "●" else "○"} $row" in text(frame) }

    // --- scenarios ---

    @Test
    fun `labels button opens the dialog with numbers marked`() {
        for ((mode, layout, open) in listOf(
            Triple(InputMode.MOUSE, mouseLayout, openLabels),
            Triple(InputMode.KEYBOARD, keyboardLayout, key(TerminalEvent.KeyPress('4'))),
        )) {
            snaps.clear()
            val run = session(listOf(snap(), open, snap()), mode)

            val expectedRow = if (mode == InputMode.MOUSE) "[ Labels ] [ Controls 6 ]" else "[Labels 4] [Controls 6]"
            assertEquals(expectedRow, secondRowShown(snaps[0], layout))
            assertTrue(dialogShown(snaps[1], marked = numbers), "$mode: the dialog, Numbers marked")
            val opened = run.event("screen_labels_dialog")
            assertEquals(if (mode == InputMode.MOUSE) "toolbar" else "shortcut", opened["opened_from"])
            assertEquals("play", opened["screen"])
            assertEquals("numbers", opened["current_labels"])
        }
    }

    @Test
    fun `choosing pairs relabels the board`() {
        for ((mode, steps) in listOf(InputMode.MOUSE to choose(pairs), InputMode.KEYBOARD to chooseByKeys(numbers, pairs))) {
            snaps.clear()
            val run = session(listOf(snap()) + steps + snap(), mode)

            assertEquals("7", tilesShown(snaps[0])[tile6])
            assertEquals("23", tilesShown(snaps[1])[tile6], "$mode")
            val changed = run.event("tile_labels_changed")
            assertEquals("numbers", changed["from"])
            assertEquals("pairs", changed["to"])
            assertEquals("default", changed["from_source"])
            assertEquals("play", changed["screen"])
            assertEquals(4, changed["board_size"])
        }
    }

    @Test
    fun `cancel and escape change nothing`() {
        val mouse = session(listOf(openLabels, row(letters), cancel, snap(), openLabels, ok, snap()))
        val keyboard = session(
            chooseByKeys(numbers, letters).dropLast(1) + key(TerminalEvent.Escape) + snap() + chooseByKeys(numbers, numbers) + snap(),
            InputMode.KEYBOARD,
        )

        for (run in listOf(mouse, keyboard)) {
            assertEquals(listOf<Any?>("labels"), run.events("dialog_cancelled").map { it["dialog"] })
            assertTrue(run.events("tile_labels_changed").isEmpty(), "OK on the current scheme changes nothing")
        }
        for (frame in snaps) assertEquals("7", tilesShown(frame)[tile6])
    }

    @Test
    fun `labels survive a mode switch, a new game and a relaunch`() {
        val dir = dir()
        // Keyboard mode's New game dialog: focus starts on 3x3, Tab to 4x4.
        session(
            choose(letters) + switchModeKeys.map(::key) + snap() +
                key(TerminalEvent.KeyPress('1')) + key(TerminalEvent.Tab) + key(TerminalEvent.Enter) + snap(),
            dir = dir,
        )
        session(listOf(snap()), dir = dir)

        assertEquals("G", tilesShown(snaps[0])[tile6], "after the mode switch")
        assertTrue(tilesShown(snaps[1]).all { it.length == 1 && it[0] in 'A'..'P' }, "after a new game: ${tilesShown(snaps[1])}")
        assertEquals("G", tilesShown(snaps[2])[tile6], "after a relaunch")
        // app_launched.tile_labels is Main's, covered by InputModeTest.
    }

    @Test
    fun `a save keeps its labels for that game only`() {
        val run = session(
            choose(letters) + save("foo") + choose(pairs) + load("foo (4x4)") + snap() + newGame() + snap() +
                load("foo (4x4)") + choose(numbers),
        )

        assertEquals("G", tilesShown(snaps[0])[tile6])
        assertEquals(true, run.events("load_command_used").first()["labels_differ_from_preference"])
        assertTrue(tilesShown(snaps[1]).all { it.length == 2 && it.all(Char::isDigit) }, "a new game takes the preference: ${tilesShown(snaps[1])}")
        assertEquals("save", run.events("tile_labels_changed").last()["from_source"])
    }

    @Test
    fun `startup restore and repair keep the save's labels`() {
        val restoreAtStartup = listOf(key(TerminalEvent.Tab), key(TerminalEvent.Enter))
        val lettersSave = "foo" to "4\n${scrambled.joinToString(" ")}\n0\n\n\n\n\nletters\n"
        session(restoreAtStartup + snap(), InputMode.KEYBOARD, dir("pairs", lettersSave), sizeChosenAtLaunch = false)

        val oddDir = dir("pairs", "odd" to "3\n2 1 0 3 4 5 6 7 8\n5\n\n\n\n\nletters\n")
        session(restoreAtStartup + key(TerminalEvent.Enter) + snap(), InputMode.KEYBOARD, oddDir, sizeChosenAtLaunch = false)

        assertEquals("G", tilesShown(snaps[0])[tile6])
        assertEquals(listOf("B", "C", "A", "D", "E", "F", "G", "H", "I"), tilesShown(snaps[1], side = 3))
        assertEquals("letters", oddDir.resolve("odd.save").readLines()[7])
    }

    @Test
    fun `an older save loads with the preference`() {
        val run = session(load("old (4x4)") + snap(), dir = dir("pairs", "old" to "4\n${scrambled.joinToString(" ")}\n0\n"))

        assertEquals("23", tilesShown(snaps[0])[tile6])
        assertEquals(false, run.event("load_command_used")["labels_differ_from_preference"])
    }

    @Test
    fun `labels on the solved board`() {
        val run = session(
            listOf(click(mouseLayout.leftArrowPosition(0)), snap(), openLabels, cancel),
            dir = dir("pairs"),
            initial = oneShiftFromSolved,
        )

        assertEquals("pairs", run.event("board_solved")["tile_labels"])
        assertEquals("11", tilesShown(snaps[0])[0])
        assertEquals("solved", run.event("screen_labels_dialog")["screen"])
    }

    @Test
    fun `labels on the solution screen`() {
        // GH-126's give-up flow: 4x4 two moves from solved; Mordie finds the path, five shifts of
        // column 3 reveal it, then Give Up and Yes.
        val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
        val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
        val found = Act { factory.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path) }
        val giveUp = BoardLayout(terminalSize, 4, giveUp = GiveUpButton.GIVE_UP).giveUpButtonPosition()
        val confirm = DialogLayout(
            Dialog(Dialog.Kind.GIVE_UP, "Give up and see Mordie's solution?", buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("cancel", "No"))),
            terminalSize,
        )
        val run = session(
            listOf(found) + List(5) { click(mouseLayout.upArrowPosition(3)) } + click(giveUp) + clickButton(confirm, "yes") +
                choose(letters) + snap(),
            initial = original,
        )

        assertEquals(listOf("J", "C", "D", "A"), tilesShown(snaps[0]).take(4))
        assertEquals(listOf("1. Row 1 → ×1", "2. Col 2 ↓ ×2"), stepsShown(snaps[0]))
        assertEquals("solution", run.event("tile_labels_changed")["screen"])
        assertEquals("solution", run.event("screen_labels_dialog")["screen"])
    }
}
