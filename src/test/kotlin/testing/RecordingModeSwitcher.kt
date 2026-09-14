package testing

import InputMode
import presenter.ModeSwitcher

/** A [ModeSwitcher] test double that records every `switchTo` call instead of acting on it -
 *  never throws, so callers can be driven and inspected without unwinding via
 *  `ModeSwitchRequestedException`. */
class RecordingModeSwitcher : ModeSwitcher {

    data class Call(val target: InputMode, val trigger: String)

    val calls = mutableListOf<Call>()

    override fun switchTo(target: InputMode, trigger: String) {
        calls.add(Call(target, trigger))
    }
}
