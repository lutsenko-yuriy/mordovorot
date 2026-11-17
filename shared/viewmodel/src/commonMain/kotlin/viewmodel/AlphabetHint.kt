package viewmodel

/** What the alphabet hint shows (GH-332): the [group]'s whole alphabet as [letters] in order, the first [used] of them in use on the board. This is
 *  the one place that decides it, so a client draws the list and greys the rest. */
data class AlphabetHint(val group: TileLabelGroup, val letters: List<String>, val used: Int) {
    companion object {
        /** The hint for a [side]x[side] board labelled with [labels]; `null` for the number schemes, which have no alphabet, and for a scheme too small for the board (Numbers stand in there). */
        fun of(labels: TileLabels, side: Int): AlphabetHint? {
            if (!labels.fits(side)) return null
            val used = labels.lettersUsed(side) ?: return null
            val group = TileLabelGroup.of(labels)
            return AlphabetHint(group, group.alphabet!!.map { it.toString() }, used)
        }
    }
}
