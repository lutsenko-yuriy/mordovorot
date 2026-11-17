package texts.de

/** `1 Zug`, `2 Züge`; after «in» and «nach» the dative: `1 Zug`, `2 Zügen`. */
internal fun germanMoves(count: Int, dative: Boolean = false) =
    "$count " + if (count == 1) "Zug" else if (dative) "Zügen" else "Züge"

internal fun germanTiles(count: Int) = "$count " + if (count == 1) "Kachel" else "Kacheln"
