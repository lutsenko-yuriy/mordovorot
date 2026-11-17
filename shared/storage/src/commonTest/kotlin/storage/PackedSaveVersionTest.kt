package storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** GH-313: saves are told apart by their first character, so several versions can be read in parallel. */
class PackedSaveVersionTest {

    private val side = 3
    private val state = IntArray(side * side) { it }

    private fun encode(labels: String?) = SaveFileCodec.encode(side, state, 5, null, labels)

    @Test
    fun `every shipped scheme, no labels and an unknown name are written with the marker, as before`() {
        listOf(null, "numbers", "numbers_from_0", "pairs", "pairs_from_0", "letters", "letter_pairs", "hexes").forEach { labels ->
            val text = encode(labels)

            assertEquals('~', text.first(), "$labels")
            assertEquals(labels, SaveFileCodec.decode("s", text).labels)
        }
    }

    @Test
    fun `the shipped schemes keep their bytes`() {
        // Goldens taken before GH-313 changed the code: one frozen list position per name, no escape.
        val goldens = mapOf(
            "numbers" to "~MgCgAAA", "numbers_from_0" to "~NACgAAA", "pairs" to "~NgCgAAA", "pairs_from_0" to "~OACgAAA",
            "letters" to "~OgCgAAA", "letter_pairs" to "~PACgAAA", null to "~MACgAAA",
        )
        assertEquals(goldens, goldens.keys.associateWith { encode(it) })
    }

    @Test
    fun `the Cyrillic names go through the escape and round-trip`() {
        listOf("cyrillic_letters", "cyrillic_letter_pairs").forEach { labels ->
            val text = encode(labels)

            assertEquals('~', text.first())
            assertEquals(labels, SaveFileCodec.decode("s", text).labels)
            assertTrue(text.length > encode("letter_pairs").length, labels)
        }
    }

    @Test
    fun `a first character no version claims is a save from a newer version`() {
        listOf("#abc", "Abc", "-abc", "?").forEach { text ->
            val e = assertFailsWith<SaveFromNewerVersionException>(text) { SaveFileCodec.decode("s", text) }

            assertIs<SaveFileFormatException>(e)
        }
    }

    @Test
    fun `a digit, whitespace or nothing is still read as text`() {
        listOf("3\n0 1 2 3 4 5 6 7 8\n0\n", " 3", "").forEach { text ->
            val e = runCatching { SaveFileCodec.decode("s", text) }.exceptionOrNull()

            assertTrue(e == null || (e is SaveFileFormatException && e !is SaveFromNewerVersionException), "'$text' $e")
        }
    }

    @Test
    fun `a broken packed save is damaged, not newer`() {
        val e = assertFailsWith<SaveFileFormatException> { SaveFileCodec.decode("s", "~!!!") }

        assertTrue(e !is SaveFromNewerVersionException)
    }
}
