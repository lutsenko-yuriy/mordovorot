package view

import InputMode
import board_model.BoardImpl
import presenter.ConsolePresenterImpl
import presenter.ModeSwitchRequestedException
import testing.FakeSaveRepository
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Hard problem 2's residual case (GH-42 WU2, see the plan comment on GH-42): a control-flow
 * exception thrown from *inside* the request-handler coroutine - not the presenter's own driving
 * coroutine - must still escape [ViewImpl.play] to its caller. No real call path reaches this
 * today (no `View` prompt method ever throws), so this test forces it by making the handler's
 * own `showMessage` output throw, to pin that [kotlinx.coroutines.coroutineScope]'s
 * structured-concurrency propagation - not a bespoke catch anywhere - is what makes this safe.
 */
class ViewImplRequestHandlingTest {

    @Test
    fun `an exception thrown inside the request handler escapes play() to the caller`(): Unit = runBlocking {
        // Only the handler's own showMessage("Saved as ...") call should throw - displayBoard's
        // println(), and processCommand()'s blank-line println(), both use the no-arg overload
        // and must keep working so the failure is pinned to the handler coroutine specifically.
        val output = object : PrintStream(ByteArrayOutputStream()) {
            override fun println(x: String?) {
                if (x?.contains("Saved as") == true) throw ModeSwitchRequestedException(InputMode.CONSOLE)
                super.println(x)
            }
        }
        val input = BufferedReader(StringReader("save foo\n"))
        val view = ViewImpl.create(input, output) { v -> ConsolePresenterImpl(v, BoardImpl(), FakeSaveRepository()) }

        assertFailsWith<ModeSwitchRequestedException> { view.play() }
    }
}
