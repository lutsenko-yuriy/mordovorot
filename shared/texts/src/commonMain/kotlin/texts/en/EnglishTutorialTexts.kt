package texts.en

import texts.TutorialTexts

object EnglishTutorialTexts : TutorialTexts {
    override fun step1(computer: Boolean) =
        "Hi there! I'm Mordie. Let's play a short game. ${verb(computer)} the top row one tile left" + lead(computer)

    override fun step2(tile: String, computer: Boolean) =
        "Lines go round: see tile $tile at the top of the second column? ${verb(computer)} that column one tile up, and" +
            " $tile comes back in at the bottom" + lead(computer)

    override fun step3(computer: Boolean) =
        "A move is one row or column shifted, however far. ${verb(computer)} the third row two tiles right: still 1 move."

    override fun step4(computer: Boolean) =
        "Last one: ${verb(computer).lowercase()} the fourth column one tile down. That solves it!"

    override val step5 = "Can you beat my results?"

    override fun again(moves: Int) = "Once more! It's the same row, so Moves stays at $moves."
    override val blockedRow = "You can do that later. For now, please shift that row!"
    override val blockedColumn = "You can do that later. For now, please shift that column!"

    override fun dragLeft(tiles: Int) = "Drag it " + if (tiles == 1) "left" else "${number(tiles)} tiles left"
    override fun dragRight(tiles: Int) = "Drag it " + if (tiles == 1) "right" else "${number(tiles)} tiles right"
    override fun dragUp(tiles: Int) = "Drag it " + if (tiles == 1) "up" else "${number(tiles)} tiles up"
    override fun dragDown(tiles: Int) = "Drag it " + if (tiles == 1) "down" else "${number(tiles)} tiles down"

    override fun click(arrow: String, times: Int) = "Click its $arrow" + times(times)
    override fun press(keys: String, times: Int) = "Press $keys" + times(times)
    override fun pressThen(select: String, selectTimes: Int, shift: String, shiftTimes: Int) =
        "Press $select${times(selectTimes)}, then $shift${times(shiftTimes)}"

    /** A computer shifts however it likes, and its ways follow on their own lines (GH-290); a phone drags. */
    private fun verb(computer: Boolean) = if (computer) "Shift" else "Drag"

    private fun lead(computer: Boolean) = if (computer) ":" else "."

    private fun times(count: Int) = when (count) {
        1 -> ""
        2 -> " twice"
        else -> " ${number(count)} times"
    }

    private fun number(count: Int) = when (count) {
        1 -> "one"
        2 -> "two"
        3 -> "three"
        else -> "$count"
    }
}
