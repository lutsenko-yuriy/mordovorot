package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import testing.FakePreferenceRepository

class SessionKeyboardLayoutTest {

    private val store = FakePreferenceRepository()

    @Test
    fun `with no preference the layout is right-handed`() {
        assertEquals(KeyboardLayout.RIGHT_HANDED, SessionKeyboardLayout(KeyboardLayoutPreference(store)).current)
    }

    @Test
    fun `a stored preference is the starting layout`() {
        store.values["keyboard_layout"] = "left_handed"
        assertEquals(KeyboardLayout.LEFT_HANDED, SessionKeyboardLayout(KeyboardLayoutPreference(store)).current)
    }

    @Test
    fun `choose applies the layout and remembers it`() {
        val layout = SessionKeyboardLayout(KeyboardLayoutPreference(store))
        layout.choose(KeyboardLayout.LEFT_HANDED)
        assertEquals(KeyboardLayout.LEFT_HANDED, layout.current)
        assertEquals("left_handed", store.values["keyboard_layout"])
    }

    @Test
    fun `a failed write still applies the layout`() {
        store.setException = RuntimeException("read-only")
        val layout = SessionKeyboardLayout(KeyboardLayoutPreference(store))
        layout.choose(KeyboardLayout.LEFT_HANDED)
        assertEquals(KeyboardLayout.LEFT_HANDED, layout.current)
    }

    @Test
    fun `choosing the current layout writes nothing`() {
        SessionKeyboardLayout(KeyboardLayoutPreference(store)).choose(KeyboardLayout.RIGHT_HANDED)
        assertEquals(emptyMap(), store.values)
    }
}
