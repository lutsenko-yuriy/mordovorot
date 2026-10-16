package testing

import mordie.MordieSearch
import mordie.MordieSearchFactory
import mordie.SearchProgress

/** A [MordieSearch] whose [progress] the test sets; records [cancel]. */
class FakeMordieSearch(val original: IntArray, val ruledOut: Int) : MordieSearch {

    var progress = SearchProgress(moves = ruledOut.coerceAtLeast(0), found = false, elapsedMs = 0)
    var cancelled = false
        private set

    override fun progress() = progress
    override fun cancel() {
        cancelled = true
    }
}

/** Records every search it starts; [latest] is the one a test scripts. */
class FakeMordieSearchFactory : MordieSearchFactory {

    val started = mutableListOf<FakeMordieSearch>()
    val latest: FakeMordieSearch get() = started.last()

    override fun start(original: IntArray, ruledOut: Int): MordieSearch =
        FakeMordieSearch(original.copyOf(), ruledOut).also { started += it }
}
