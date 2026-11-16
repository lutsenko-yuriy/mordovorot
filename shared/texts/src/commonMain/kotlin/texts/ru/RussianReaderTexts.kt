package texts.ru

import texts.ReaderTexts
import texts.NATO_WORDS

object RussianReaderTexts : ReaderTexts {
    override fun rowLeft(line: Int) = "Строка $line влево"
    override fun rowRight(line: Int) = "Строка $line вправо"
    override fun columnUp(line: Int) = "Столбец $line вверх"
    override fun columnDown(line: Int) = "Столбец $line вниз"

    override fun withMoves(words: String, moves: Int) = "$words, ${russianMoves(moves)}"
    override fun dragged(words: String, distance: Int) = "$words $distance"
    override fun solved(moves: Int) = "Решено за ${russianMoves(moves)}"
    override fun newGame(side: Int) = "Новая игра ${side}x$side"
    override fun loadedSolution(name: String, side: Int, steps: Int) = "Загружено «$name», ${side}x$side, решение, ${russianMoves(steps)}"
    override fun loadedSolved(name: String, side: Int, moves: Int) = "Загружено «$name», ${side}x$side, решено за ${russianMoves(moves)}"
    override fun loadedPlain(name: String, side: Int, moves: Int) = "Загружено «$name», ${side}x$side, ${russianMoves(moves)}"
    override fun solution(steps: Int) = "Решение, ${russianMoves(steps)}"
    override fun stepStart(total: Int) = "Начало, шаг 0 из $total"
    override fun stepOf(current: Int, total: Int, words: String, count: Int) = "Шаг $current из $total: $words $count"
    override fun boardStep(step: String) = "Доска, " + step.replaceFirstChar { it.lowercase() }
    override fun withNow(words: String, tile: String) = "$words. Теперь $tile"

    override fun boardRow(row: Int, tiles: String) = "Строка $row: $tiles."
    override fun tilesInPlace(count: Int) = "${russianTiles(count)} $inPlace."
    override val inPlace = "на месте"
    override val board = "Доска"
    override val readBoard = "Озвучить доску"

    /** The Latin letters stay NATO words in every language (GH-275). */
    override val phonetic = NATO_WORDS
}
