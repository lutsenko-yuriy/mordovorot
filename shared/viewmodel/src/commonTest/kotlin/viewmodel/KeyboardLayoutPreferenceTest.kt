package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import testing.FakePreferenceRepository

class KeyboardLayoutPreferenceTest {

    private val store = FakePreferenceRepository()
    private val preference = KeyboardLayoutPreference(store)

    @Test
    fun `nothing stored gives null`() {
        assertNull(preference.load())
    }

    @Test
    fun `a stored layout loads by its wire name`() {
        store.values["keyboard_layout"] = "left_handed"
        assertEquals(KeyboardLayout.LEFT_HANDED, preference.load())
    }

    @Test
    fun `an unknown name gives null`() {
        store.values["keyboard_layout"] = "dvorak"
        assertNull(preference.load())
    }

    @Test
    fun `a failed read gives null`() {
        store.getException = RuntimeException("unreadable")
        assertNull(preference.load())
    }

    @Test
    fun `save stores the wire name under keyboard_layout`() {
        assertTrue(preference.save(KeyboardLayout.RIGHT_HANDED))
        assertEquals(mapOf("keyboard_layout" to "right_handed"), store.values)
    }

    @Test
    fun `a failed write gives false`() {
        store.setException = RuntimeException("read-only")
        assertFalse(preference.save(KeyboardLayout.LEFT_HANDED))
    }
}
