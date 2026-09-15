package testing

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import presenter.Presenter
import presenter.UiRequest

/**
 * A [Presenter] test double that records every call it receives. Used by both `ViewImpl` and
 * `TuiView` tests to verify command/click dispatch without depending on real presenter/board
 * logic - collapses GH-23's `RecordingPresenter`/`FakeConsolePresenter`/`FakeTuiPresenter` split
 * once the interfaces it mirrored collapsed the same way (GH-42 WU3).
 */
class FakePresenter : Presenter {

    /** Never written to - this fake records calls instead of calling back into a View, so
     *  nothing ever raises a [UiRequest] here (GH-42 WU2). */
    override val uiRequests: ReceiveChannel<UiRequest<*>> = Channel()

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
     *  directly against a real [presenter.PresenterImpl], not this fake. */
    override suspend fun saveGame(name: String): Boolean {
        calls.add("saveGame($name)")
        return true
    }

    override suspend fun loadGame(name: String) {
        calls.add("loadGame($name)")
    }

    override suspend fun exitGame() {
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

    override suspend fun restoreOnStartup() {
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
