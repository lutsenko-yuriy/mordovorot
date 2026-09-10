package storage

import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileSaveRepositoryTest {

    private fun newRepository() = FileSaveRepository(createTempDirectory("mordovorot-saves"))

    @Test
    fun `save then load round-trips the board state`() {
        val repo = newRepository()
        val state = intArrayOf(3, 0, 7, 1, 12, 5, 6, 4, 9, 8, 11, 10, 13, 2, 15, 14)

        repo.save("foo", state, 4)
        val loaded = repo.load("foo")

        assertEquals(SavedBoard(4, state), loaded)
    }

    @Test
    fun `save overwrites an existing file of the same name`() {
        val repo = newRepository()
        repo.save("foo", intArrayOf(0, 1, 2, 3), 2)

        repo.save("foo", intArrayOf(3, 2, 1, 0), 2)

        assertEquals(SavedBoard(2, intArrayOf(3, 2, 1, 0)), repo.load("foo"))
    }

    @Test
    fun `load returns null for a name with no save`() {
        val repo = newRepository()

        assertNull(repo.load("missing"))
    }

    @Test
    fun `listSaves is empty when the directory doesn't exist`() {
        val repo = FileSaveRepository(createTempDirectory("mordovorot-saves").resolve("does-not-exist"))

        assertEquals(emptyList(), repo.listSaves())
    }

    @Test
    fun `listSaves returns save names sorted`() {
        val repo = newRepository()
        repo.save("charlie", intArrayOf(0, 1, 2, 3), 2)
        repo.save("alpha", intArrayOf(0, 1, 2, 3), 2)
        repo.save("bravo", intArrayOf(0, 1, 2, 3), 2)

        assertEquals(listOf("alpha", "bravo", "charlie"), repo.listSaves())
    }

    @Test
    fun `exists is true only for a known save name`() {
        val repo = newRepository()
        repo.save("foo", intArrayOf(0, 1, 2, 3), 2)

        assertTrue(repo.exists("foo"))
        assertTrue(!repo.exists("bar"))
    }

    @Test
    fun `save rejects an empty or blank name`() {
        val repo = newRepository()

        assertFailsWith<IllegalArgumentException> { repo.save("", intArrayOf(0), 1) }
        assertFailsWith<IllegalArgumentException> { repo.save("   ", intArrayOf(0), 1) }
    }

    @Test
    fun `save rejects a name containing a path separator`() {
        val repo = newRepository()

        assertFailsWith<IllegalArgumentException> { repo.save("a/b", intArrayOf(0), 1) }
        assertFailsWith<IllegalArgumentException> { repo.save("a\\b", intArrayOf(0), 1) }
    }

    @Test
    fun `save rejects a name containing a dot-dot segment`() {
        val repo = newRepository()

        assertFailsWith<IllegalArgumentException> { repo.save("..", intArrayOf(0), 1) }
    }

    @Test
    fun `save rejects a non-positive squareSide`() {
        val repo = newRepository()

        assertFailsWith<IllegalArgumentException> { repo.save("foo", intArrayOf(0), 0) }
        assertFailsWith<IllegalArgumentException> { repo.save("foo", intArrayOf(0), -3) }
    }

    @Test
    fun `save rejects a state whose size doesn't match squareSide`() {
        val repo = newRepository()

        assertFailsWith<IllegalArgumentException> { repo.save("foo", intArrayOf(0, 1, 2), 4) }
    }

    @Test
    fun `save rejects a state that isn't a permutation`() {
        val repo = newRepository()

        assertFailsWith<IllegalArgumentException> { repo.save("foo", intArrayOf(0, 0, 2, 3), 2) }
    }

    @Test
    fun `save overwrite is atomic - a failed overwrite attempt leaves the previous save intact`() {
        val repo = newRepository()
        repo.save("foo", intArrayOf(0, 1, 2, 3), 2)

        // An invalid second save must be rejected before any file is touched.
        assertFailsWith<IllegalArgumentException> { repo.save("foo", intArrayOf(0, 1, 2), 4) }

        assertEquals(SavedBoard(2, intArrayOf(0, 1, 2, 3)), repo.load("foo"))
    }

    @Test
    fun `the on-disk format is square side on line 1, values on line 2`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)

        repo.save("foo", intArrayOf(3, 0, 2, 1), 2)

        assertEquals("2\n3 0 2 1", dir.resolve("foo.save").readText().trim())
    }

    @Test
    fun `load throws SaveFileFormatException on a non-integer token`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("foo.save").writeText("2\n0 1 x 3")

        assertFailsWith<SaveFileFormatException> { repo.load("foo") }
    }

    @Test
    fun `load throws SaveFileFormatException when the value count doesn't match the square side`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("foo.save").writeText("2\n0 1 2")

        assertFailsWith<SaveFileFormatException> { repo.load("foo") }
    }

    @Test
    fun `load throws SaveFileFormatException when the values aren't a permutation`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("foo.save").writeText("2\n0 0 2 3")

        assertFailsWith<SaveFileFormatException> { repo.load("foo") }
    }

    @Test
    fun `load throws SaveFileFormatException on an empty file`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("foo.save").writeText("")

        assertFailsWith<SaveFileFormatException> { repo.load("foo") }
    }

    @Test
    fun `load throws SaveFileFormatException when the second line is missing`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("foo.save").writeText("2")

        assertFailsWith<SaveFileFormatException> { repo.load("foo") }
    }

    @Test
    fun `load throws SaveFileFormatException on a non-positive square side`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("foo.save").writeText("0\n")

        assertFailsWith<SaveFileFormatException> { repo.load("foo") }
    }

    @Test
    fun `listSaves ignores a bare extension file with no name`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve(".save").writeText("2\n0 1 2 3")

        assertEquals(emptyList(), repo.listSaves())
    }
}
