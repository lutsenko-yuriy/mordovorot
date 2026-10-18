package web.drag

/** Where the tiles of a line sit while it is shifted part of the way (GH-110). */
object WrapPositions {

    /** Tile `i` of a line of [side] tiles moved by [offset] tiles sits at `i + offset`, wrapped into
     *  `[0, side)`. A tile past `side - 1` straddles the end, so it also shows `side` earlier. */
    fun of(side: Int, offset: Double): List<Double> = List(side) { ((it + offset) % side + side) % side }
}
