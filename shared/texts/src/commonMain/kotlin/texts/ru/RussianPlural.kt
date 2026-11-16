package texts.ru

/** One, few or many: `1 ход`, `2 хода`, `5 ходов`, `11 ходов`, `21 ход`. */
internal fun russianPlural(count: Int, one: String, few: String, many: String): String {
    val teens = count % 100 in 11..14
    return when {
        !teens && count % 10 == 1 -> one
        !teens && count % 10 in 2..4 -> few
        else -> many
    }
}

/** Counted and after «за» or «сделал»: `1 ход`, `2 хода`, `5 ходов`. */
internal fun russianMoves(count: Int) = "$count " + russianPlural(count, "ход", "хода", "ходов")

/** After «после»: `1 хода`, `2 ходов`, `21 хода`. */
internal fun russianMovesGenitive(count: Int) = "$count " + russianPlural(count, "хода", "ходов", "ходов")

internal fun russianTiles(count: Int) = "$count " + russianPlural(count, "плитка", "плитки", "плиток")
