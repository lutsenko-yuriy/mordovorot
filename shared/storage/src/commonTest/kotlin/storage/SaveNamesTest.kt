package storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveNamesTest {

    @Test
    fun `isSafe accepts ordinary names and rejects blank, path separators, and dot-dot`() {
        for (name in listOf("foo", "my game", "save_1", "a..b")) {
            assertTrue(SaveNames.isSafe(name), "expected '$name' to be safe")
        }
        for (name in listOf("", "  ", "a/b", "a\\b", "..", "a/../b")) {
            assertFalse(SaveNames.isSafe(name), "expected '$name' to be unsafe")
        }
    }

    @Test
    fun `requireSafe throws IllegalArgumentException with the terminal's message`() {
        val e = assertFailsWith<IllegalArgumentException> { SaveNames.requireSafe("a/b") }

        assertEquals("Save name must not be blank, contain a path separator, or contain '..'", e.message)
        SaveNames.requireSafe("foo")
    }
}
