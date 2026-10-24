package storage

import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.listDirectoryEntries
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
    fun `isValidName accepts ordinary names and rejects blank, path separators, and dot-dot`() {
        val repo = newRepository()

        for (name in listOf("foo", "my game", "save_1")) {
            assertTrue(repo.isValidName(name), "expected '$name' to be valid")
        }
        for (name in listOf("", "  ", "a/b", "a\\b", "..", "a/../b")) {
            assertTrue(!repo.isValidName(name), "expected '$name' to be invalid")
        }
    }

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
    fun `save cleans up its temp file when the atomic move fails`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        // Occupy the target path with a directory so the move can't complete.
        Files.createDirectory(dir.resolve("foo.save"))

        assertFailsWith<IOException> { repo.save("foo", intArrayOf(0, 1, 2, 3), 2) }

        assertEquals(emptyList(), dir.listDirectoryEntries("*.tmp").toList())
    }

    @Test
    fun `the on-disk format is square side on line 1, values on line 2, move count on line 3`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)

        repo.save("foo", intArrayOf(3, 0, 2, 1), 2, moveCount = 7)

        assertEquals("2\n3 0 2 1\n7", dir.resolve("foo.save").readText().trim())
    }

    @Test
    fun `save then load round-trips the move count`() {
        val repo = newRepository()
        val state = intArrayOf(3, 0, 2, 1)

        repo.save("foo", state, 2, moveCount = 12)

        assertEquals(SavedBoard(2, state, moveCount = 12), repo.load("foo"))
    }

    @Test
    fun `load treats a missing or blank move count line as 0, GH-62`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("legacy.save").writeText("2\n3 0 2 1\n")
        dir.resolve("blank.save").writeText("2\n3 0 2 1\n  \n")

        assertEquals(0, repo.load("legacy")?.moveCount)
        assertEquals(0, repo.load("blank")?.moveCount)
    }

    @Test
    fun `load throws SaveFileFormatException on a non-integer or negative move count`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("word.save").writeText("2\n3 0 2 1\nmany\n")
        dir.resolve("negative.save").writeText("2\n3 0 2 1\n-1\n")

        assertFailsWith<SaveFileFormatException> { repo.load("word") }
        assertFailsWith<SaveFileFormatException> { repo.load("negative") }
    }

    @Test
    fun `save rejects a negative move count`() {
        val repo = newRepository()

        assertFailsWith<IllegalArgumentException> { repo.save("foo", intArrayOf(3, 0, 2, 1), 2, moveCount = -1) }
    }

    @Test
    fun `save then load round-trips Mordie data, searching and found, GH-83`() {
        val repo = newRepository()
        val state = intArrayOf(3, 0, 2, 1)
        val searching = SavedMordie(intArrayOf(1, 0, 3, 2), moves = 8, found = false)
        val found = SavedMordie(intArrayOf(2, 3, 0, 1), moves = 11, found = true)

        repo.save("searching", state, 2, moveCount = 5, mordie = searching)
        repo.save("found", state, 2, moveCount = 5, mordie = found)

        assertEquals(SavedBoard(2, state, 5, searching), repo.load("searching"))
        assertEquals(SavedBoard(2, state, 5, found), repo.load("found"))
    }

    @Test
    fun `save then load round-trips Mordie's solution and the solution-screen flag, GH-126`() {
        val repo = newRepository()
        val original = intArrayOf(2, 3, 0, 1)
        val path = listOf(SavedMove(isRow = false, index = 0, amount = 1), SavedMove(isRow = false, index = 1, amount = 1))
        val screen = SavedMordie(original, moves = 2, found = true, solution = path, solutionScreen = true)

        repo.save("screen", original, 2, moveCount = 0, mordie = screen)

        assertEquals(SavedBoard(2, original, 0, screen), repo.load("screen"))
    }

    @Test
    fun `save then load round-trips the tile labels on line 8, GH-139`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)

        repo.save("l", intArrayOf(3, 0, 2, 1), 2, moveCount = 7, labels = "pairs")

        assertEquals(SavedBoard(2, intArrayOf(3, 0, 2, 1), 7, labels = "pairs"), repo.load("l"))
        assertEquals("2\n3 0 2 1\n7\n\n\n\n\npairs\n", dir.resolve("l.save").readText())
    }

    @Test
    fun `Mordie data goes on lines 4-5 after an unchanged first three lines`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)

        repo.save("a", intArrayOf(3, 0, 2, 1), 2, moveCount = 7, mordie = SavedMordie(intArrayOf(1, 0, 3, 2), 8, found = false))
        repo.save("b", intArrayOf(3, 0, 2, 1), 2, moveCount = 7, mordie = SavedMordie(intArrayOf(1, 0, 3, 2), 1, found = true))

        assertEquals("2\n3 0 2 1\n7\n1 0 3 2\n8 searching", dir.resolve("a.save").readText().trim())
        assertEquals("2\n3 0 2 1\n7\n1 0 3 2\n1 found", dir.resolve("b.save").readText().trim())
    }

    @Test
    fun `a save without Mordie data loads with none - legacy or blank line 4`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        repo.save("plain", intArrayOf(3, 0, 2, 1), 2, moveCount = 7)
        dir.resolve("legacy.save").writeText("2\n3 0 2 1\n7\n")
        dir.resolve("blank.save").writeText("2\n3 0 2 1\n7\n  \n")

        assertNull(repo.load("plain")?.mordie)
        assertNull(repo.load("legacy")?.mordie)
        assertNull(repo.load("blank")?.mordie)
    }

    @Test
    fun `load throws SaveFileFormatException on bad Mordie data`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        val bad = mapOf(
            "no-status" to "1 0 3 2\n",
            "blank-status" to "1 0 3 2\n \n",
            "bad-keyword" to "1 0 3 2\n8 solved\n",
            "negative" to "1 0 3 2\n-1 searching\n",
            "not-a-number" to "1 0 3 2\neight found\n",
            "extra-token" to "1 0 3 2\n8 found now\n",
            "wrong-size" to "1 0 2\n8 found\n",
            "not-a-permutation" to "1 1 3 2\n8 found\n",
            "non-integer-tile" to "1 x 3 2\n8 found\n",
        )
        for ((name, mordieLines) in bad) dir.resolve("$name.save").writeText("2\n3 0 2 1\n7\n$mordieLines")

        for (name in bad.keys) assertFailsWith<SaveFileFormatException>(name) { repo.load(name) }
    }

    @Test
    fun `save rejects invalid Mordie data`() {
        val repo = newRepository()
        val state = intArrayOf(3, 0, 2, 1)

        assertFailsWith<IllegalArgumentException> { repo.save("a", state, 2, mordie = SavedMordie(intArrayOf(0, 1, 2), 3, false)) }
        assertFailsWith<IllegalArgumentException> { repo.save("b", state, 2, mordie = SavedMordie(intArrayOf(0, 0, 2, 3), 3, false)) }
        assertFailsWith<IllegalArgumentException> { repo.save("c", state, 2, mordie = SavedMordie(intArrayOf(0, 1, 2, 3), -1, false)) }
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

    @Test
    fun `listSaves ignores a name that would be rejected as unsafe`() {
        val dir = createTempDirectory("mordovorot-saves")
        val repo = FileSaveRepository(dir)
        dir.resolve("...save").writeText("2\n0 1 2 3") // strips to ".."

        assertEquals(emptyList(), repo.listSaves())
    }
}
