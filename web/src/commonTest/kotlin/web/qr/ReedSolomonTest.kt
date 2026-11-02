package web.qr

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ReedSolomonTest {

    @Test
    fun `multiplying in GF 256 reduces by 0x11D`() {
        assertEquals(0, ReedSolomon.multiply(0, 77))
        assertEquals(77, ReedSolomon.multiply(1, 77))
        assertEquals(0x1D, ReedSolomon.multiply(0x80, 2)) // x^8 = x^4 + x^3 + x^2 + 1
        assertEquals(ReedSolomon.multiply(23, 201), ReedSolomon.multiply(201, 23))
    }

    @Test
    fun `the generator of degree 2 is x squared plus 3x plus 2`() {
        assertContentEquals(intArrayOf(3, 2), ReedSolomon.generator(2))
    }

    @Test
    fun `the generator of degree 7 matches the standard's exponents`() {
        // alpha^87, alpha^229, alpha^146, alpha^149, alpha^238, alpha^102, alpha^21 (the leading 1 is implied).
        val alpha = IntArray(256).also { table ->
            var x = 1
            for (i in 0 until 255) {
                table[i] = x
                x = ReedSolomon.multiply(x, 2)
            }
        }
        assertContentEquals(intArrayOf(87, 229, 146, 149, 238, 102, 21).map { alpha[it] }.toIntArray(), ReedSolomon.generator(7))
    }

    @Test
    fun `the check bytes of the HELLO WORLD codewords at version 1 M`() {
        val data = intArrayOf(32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17, 236, 17, 236, 17).map { it.toByte() }.toByteArray()
        val expected = intArrayOf(196, 35, 39, 119, 235, 215, 231, 226, 93, 23).map { it.toByte() }.toByteArray()

        assertContentEquals(expected, ReedSolomon.remainder(data, 10))
    }
}
