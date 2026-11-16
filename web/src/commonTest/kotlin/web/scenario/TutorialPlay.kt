@file:OptIn(ExperimentalCoroutinesApi::class)

package web.scenario

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import web.Arrow
import web.Focus
import web.drag.Axis
import web.storage.InMemoryKeyValueStore
import web.tutorial.TutorialScript

/** Tiles this far apart, in pixels. */
private const val PITCH = 40.0

/** A first launch (GH-288) on [store], with no save. */
internal fun tutorialPage(scope: CoroutineScope, store: InMemoryKeyValueStore = InMemoryKeyValueStore(), touch: Boolean = false, address: FakePageAddress = FakePageAddress()) =
    WebHarness(scope, store = store, touch = touch, address = address, firstLaunch = true).start()

/** The tutorial's solved board, 1-based. */
internal val TUTORIAL_SOLVED = shown(solved(TutorialScript.SIDE))

/** The tutorial's step [n] (1 to 4) made by arrow clicks. */
internal fun WebHarness.stepByArrow(n: Int) {
    when (n) {
        1 -> game.onArrow(Arrow.LEFT, 0)
        2 -> game.onArrow(Arrow.UP, 1)
        3 -> repeat(2) { game.onArrow(Arrow.RIGHT, 2) }
        4 -> game.onArrow(Arrow.DOWN, 3)
    }
}

/** Steps 1 to [n] by arrow clicks. */
internal fun WebHarness.playSteps(n: Int = 4) = (1..n).forEach(::stepByArrow)

/** The tutorial's step [n] by one drag. */
internal fun WebHarness.stepByDrag(n: Int) {
    when (n) {
        1 -> dragLine(Axis.ROW, 0, -1)
        2 -> dragLine(Axis.COLUMN, 1, -1)
        3 -> dragLine(Axis.ROW, 2, 2)
        4 -> dragLine(Axis.COLUMN, 3, 1)
    }
}

/** The tutorial's step [n] by keys: the selection starts on row 1, column 1. */
internal fun WebHarness.stepByKey(n: Int) {
    fun key(name: String) = press(name, from = Focus.Board)
    when (n) {
        1 -> key("ArrowLeft")
        2 -> { key("d"); key("ArrowUp") }
        3 -> { key("s"); key("s"); key("ArrowRight"); key("ArrowRight") }
        4 -> { key("d"); key("d"); key("ArrowDown") }
    }
}

/** Presses the first tile of [line] and drags [tiles] tiles along [axis] (negative is left or up). */
internal fun WebHarness.dragLine(axis: Axis, line: Int, tiles: Int) {
    val (row, col) = if (axis == Axis.ROW) line to 0 else 0 to line
    game.onDragStart(row, col, 100.0, 100.0, PITCH)
    val to = 100.0 + PITCH * tiles
    if (axis == Axis.ROW) game.onDragMove(to, 100.0) else game.onDragMove(100.0, to)
    game.onDragEnd()
}

/** Step 5's pause ends. */
internal fun TestScope.waitPause() {
    advanceTimeBy(3_000)
    runCurrent()
}
