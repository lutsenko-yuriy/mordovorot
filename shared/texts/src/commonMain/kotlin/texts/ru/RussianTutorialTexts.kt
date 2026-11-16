package texts.ru

import texts.TutorialTexts

object RussianTutorialTexts : TutorialTexts {
    override fun step1(computer: Boolean) =
        "Привет! Я Морди. Давай сыграем в короткую игру. ${verb(computer)} верхнюю строку на одну плитку влево" + lead(computer)

    override fun step2(tile: String, computer: Boolean) =
        "Линии идут по кругу: видишь плитку $tile вверху второго столбца? ${verb(computer)} этот столбец на одну плитку вверх, и" +
            " $tile вернётся снизу" + lead(computer)

    override fun step3(computer: Boolean) =
        "Ход — это один сдвиг строки или столбца, на сколько угодно. ${verb(computer)} третью строку на две плитки вправо: это всё равно 1 ход."

    override fun step4(computer: Boolean) =
        "Последний: ${verb(computer).lowercase()} четвёртый столбец на одну плитку вниз. И головоломка решена!"

    override val step5 = "Сможешь побить мои результаты?"

    override fun again(moves: Int) = "Ещё раз! Строка та же, так что в «Ходы» по-прежнему $moves."
    override val blockedRow = "Это можно сделать потом. А пока сдвинь, пожалуйста, эту строку!"
    override val blockedColumn = "Это можно сделать потом. А пока сдвинь, пожалуйста, этот столбец!"

    override fun dragLeft(tiles: Int) = "Перетащи " + if (tiles == 1) "влево" else "на ${tilesIn(tiles)} влево"
    override fun dragRight(tiles: Int) = "Перетащи " + if (tiles == 1) "вправо" else "на ${tilesIn(tiles)} вправо"
    override fun dragUp(tiles: Int) = "Перетащи " + if (tiles == 1) "вверх" else "на ${tilesIn(tiles)} вверх"
    override fun dragDown(tiles: Int) = "Перетащи " + if (tiles == 1) "вниз" else "на ${tilesIn(tiles)} вниз"

    override fun click(arrow: String, times: Int) = "Нажми на $arrow" + times(times)
    override fun press(keys: String, times: Int) = "Нажми $keys" + times(times)
    override fun pressThen(select: String, selectTimes: Int, shift: String, shiftTimes: Int) =
        "Нажми $select${times(selectTimes)}, затем $shift${times(shiftTimes)}"

    /** A computer shifts however it likes, and its ways follow on their own lines (GH-290); a phone drags. */
    private fun verb(computer: Boolean) = if (computer) "Сдвинь" else "Перетащи"

    private fun lead(computer: Boolean) = if (computer) ":" else "."

    private fun times(count: Int) = when (count) {
        1 -> ""
        2 -> " дважды"
        else -> " ${number(count, "три")} " + russianPlural(count, "раз", "раза", "раз")
    }

    private fun tilesIn(count: Int) = "${number(count, "одну")} " + russianPlural(count, "плитку", "плитки", "плиток")

    /** [count] in words where it has a form of its own (`две плитки`, `три раза`), else in digits. */
    private fun number(count: Int, one: String) = when (count) {
        1 -> one
        2 -> "две"
        3 -> "три"
        else -> "$count"
    }
}
