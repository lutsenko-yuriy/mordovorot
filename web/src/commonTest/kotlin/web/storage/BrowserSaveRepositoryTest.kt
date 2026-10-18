package web.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import storage.SaveFileFormatException
import storage.SavedBoard
import storage.SavedMordie

class BrowserSaveRepositoryTest {

    private val store = InMemoryKeyValueStore()
    private val repo = BrowserSaveRepository(store)

    @Test
    fun `a save is one entry in the terminal's text format`() {
        repo.save("foo", intArrayOf(1, 0, 2, 3), 2, moveCount = 4)

        assertEquals(mapOf("mordovorot.save.foo" to "2\n1 0 2 3\n4\n"), store.entries)
    }

    @Test
    fun `load returns what save stored, Mordie data included`() {
        val mordie = SavedMordie(intArrayOf(2, 3, 0, 1), moves = 11, found = true)
        repo.save("m", intArrayOf(3, 0, 2, 1), 2, moveCount = 7, mordie = mordie)

        assertEquals(SavedBoard(2, intArrayOf(3, 0, 2, 1), 7, mordie), repo.load("m"))
    }

    @Test
    fun `load of a missing save is null`() {
        assertNull(repo.load("nope"))
    }

    @Test
    fun `a corrupted entry fails to load with the save's name`() {
        store.entries["mordovorot.save.bad"] = "x\n0 1 2 3\n"

        val e = assertFailsWith<SaveFileFormatException> { repo.load("bad") }
        assertEquals("Save 'bad' is corrupted: square side is not an integer", e.message)
    }

    @Test
    fun `listSaves is sorted, and ignores other keys and unsafe names`() {
        repo.save("b", intArrayOf(0, 1, 2, 3), 2)
        repo.save("a", intArrayOf(0, 1, 2, 3), 2)
        store.entries["other.app"] = "1"
        store.entries["mordovorot.save."] = "1"
        store.entries["mordovorot.save.x/y"] = "1"

        assertEquals(listOf("a", "b"), repo.listSaves())
    }

    @Test
    fun `exists follows save`() {
        assertFalse(repo.exists("foo"))
        repo.save("foo", intArrayOf(0, 1, 2, 3), 2)
        assertTrue(repo.exists("foo"))
    }

    @Test
    fun `unsafe names are rejected as on the terminal`() {
        for (name in listOf("", " ", "a/b", "a\\b", "..")) {
            assertFalse(repo.isValidName(name), name)
            assertFailsWith<IllegalArgumentException>(name) { repo.save(name, intArrayOf(0, 1, 2, 3), 2) }
            assertFailsWith<IllegalArgumentException>(name) { repo.load(name) }
            assertFailsWith<IllegalArgumentException>(name) { repo.exists(name) }
        }
        assertTrue(store.entries.isEmpty())
    }

    @Test
    fun `an invalid state is rejected and stores nothing`() {
        assertFailsWith<IllegalArgumentException> { repo.save("foo", intArrayOf(0, 0, 2, 3), 2) }
        assertTrue(store.entries.isEmpty())
    }
}
