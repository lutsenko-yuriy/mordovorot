package viewmodel

/** What a share link says about the sender's game (GH-272): still [Solving], [Solved], or [GaveUp]. */
sealed interface SharedProgress {
    val moves: Int?

    /** The `shared_board_opened.progress` value. */
    val analyticsName: String

    data class Solving(override val moves: Int) : SharedProgress {
        override val analyticsName get() = "solving"
    }

    data class Solved(override val moves: Int) : SharedProgress {
        override val analyticsName get() = "solved"
    }

    /** [moves] is `null` for a solution-screen save from before GH-273, which kept no count. */
    data class GaveUp(override val moves: Int?) : SharedProgress {
        override val analyticsName get() = "gave_up"
    }
}
