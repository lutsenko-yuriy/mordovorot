package texts.de

import texts.GREEK_WORDS_DE
import texts.NATO_WORDS
import texts.PREREFORM_SPELLING_WORDS
import texts.RUSSIAN_SPELLING_WORDS
import texts.ReaderTexts

object GermanReaderTexts : ReaderTexts {
    override fun rowLeft(line: Int) = "Zeile $line nach links"
    override fun rowRight(line: Int) = "Zeile $line nach rechts"
    override fun columnUp(line: Int) = "Spalte $line nach oben"
    override fun columnDown(line: Int) = "Spalte $line nach unten"

    override fun withMoves(words: String, moves: Int) = "$words, ${germanMoves(moves)}"
    override fun dragged(words: String, distance: Int) = "$words $distance"
    override fun solved(moves: Int) = "Gelöst in ${germanMoves(moves, dative = true)}"
    override fun newGame(side: Int) = "Neues Spiel ${side}x$side"
    override fun loadedSolution(name: String, side: Int, steps: Int) = "„$name“ geladen, ${side}x$side, Lösung, ${germanMoves(steps)}"
    override fun loadedSolved(name: String, side: Int, moves: Int) =
        "„$name“ geladen, ${side}x$side, gelöst in ${germanMoves(moves, dative = true)}"
    override fun loadedPlain(name: String, side: Int, moves: Int) = "„$name“ geladen, ${side}x$side, ${germanMoves(moves)}"
    override fun solution(steps: Int) = "Lösung, ${germanMoves(steps)}"
    override fun stepStart(total: Int) = "Anfang, Schritt 0 von $total"
    override fun stepOf(current: Int, total: Int, words: String, count: Int) = "Schritt $current von $total: $words $count"
    override fun boardStep(step: String) = "Brett, $step"
    override fun withNow(words: String, tile: String) = "$words. Jetzt $tile"

    override fun boardRow(row: Int, tiles: String) = "Zeile $row: $tiles."
    override fun tilesInPlace(count: Int) = "${germanTiles(count)} $inPlace."
    override val inPlace = "an ihrem Platz"
    override val board = "Brett"
    override val readBoard = "Brett vorlesen"
    override fun notOnBoard(letter: String) = "$letter, nicht auf diesem Brett"

    /** The Latin letters stay NATO words in every language (GH-275). */
    override val phonetic = NATO_WORDS
    override val cyrillicPhonetic = RUSSIAN_SPELLING_WORDS
    override val greekPhonetic = GREEK_WORDS_DE
    override val prereformPhonetic = PREREFORM_SPELLING_WORDS
}
