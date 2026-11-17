package texts.en

import texts.GREEK_WORDS_EN
import texts.NATO_WORDS
import texts.RUSSIAN_SPELLING_WORDS
import texts.ReaderTexts

object EnglishReaderTexts : ReaderTexts {
    override fun rowLeft(line: Int) = "Row $line left"
    override fun rowRight(line: Int) = "Row $line right"
    override fun columnUp(line: Int) = "Column $line up"
    override fun columnDown(line: Int) = "Column $line down"

    override fun withMoves(words: String, moves: Int) = "$words, ${englishMoves(moves)}"
    override fun dragged(words: String, distance: Int) = "$words $distance"
    override fun solved(moves: Int) = "Solved in ${englishMoves(moves)}"
    override fun newGame(side: Int) = "New ${side}x$side game"
    override fun loadedSolution(name: String, side: Int, steps: Int) = "Loaded '$name', ${side}x$side, solution, ${englishMoves(steps)}"
    override fun loadedSolved(name: String, side: Int, moves: Int) = "Loaded '$name', ${side}x$side, solved in ${englishMoves(moves)}"
    override fun loadedPlain(name: String, side: Int, moves: Int) = "Loaded '$name', ${side}x$side, ${englishMoves(moves)}"
    override fun solution(steps: Int) = "Solution, ${englishMoves(steps)}"
    override fun stepStart(total: Int) = "Start, step 0 of $total"
    override fun stepOf(current: Int, total: Int, words: String, count: Int) = "Step $current of $total: $words $count"
    override fun boardStep(step: String) = "Board, " + step.replaceFirstChar { it.lowercase() }
    override fun withNow(words: String, tile: String) = "$words. Now $tile"

    override fun boardRow(row: Int, tiles: String) = "Row $row: $tiles."
    override fun tilesInPlace(count: Int) = "${if (count == 1) "1 tile" else "$count tiles"} $inPlace."
    override val inPlace = "in place"
    override val board = "Board"
    override val readBoard = "Read board"
    override fun notOnBoard(letter: String) = "$letter, not on this board"

    override val phonetic = NATO_WORDS
    override val cyrillicPhonetic = RUSSIAN_SPELLING_WORDS
    override val greekPhonetic = GREEK_WORDS_EN
}
