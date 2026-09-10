package board_model

import java.util.*

/**
 * Fisher-Yates shuffling
 *
 */
fun IntArray.shuffle() {
    val shuffler: Random = Random()

    for (i in 0..this.size - 1) {
        val randomPosition = shuffler.nextInt(this.size)

        val bufferInt = this[randomPosition]
        this[randomPosition] = this[i]
        this[i] = bufferInt
    }
}