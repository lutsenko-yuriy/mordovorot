package testing

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import viewmodel.SaveInfo
import viewmodel.ShiftSource
import viewmodel.UiRequest
import viewmodel.ViewModel

/**
 * A [ViewModel] test double that records every call it receives. Used by `TuiView` tests to
 * verify click/key dispatch without depending on real viewModel/board logic.
 */
class FakeViewModel : ViewModel {

    /** Never written to - this fake records calls instead of calling back into a View, so
     *  nothing ever raises a [UiRequest] here (GH-42 WU2). */
    override val uiRequests: ReceiveChannel<UiRequest<*>> = Channel()

    val calls = mutableListOf<String>()

    /** Scripts [listSaves]'s return value. */
    var saveNames: List<String> = emptyList()

    /** Scripts [currentSaveName]'s return value (GH-99). */
    var currentSaveName: String? = null

    /** Scripts which names [saveExists] reports as already taken. */
    var existingSaveNames: Set<String> = emptySet()

    /** Scripts [isSolved]'s return value. */
    var solved: Boolean = false

    /** Scripts [boardState]'s return value. */
    var board: IntArray = IntArray(16) { it }

    /** Scripts [squareSide]'s return value. */
    var side: Int = 4

    /** Scripts [moveCount]'s return value. */
    var moves: Int = 0

    /** Scripts [mordieLine]'s return value. */
    var mordieLine: String? = null

    /** Scripts [mordieEnding]'s return value. */
    var mordieEnding: String? = null

    override fun shiftLeft(row: Int, source: ShiftSource?) {
        calls.add("shiftLeft($row" + sourceSuffix(source) + ")")
    }

    override fun shiftRight(row: Int, source: ShiftSource?) {
        calls.add("shiftRight($row" + sourceSuffix(source) + ")")
    }

    override fun shiftUp(col: Int, source: ShiftSource?) {
        calls.add("shiftUp($col" + sourceSuffix(source) + ")")
    }

    override fun shiftDown(col: Int, source: ShiftSource?) {
        calls.add("shiftDown($col" + sourceSuffix(source) + ")")
    }

    override fun shiftRow(row: Int, steps: Int, source: ShiftSource?) {
        calls.add("shiftRow($row, $steps" + sourceSuffix(source) + ")")
    }

    override fun shiftColumn(col: Int, steps: Int, source: ShiftSource?) {
        calls.add("shiftColumn($col, $steps" + sourceSuffix(source) + ")")
    }

    /** A keyboard shift records its key (GH-101): `shiftLeft(0, HJKL)`; a click stays `shiftLeft(0)`. */
    private fun sourceSuffix(source: ShiftSource?) = source?.let { ", $it" } ?: ""

    override fun newGame(size: Int, trigger: String) {
        calls.add("newGame($size, $trigger)")
    }

    /** Always reports success - tests exercising a failed save use [testing.FakeSaveRepository]
     *  directly against a real [viewmodel.ViewModelImpl], not this fake. */
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

    /** [SaveInfo.squareSide] is always `null` here - tests that care about size drive a real
     *  [viewmodel.ViewModelImpl] instead, same as save failures (see [saveGame]'s KDoc). */
    override fun listSaves(): List<SaveInfo> {
        calls.add("listSaves")
        return saveNames.map { SaveInfo(it, null) }
    }

    override fun currentSaveName(): String? = currentSaveName

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

    // Not recorded in [calls] - a read-only query, like [moveCount] below.
    override fun squareSide(): Int = side

    // Not recorded in [calls] - it's a read-only query like [boardState], but tests asserting
    // exact call lists predate it.
    override fun moveCount(): Int = moves

    // Not recorded in [calls] - read-only queries, like [moveCount].
    override fun mordieLine(): String? = mordieLine

    override fun mordieEnding(): String? = mordieEnding
}
