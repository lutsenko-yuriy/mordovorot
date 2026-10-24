package storage

import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FilePreferenceRepositoryTest {

    private val root = createTempDirectory("mordovorot-prefs")
    private val dir = root.resolve("saves")
    private val repo = FilePreferenceRepository(dir)

    @Test
    fun `a missing file reads as empty`() {
        assertNull(repo.get("tile_labels"))
    }

    @Test
    fun `set creates the directory and stores key=value lines`() {
        repo.set("tile_labels", "pairs")

        assertEquals("pairs", repo.get("tile_labels"))
        assertEquals("tile_labels=pairs\n", dir.resolve("preferences").readText())
    }

    @Test
    fun `set replaces the key's value and keeps other lines`() {
        dir.toFile().mkdirs()
        dir.resolve("preferences").writeText("future=1\ntile_labels=pairs\n# a note\n")

        repo.set("tile_labels", "letters")

        assertEquals("future=1\ntile_labels=letters\n# a note\n", dir.resolve("preferences").readText())
        assertEquals("1", FilePreferenceRepository(dir).get("future"))
    }

    @Test
    fun `a key with = or a line break, or a value with a line break, is rejected`() {
        for ((key, value) in listOf("a=b" to "x", "a\nb" to "x", "a" to "x\ny", "" to "x")) {
            assertFailsWith<IllegalArgumentException>("$key / $value") { repo.set(key, value) }
        }
        assertNull(repo.get("a"))
    }

    @Test
    fun `the preferences file is never listed as a save`() {
        repo.set("tile_labels", "pairs")
        FileSaveRepository(dir).save("game", intArrayOf(0, 1, 2, 3), 2)

        assertEquals(listOf("game"), FileSaveRepository(dir).listSaves())
        assertTrue(dir.resolve("preferences").exists())
    }
}
