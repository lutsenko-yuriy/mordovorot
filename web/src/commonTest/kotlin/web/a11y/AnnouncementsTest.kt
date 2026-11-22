package web.a11y

import texts.Language
import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.Direction
import viewmodel.TileLabels
import viewmodel.SolutionStep

class AnnouncementsTest {
    private val reader = texts().reader


    @Test
    fun `a line is named from 1, with its direction`() {
        assertEquals(reader.rowLeft(2), Announcements.line(Direction.LEFT, 1))
        assertEquals(reader.rowRight(1), Announcements.line(Direction.RIGHT, 0))
        assertEquals(reader.columnUp(4), Announcements.line(Direction.UP, 3))
        assertEquals(reader.columnDown(3), Announcements.line(Direction.DOWN, 2))
    }

    @Test
    fun `an arrow shift adds the move count only when it starts a move`() {
        assertEquals(reader.rowLeft(2), Announcements.shift(Direction.LEFT, 1, newMove = null))
        assertEquals(reader.withMoves(reader.rowLeft(2), 1), Announcements.shift(Direction.LEFT, 1, newMove = 1))
        assertEquals(reader.withMoves(reader.columnUp(4), 5), Announcements.shift(Direction.UP, 3, newMove = 5))
    }

    @Test
    fun `a drag says how far, either way`() {
        assertEquals(reader.dragged(reader.rowLeft(1), 2), Announcements.drag(Direction.LEFT, 0, 2, newMove = null))
        assertEquals(reader.withMoves(reader.dragged(reader.rowRight(3), 1), 2), Announcements.drag(Direction.RIGHT, 2, 1, newMove = 2))
        assertEquals(reader.dragged(reader.columnUp(2), 3), Announcements.drag(Direction.UP, 1, 3, newMove = null))
        assertEquals(reader.dragged(reader.columnDown(1), 2), Announcements.drag(Direction.DOWN, 0, 2, newMove = null))
    }

    @Test
    fun `the win, a new game and the solution screen`() {
        assertEquals(reader.solved(1), Announcements.solved(1))
        assertEquals(reader.solved(23), Announcements.solved(23))
        assertEquals(reader.newGame(4), Announcements.newGame(4))
        assertEquals(reader.solution(12), Announcements.solution(12))
        assertEquals(reader.solution(1), Announcements.solution(1))
    }

    @Test
    fun `a load says the save's name, size and moves, or that it's solved`() {
        assertEquals(reader.loadedPlain("foo", 5, 12), Announcements.loaded("foo", 5, 12, solved = false))
        assertEquals(reader.loadedPlain("foo", 3, 1), Announcements.loaded("foo", 3, 1, solved = false))
        assertEquals(reader.loadedSolved("done", 4, 14), Announcements.loaded("done", 4, 14, solved = true))
        assertEquals(reader.loadedSolution("sol", 4, 12), Announcements.loaded("sol", 4, 0, solved = false, solutionSteps = 12))
    }

    @Test
    fun `a solution step says its number and its shift in words, or the start`() {
        val steps = listOf(
            SolutionStep(isRow = true, index = 0, forward = false, count = 1),
            SolutionStep(isRow = false, index = 2, forward = false, count = 2),
            SolutionStep(isRow = true, index = 1, forward = true, count = 1),
            SolutionStep(isRow = false, index = 0, forward = true, count = 3),
        )

        assertEquals(reader.stepStart(4), Announcements.step(steps, current = null))
        assertEquals(reader.stepOf(1, 4, reader.rowLeft(1), 1), Announcements.step(steps, current = 0))
        assertEquals(reader.stepOf(2, 4, reader.columnUp(3), 2), Announcements.step(steps, current = 1))
        assertEquals(reader.stepOf(3, 4, reader.rowRight(2), 1), Announcements.step(steps, current = 2))
        assertEquals(reader.stepOf(4, 4, reader.columnDown(1), 3), Announcements.step(steps, current = 3))
    }

    @Test
    fun `a board read says each row from 1, then how many tiles are in place`() {
        val texts = listOf("5", "2", "9", "1", "4", "6", "7", "8", "3")
        val home = listOf(false, true, false, false, false, true, true, true, false)
        assertEquals("${reader.boardRow(1, "5, 2, 9")} ${reader.boardRow(2, "1, 4, 6")} ${reader.boardRow(3, "7, 8, 3")} ${reader.tilesInPlace(4)}", Announcements.board(texts, home, 3))
    }

