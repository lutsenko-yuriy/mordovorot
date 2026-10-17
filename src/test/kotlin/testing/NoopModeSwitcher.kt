package testing

import InputMode
import viewmodel.ModeSwitcher

/** No-op [ModeSwitcher] for a test View whose mode switching isn't under test. */
class NoopModeSwitcher : ModeSwitcher {
    override suspend fun switchTo(target: InputMode, trigger: String) {}
}
