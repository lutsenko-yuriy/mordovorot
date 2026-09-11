package testing

import presenter.Presenter

/**
 * Shared [Presenter] core recording behind [FakeConsolePresenter] and [FakeTuiPresenter] - the
 * six shared-core methods (shift/reset/save/load/exit) both fakes record identically, split out
 * of the old dual-interface `FakePresenter` (GH-23).
 */
abstract class RecordingPresenter : Presenter {

    val calls = mutableListOf<String>()

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
}
