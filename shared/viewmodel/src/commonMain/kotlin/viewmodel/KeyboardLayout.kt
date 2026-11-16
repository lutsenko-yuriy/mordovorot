package viewmodel

import texts.texts

/** Which hand keyboard mode's keys suit (GH-170, ADR-0003). [wireName] is the stored
 *  preference's value only: the layout is never sent to analytics. */
enum class KeyboardLayout(val wireName: String) {
    RIGHT_HANDED("right_handed"),
    LEFT_HANDED("left_handed"),
    ;

    val title: String get() = with(texts().page) { if (this@KeyboardLayout == RIGHT_HANDED) layoutRightHanded else layoutLeftHanded }

    companion object {
        val DEFAULT = RIGHT_HANDED

        fun fromWireName(name: String?): KeyboardLayout? = entries.firstOrNull { it.wireName == name }
    }
}
