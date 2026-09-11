package testing

import presenter.ConsolePresenter

/**
 * A [ConsolePresenter] test double that records every call it receives. Used to verify
 * [view.ViewImpl] parses input and delegates correctly, without depending on real
 * presenter/board logic. Replaces the console half of the old dual-interface `FakePresenter`
 * (GH-23).
 */
class FakeConsolePresenter : RecordingPresenter(), ConsolePresenter {

    override fun play() {
        calls.add("play")
    }
}
