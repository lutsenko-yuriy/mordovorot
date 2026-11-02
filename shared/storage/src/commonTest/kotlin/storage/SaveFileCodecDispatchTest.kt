package storage

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** GH-237: [SaveFileCodec.decode] reads both formats, told apart by the first character. */
class SaveFileCodecDispatchTest {

    @Test
    fun `a save starting with the marker is read as packed`() {
        val saved = SaveFileCodec.decode("p", "~IAAA")

        assertEquals(2, saved.squareSide)
        assertContentEquals(intArrayOf(0, 1, 2, 3), saved.state)
    }

    @Test
    fun `a save starting with a digit is read as text`() {
        val saved = SaveFileCodec.decode("t", "2\n3 0 2 1\n7\n")

        assertEquals(SavedBoard(2, intArrayOf(3, 0, 2, 1), 7), saved)
    }

    @Test
    fun `anything else keeps the text format's messages`() {
        assertEquals("Save 'e' is corrupted: missing square side", assertFailsWith<SaveFileFormatException> { SaveFileCodec.decode("e", "") }.message)
        assertEquals("Save 'x' is corrupted: square side is not an integer", assertFailsWith<SaveFileFormatException> { SaveFileCodec.decode("x", "x\n0 1 2 3\n") }.message)
    }

    @Test
    fun `a corrupt packed save names the save`() {
        assertEquals("Save 'p' is corrupted: the packed data ends early", assertFailsWith<SaveFileFormatException> { SaveFileCodec.decode("p", "~IA") }.message)
    }
}
