package testing

import presenter.Presenter

/**
 * A [Presenter] test double that records every call it receives. Used to
 * verify [view.ViewImpl] parses console input and delegates correctly,
 * without depending on real presenter/board logic.
 */
class FakePresenter : Presenter {

    val calls = mutableListOf<String>()

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

    override fun play() {
        calls.add("play")
    }

    override fun shiftLeft(row: Int) {
        calls.add("shiftLeft($row)")
    }

    override fun shiftRight(row: Int) {
        calls.add("shiftRight($row)")
    }

    override fun shiftUp(col: Int) {
        calls.add("shiftUp($col)")
    }

    override fun shiftDown(col: Int) {
        calls.add("shiftDown($col)")
    }

    override fun resetGame() {
        calls.add("resetGame")
    }

    /** Always reports success - tests exercising a failed save use [testing.FakeSaveRepository]
     *  directly against [presenter.PresenterImpl], not this fake. */
    override fun saveGame(name: String): Boolean {
        calls.add("saveGame($name)")
        return true
    }

    override fun loadGame(name: String) {
        calls.add("loadGame($name)")
    }

    override fun exitGame() {
        calls.add("exitGame")
    }

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
