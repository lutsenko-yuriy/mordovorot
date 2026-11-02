package web.qr

/** Reed-Solomon error correction over GF(256) with the QR polynomial 0x11D (ISO/IEC 18004). */
internal object ReedSolomon {

    /** The [degree] check bytes of [data]: the remainder of data * x^degree divided by the generator with roots 1, 2, 4, ... */
    fun remainder(data: ByteArray, degree: Int): ByteArray {
        val generator = generator(degree)
        val result = IntArray(degree)
        for (byte in data) {
            val factor = (byte.toInt() and 0xFF) xor result[0]
            result.copyInto(result, 0, 1)
            result[degree - 1] = 0
            for (i in 0 until degree) result[i] = result[i] xor multiply(generator[i], factor)
        }
        return ByteArray(degree) { result[it].toByte() }
    }

    /** The generator's coefficients below its leading 1, highest power first. */
    fun generator(degree: Int): IntArray {
        val result = IntArray(degree)
        result[degree - 1] = 1
        var root = 1
        repeat(degree) {
            for (j in 0 until degree) {
                result[j] = multiply(result[j], root)
                if (j + 1 < degree) result[j] = result[j] xor result[j + 1]
            }
            root = multiply(root, 2)
        }
        return result
    }

    fun multiply(x: Int, y: Int): Int {
        var product = 0
        for (i in 7 downTo 0) {
            product = (product shl 1) xor ((product ushr 7) * 0x11D)
            product = product xor (((y ushr i) and 1) * x)
        }
        return product
    }
}