    @Test
    fun `a board read counts 1 tile and 0 tiles, and says letters phonetically`() {
        val texts = listOf("B", "A", "D", "C")
        assertEquals("${reader.boardRow(1, "bravo, alpha")} ${reader.boardRow(2, "delta, charlie")} ${reader.tilesInPlace(0)}", Announcements.board(texts, List(4) { false }, 2))
        assertEquals("${reader.boardRow(1, "bravo, alpha")} ${reader.boardRow(2, "delta, charlie")} ${reader.tilesInPlace(1)}", Announcements.board(texts, listOf(false, false, false, true), 2))
    }

    @Test
    fun `the solution's board is named after its step`() {
        val steps = listOf(SolutionStep(isRow = false, index = 2, forward = false, count = 2), SolutionStep(isRow = true, index = 0, forward = true, count = 1))
        assertEquals(reader.boardStep(reader.stepStart(2)), Announcements.boardStep(steps, null))
        assertEquals(reader.boardStep(reader.stepOf(1, 2, reader.columnUp(3), 2)), Announcements.boardStep(steps, 0))
    }

    @Test
    fun `a keyboard shift's words end with the tile now under the selection, as said`() {
        assertEquals(reader.withNow(reader.withMoves(reader.rowLeft(2), 5), "charlie"), Announcements.withNow(reader.withMoves(reader.rowLeft(2), 5), "C"))
        assertEquals(reader.withNow(reader.rowLeft(2), "7"), Announcements.withNow(reader.rowLeft(2), "7"))
    }

    @Test
    fun `a label is spoken letter by letter in the phonetic alphabet, numbers as they are`() {
        assertEquals("alpha", Announcements.spoken("A"))
        assertEquals("bravo charlie", Announcements.spoken("BC"))
        assertEquals("juliet yankee", Announcements.spoken("JY"))
        assertEquals("x-ray zulu", Announcements.spoken("XZ"))
        assertEquals("12", Announcements.spoken("12"))
        assertEquals("0", Announcements.spoken("0"))
    }

    @Test
    fun `a Cyrillic label is spoken with the Russian spelling words (GH-313)`() {
        assertEquals("Анна", Announcements.spoken("А"))
        assertEquals("Анна Борис", Announcements.spoken("АБ"))
        assertEquals("Яков Юрий", Announcements.spoken("ЯЮ"))
        assertEquals("Щука Эхо", Announcements.spoken("ЩЭ"))
    }

    @Test
    fun `a Greek label is spoken with the letter names, and the question mark by its own (GH-322)`() {
        assertEquals("alpha", Announcements.spoken("Α"))
        assertEquals("gamma alpha", Announcements.spoken("ΓΑ"))
        assertEquals("omega xi", Announcements.spoken("ΩΞ"))
        assertEquals("Greek question mark", Announcements.spoken("\u037E"))
    }

    @Test
    fun `the pre-reform letters the Cyrillic scheme lacks are said by their names (GH-354)`() {
        assertEquals("Иван краткий", Announcements.spoken("Й"))
        assertEquals("и десятеричное", Announcements.spoken("\u0406"))
        assertEquals("твёрдый знак", Announcements.spoken("Ъ"))
        assertEquals("еры", Announcements.spoken("Ы"))
        assertEquals("мягкий знак", Announcements.spoken("Ь"))
        assertEquals("ять", Announcements.spoken("Ѣ"))
        assertEquals("фита", Announcements.spoken("Ѳ"))
        assertEquals("ижица", Announcements.spoken("Ѵ"))
    }

    @Test
    fun `a pre-reform pair of shared letters is said as a Cyrillic one`() {
        assertEquals("Анна Борис", Announcements.spoken("АБ"))
    }

    @Test
    fun `the 28 shared letters are said alike by both Cyrillic word lists, in every language (GH-354)`() {
        for (language in Language.entries) {
            val reader = language.texts.reader
            for (letter in TileLabels.CYRILLIC_ALPHABET) {
                assertEquals(
                    reader.cyrillicPhonetic[TileLabels.CYRILLIC_ALPHABET.indexOf(letter)],
                    reader.prereformPhonetic[TileLabels.PREREFORM_ALPHABET.indexOf(letter)],
                    "$letter ${language.code}",
                )
            }
        }
    }

    @Test
    fun `a label mixing scripts is said as it is`() {
        assertEquals("AБ", Announcements.spoken("AБ"))
    }
}
