package viewmodel

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CatchingNonCancellationTest {

    @Test
    fun `returns the block result when nothing throws`() {
        val result = catchingNonCancellation(onError = { "error" }) { "ok" }
        assertEquals("ok", result)
    }

    @Test
    fun `routes an ordinary exception to onError and returns its result`() {
        val thrown = IllegalStateException("boom")
        var seen: Exception? = null
        val result = catchingNonCancellation(onError = { seen = it; "handled" }) { throw thrown }
        assertSame(thrown, seen)
        assertEquals("handled", result)
    }

    @Test
    fun `rethrows a CancellationException without calling onError`() {
        var called = false
        assertFailsWith<CancellationException> {
            catchingNonCancellation(onError = { called = true }) { throw CancellationException("cancelled") }
        }
        assertEquals(false, called)
    }

    @Test
    fun `lets a non-Exception Throwable propagate`() {
        assertFailsWith<AssertionError> {
            catchingNonCancellation(onError = {}) { throw AssertionError("fatal") }
        }
    }
}
