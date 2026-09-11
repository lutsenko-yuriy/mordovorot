package testing

import presenter.TuiPresenter

/**
 * A [TuiPresenter] test double that records every call it receives. Used to verify
 * [view.tui.TuiView] dispatches clicks and delegates correctly, without depending on real
 * presenter/board logic. Replaces the TUI half of the old dual-interface `FakePresenter`
 * (GH-23).
 */
class FakeTuiPresenter : RecordingPresenter(), TuiPresenter {

    /** Scripts [listSaves]'s return value. */
    var saveNames: List<String> = emptyList()

    /** Scripts which names [saveExists] reports as already taken. */
    var existingSaveNames: Set<String> = emptySet()

    /** Scripts [isSolved]'s return value. */
    var solved: Boolean = false

    /** Scripts [boardState]'s return value. */
    var board: IntArray = IntArray(16) { it }

    /** Scripts [squareSide]'s return value. */
    var side: Int = 4

    override fun listSaves(): List<String> {
        calls.add("listSaves")
        return saveNames
    }

    override fun saveExists(name: String): Boolean {
        calls.add("saveExists($name)")
        return name in existingSaveNames
    }

    override fun isSolved(): Boolean {
        calls.add("isSolved")
        return solved
    }

    override fun restoreOnStartup() {
        calls.add("restoreOnStartup")
    }

    override fun boardState(): IntArray {
        calls.add("boardState")
        return board
    }

    override fun squareSide(): Int {
        calls.add("squareSide")
        return side
    }
}
