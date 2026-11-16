package texts

/** What Mordie says in the tutorial's bubble (GH-288), and the ways lines under it (GH-290). A phone [computer] `false` drags only. */
interface TutorialTexts {
    fun step1(computer: Boolean): String

    /** [tile] is the label of the tile that wraps round. */
    fun step2(tile: String, computer: Boolean): String
    fun step3(computer: Boolean): String
    fun step4(computer: Boolean): String
    val step5: String

    /** Step 3 after one of its two tiles, at [moves] moves. */
    fun again(moves: Int): String
    val blockedRow: String
    val blockedColumn: String

    /** `Drag it left`, `Drag it two tiles left`: [tiles] is 1 to 3. */
    fun dragLeft(tiles: Int): String
    fun dragRight(tiles: Int): String
    fun dragUp(tiles: Int): String
    fun dragDown(tiles: Int): String

    /** `Click its ◀`, `Click its ◀ twice`: [arrow] is the arrow's glyph, [times] 1 to 3. */
    fun click(arrow: String, times: Int): String

    /** `Press H or ←`, `Press H or ← twice`. */
    fun press(keys: String, times: Int): String

    /** `Press W twice, then H or ←`. */
    fun pressThen(select: String, selectTimes: Int, shift: String, shiftTimes: Int): String
}
