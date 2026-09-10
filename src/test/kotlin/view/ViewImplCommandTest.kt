package view

import testing.FakePresenter
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.PrintStream
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ViewImplCommandTest {

    private fun viewWith(input: String, presenter: FakePresenter = FakePresenter()): Pair<ViewImpl, ByteArrayOutputStream> {
        val outputBuffer = ByteArrayOutputStream()
        val view = ViewImpl(BufferedReader(StringReader(input)), PrintStream(outputBuffer))
        view.presenter = presenter
        return view to outputBuffer
    }

    @Test
    fun `left command delegates to presenter shiftLeft`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("left 0\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftLeft(0)"), presenter.calls)
    }

    @Test
    fun `right command delegates to presenter shiftRight`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("right 2\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftRight(2)"), presenter.calls)
    }

    @Test
    fun `up command delegates to presenter shiftUp`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("up 1\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftUp(1)"), presenter.calls)
    }

    @Test
    fun `down command delegates to presenter shiftDown`() {
        val presenter = FakePresenter()
        val (view, _) = viewWith("down 3\n", presenter)

        view.processCommand()

        assertEquals(listOf("shiftDown(3)"), presenter.calls)
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
        val (view, _) = viewWith("LEFT 0\n", presenter)

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
    fun `EOF throws EOFException instead of looping forever - regression guard for GH-4's 651ca64`() {
        val (view, _) = viewWith("")

        assertFailsWith<EOFException> { view.processCommand() }
    }

    @Test
    fun `displayBoard renders a tab-separated grid`() {
        val (view, output) = viewWith("")

        view.displayBoard(intArrayOf(0, 1, 2, 3), 2)

        assertEquals("0\t1\t\n2\t3\t\n\n", output.toString())
    }
}
