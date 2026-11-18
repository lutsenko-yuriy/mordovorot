package mordie

/** What a save stores about Mordie; [solution] is its path when known (GH-126). A found answer that
 *  is not [provenShortest] also has [ruledOut], the largest length the exact search had ruled out (GH-224). */
class MordieSnapshot(
    val original: IntArray,
    val moves: Int,
    val found: Boolean,
    val solution: List<SolutionMove>?,
    val provenShortest: Boolean = true,
    val ruledOut: Int = 0,
)

/** The snapshot of [state] on [original]: [solution] and [ruledOut] as known now. */
internal fun snapshotFor(original: IntArray, state: MordieOutcome, solution: List<SolutionMove>?, ruledOut: Int): MordieSnapshot {
    // One move is always the shortest, and the save format has no R below 0 for it (GH-224).
    val unproven = state.found && !state.provenShortest && state.moves >= 2
    val kept = if (unproven) ruledOut.coerceIn(0, state.moves - 2) else 0
    return MordieSnapshot(original.copyOf(), state.moves, state.found, solution, provenShortest = !unproven, ruledOut = kept)
}
