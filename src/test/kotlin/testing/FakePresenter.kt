package testing

import presenter.ConsolePresenter
import presenter.TuiPresenter

/**
 * A [presenter.Presenter] test double that records every call it receives. Used to verify
 * [view.ViewImpl] and [view.tui.TuiView] parse input and delegate correctly, without depending
 * on real presenter/board logic. Temporarily implements both [ConsolePresenter] and
 * [TuiPresenter] (GH-23 WU1) - WU2 splits this into `FakeConsolePresenter`/`FakeTuiPresenter`.
 */
class FakePresenter : ConsolePresenter, TuiPresenter {

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
     *  directly against [presenter.BasePresenter], not this fake. */
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
