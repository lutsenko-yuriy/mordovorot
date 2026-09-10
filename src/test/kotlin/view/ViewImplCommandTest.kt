package view

import testing.FakePresenter
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.io.Reader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** A [Reader] that always fails, simulating a dead stream (e.g. the controlling
 *  terminal disappearing) rather than a clean end-of-stream. */
private class ThrowingReader : Reader() {
    override fun read(cbuf: CharArray, off: Int, len: Int): Int = throw IOException("stream error")
    override fun close() {}
}

class ViewImplCommandTest {

    private fun viewWith(input: String, presenter: FakePresenter = FakePresenter()): Pair<ViewImpl, ByteArrayOutputStream> {
        val outputBuffer = ByteArrayOutputStream()
        val view = ViewImpl(BufferedReader(StringReader(input)), PrintStream(outputBuffer))
        view.presenter = presenter
        return view to outputBuffer
    }

    @Test
    fun `left command delegates to presenter shiftLeft, translating the 1-based row to 0-based`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("left 1\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftLeft(0)"), presenter.calls)
    }

    @Test
    fun `right command delegates to presenter shiftRight, translating the 1-based row to 0-based`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("right 3\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftRight(2)"), presenter.calls)
    }

    @Test
    fun `up command delegates to presenter shiftUp, translating the 1-based column to 0-based`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("up 2\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftUp(1)"), presenter.calls)
    }

    @Test
    fun `down command delegates to presenter shiftDown, translating the 1-based column to 0-based`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("down 4\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftDown(3)"), presenter.calls)
    }

    @Test
    fun `left 0 translates to -1, delegating to the board's own bounds check rather than validating in view`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("left 0\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftLeft(-1)"), presenter.calls)
    }

    @Test
    fun `Int-MIN_VALUE argument wraps rather than crashing, and still lands outside any valid board range`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("left ${Int.MIN_VALUE}\n", presenter)

        view.processCommand()

        // Int.MIN_VALUE - 1 wraps to Int.MAX_VALUE - still far outside 0 until SQUARE_SIDE.
        assertEquals(listOf("shiftLeft(${Int.MAX_VALUE})"), presenter.calls)
    }

    @Test
    fun `save command delegates to presenter saveGame with the raw file name, untranslated`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("save foo\n", presenter)

        view.processCommand()

        assertEquals(listOf("saveGame(foo)"), presenter.calls)
    }

    @Test
    fun `load command delegates to presenter loadGame with the raw file name, untranslated`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("load foo\n", presenter)

        view.processCommand()

        assertEquals(listOf("loadGame(foo)"), presenter.calls)
    }

    @Test
    fun `save with a missing file name throws without delegating`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("save\n", presenter)

        assertFailsWith<IllegalArgumentException> { view.processCommand() }
        assertEquals(emptyList(), presenter.calls)
    }

    @Test
    fun `save with a trailing extra token is rejected rather than silently ignored`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("save foo bar\n", presenter)

        assertFailsWith<IllegalArgumentException> { view.processCommand() }
        assertEquals(emptyList(), presenter.calls)
    }

    @Test
    fun `reset command delegates to presenter resetGame`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("reset\n", presenter)

        view.processCommand()

        assertEquals(listOf("resetGame"), presenter.calls)
    }

    @Test
    fun `commands are case-insensitive`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("LEFT 1\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftLeft(0)"), presenter.calls)
    }

    @Test
    fun `missing numeric argument throws without delegating`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("left\n", presenter)

        assertFailsWith<IllegalArgumentException> { view.processCommand() }
        assertEquals(emptyList(), presenter.calls)
    }

    @Test
    fun `non-numeric argument throws without delegating`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("left abc\n", presenter)

        assertFailsWith<IllegalArgumentException> { view.processCommand() }
        assertEquals(emptyList(), presenter.calls)
    }

    @Test
    fun `unknown command throws without delegating`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("teleport\n", presenter)

        assertFailsWith<IllegalArgumentException> { view.processCommand() }
        assertEquals(emptyList(), presenter.calls)
    }

    @Test
    fun `EOF throws EndOfInputException instead of looping forever - regression guard for GH-4's 651ca64`() {
        val (view, _) = viewWith("")

        assertFailsWith<EndOfInputException> { view.processCommand() }
    }

    @Test
    fun `trailing extra token is rejected rather than silently ignored`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("left 1 99\n", presenter)

        assertFailsWith<IllegalArgumentException> { view.processCommand() }
        assertEquals(emptyList(), presenter.calls)
    }

    @Test
    fun `reset with a trailing argument is rejected rather than silently ignored`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("reset foo\n", presenter)

        assertFailsWith<IllegalArgumentException> { view.processCommand() }
        assertEquals(emptyList(), presenter.calls)
    }

    @Test
    fun `displayBoard renders a tab-separated grid, offset to 1-based tile values`() {
        val (view, output) = viewWith("")

        view.displayBoard(intArrayOf(0, 1, 2, 3), 2)

        val nl = System.lineSeparator()
        assertEquals("1\t2\t${nl}3\t4\t$nl$nl", output.toString())
    }

    @Test
    fun `showMessage writes to the injected output`() {
        val (view, output) = viewWith("")

        view.showMessage("save not found")

        assertEquals("save not found${System.lineSeparator()}", output.toString())
    }

    @Test
    fun `IOException while reading input throws EndOfInputException instead of spinning forever`() {
        val view = ViewImpl(BufferedReader(ThrowingReader()), PrintStream(ByteArrayOutputStream()))
        view.presenter = FakePresenter()

        assertFailsWith<EndOfInputException> { view.processCommand() }
    }

    @Test
    fun `confirmRestore returns true for y or yes, case-insensitively`() {
        for (answer in listOf("y", "Y", "yes", "YES", "Yes")) {
            val (view, _) = viewWith("$answer\n")
            assertEquals(true, view.confirmRestore("foo"), "expected '$answer' to confirm")
        }
    }

    @Test
    fun `confirmRestore returns false for a blank line, a no, or garbage input`() {
        for (answer in listOf("", "n", "no", "blah")) {
            val (view, _) = viewWith("$answer\n")
            assertEquals(false, view.confirmRestore("foo"), "expected '$answer' to decline")
        }
    }

    @Test
    fun `confirmRestore returns false on EOF instead of throwing`() {
        val (view, _) = viewWith("")

        assertEquals(false, view.confirmRestore("foo"))
    }

    @Test
    fun `confirmRestore returns false when the input stream is dead, same as clean EOF`() {
        val view = ViewImpl(BufferedReader(ThrowingReader()), PrintStream(ByteArrayOutputStream()))
        view.presenter = FakePresenter()

        assertEquals(false, view.confirmRestore("foo"))
    }

    @Test
    fun `chooseSaveToRestore returns the raw typed name unvalidated`() {
        val (view, _) = viewWith("bar\n")

        assertEquals("bar", view.chooseSaveToRestore(listOf("foo", "bar")))
    }

    @Test
    fun `chooseSaveToRestore returns null on a blank line`() {
        val (view, _) = viewWith("\n")

        assertEquals(null, view.chooseSaveToRestore(listOf("foo", "bar")))
    }

    @Test
    fun `chooseSaveToRestore returns null on EOF instead of throwing`() {
        val (view, _) = viewWith("")

        assertEquals(null, view.chooseSaveToRestore(listOf("foo", "bar")))
    }

    @Test
    fun `chooseSaveToRestore returns null when the input stream is dead, same as clean EOF`() {
        val view = ViewImpl(BufferedReader(ThrowingReader()), PrintStream(ByteArrayOutputStream()))
        view.presenter = FakePresenter()

        assertEquals(null, view.chooseSaveToRestore(listOf("foo", "bar")))
    }

    @Test
    fun `exit command delegates to presenter exitGame`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("exit\n", presenter)

        view.processCommand()

        assertEquals(listOf("exitGame"), presenter.calls)
    }

    @Test
    fun `quit command is an alias for exit`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("quit\n", presenter)

        view.processCommand()

        assertEquals(listOf("exitGame"), presenter.calls)
    }

    @Test
    fun `exit with a trailing argument is rejected rather than silently ignored`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("exit foo\n", presenter)

        assertFailsWith<IllegalArgumentException> { view.processCommand() }
        assertEquals(emptyList(), presenter.calls)
    }

    @Test
    fun `confirmSaveBeforeExit returns true for y or yes, case-insensitively`() {
        for (answer in listOf("y", "Y", "yes", "YES", "Yes")) {
            val (view, _) = viewWith("$answer\n")
            assertEquals(true, view.confirmSaveBeforeExit(), "expected '$answer' to confirm")
        }
    }

    @Test
    fun `confirmSaveBeforeExit returns false for a blank line, a no, or garbage input`() {
        for (answer in listOf("", "n", "no", "blah")) {
            val (view, _) = viewWith("$answer\n")
            assertEquals(false, view.confirmSaveBeforeExit(), "expected '$answer' to decline")
        }
    }

    @Test
    fun `confirmSaveBeforeExit returns false on EOF instead of throwing`() {
        val (view, _) = viewWith("")

        assertEquals(false, view.confirmSaveBeforeExit())
    }

    @Test
    fun `confirmSaveBeforeExit returns false when the input stream is dead, same as clean EOF`() {
        val view = ViewImpl(BufferedReader(ThrowingReader()), PrintStream(ByteArrayOutputStream()))
        view.presenter = FakePresenter()

        assertEquals(false, view.confirmSaveBeforeExit())
    }

    @Test
    fun `promptSaveName returns the raw typed name unvalidated`() {
        val (view, _) = viewWith("foo\n")

        assertEquals("foo", view.promptSaveName())
    }

    @Test
    fun `promptSaveName returns null on a blank line`() {
        val (view, _) = viewWith("\n")

        assertEquals(null, view.promptSaveName())
    }

    @Test
    fun `promptSaveName returns null on EOF instead of throwing`() {
        val (view, _) = viewWith("")

        assertEquals(null, view.promptSaveName())
    }

    @Test
    fun `promptSaveName returns null when the input stream is dead, same as clean EOF`() {
        val view = ViewImpl(BufferedReader(ThrowingReader()), PrintStream(ByteArrayOutputStream()))
        view.presenter = FakePresenter()

        assertEquals(null, view.promptSaveName())
    }

    @Test
    fun `create wires the injected input, output, and presenter together atomically`() {
        val presenter = FakePresenter()
        val outputBuffer = ByteArrayOutputStream()

        val view = ViewImpl.create(
            input = BufferedReader(StringReader("left 1\n")),
            output = PrintStream(outputBuffer),
        ) { presenter }

        view.processCommand()

        assertEquals(listOf("shiftLeft(0)"), presenter.calls)
    }
}
