package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers GH-18's keyboard dialog focus ring: the order of a [Dialog]'s focusable controls (text
 * field, then list rows, then buttons - each only if the dialog actually has one), and
 * [DialogFocus.next]/[DialogFocus.previous] wrapping around that ring. Pure geometry, no
 * terminal or presenter involved.
 */
class DialogFocusTest {

    private val saveButtons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel"))
    private val loadButtons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel"))
    private val exitButtons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel"))

    @Test
    fun `a Save dialog's ring is the text field then its buttons in order`() {
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", textFieldValue = "foo", buttons = saveButtons)
        val focus = DialogFocus(dialog)

        assertEquals(3, focus.size)
        assertEquals(DialogFocusTarget.TextField, focus.target(0))
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("save")), focus.target(1))
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("cancel")), focus.target(2))
    }

    @Test
    fun `a Load dialog's ring is its list rows then its buttons in order`() {
        val dialog = Dialog(Dialog.Kind.LOAD, "Load game", listItems = listOf("a", "b"), buttons = loadButtons)
        val focus = DialogFocus(dialog)

        assertEquals(4, focus.size)
        assertEquals(DialogFocusTarget.ListRow(0), focus.target(0))
        assertEquals(DialogFocusTarget.ListRow(1), focus.target(1))
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("load")), focus.target(2))
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("cancel")), focus.target(3))
    }

    @Test
    fun `a Load dialog with zero saves has a ring of buttons only`() {
        val dialog = Dialog(Dialog.Kind.LOAD, "Load game", message = "No saves found.", buttons = loadButtons)
        val focus = DialogFocus(dialog)

        assertEquals(2, focus.size)
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("load")), focus.target(0))
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("cancel")), focus.target(1))
    }

    @Test
    fun `an Exit dialog's ring is its buttons only, no text field or list rows`() {
        val dialog = Dialog(Dialog.Kind.EXIT, "Save before quitting?", buttons = exitButtons)
        val focus = DialogFocus(dialog)

        assertEquals(3, focus.size)
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("yes")), focus.target(0))
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("no")), focus.target(1))
        assertEquals(DialogFocusTarget.Button(HitTarget.DialogButton("cancel")), focus.target(2))
    }

    @Test
    fun `next and previous wrap around the ring in both directions`() {
        val dialog = Dialog(Dialog.Kind.EXIT, "Save before quitting?", buttons = exitButtons)
        val focus = DialogFocus(dialog)

        assertEquals(1, focus.next(0))
        assertEquals(2, focus.next(1))
        assertEquals(0, focus.next(2)) // wraps forward past the last item

        assertEquals(2, focus.previous(0)) // wraps backward past the first item
        assertEquals(1, focus.previous(2))
        assertEquals(0, focus.previous(1))
    }

    @Test
    fun `target returns null past the end of the ring`() {
        val dialog = Dialog(Dialog.Kind.EXIT, "Save before quitting?", buttons = exitButtons)
        val focus = DialogFocus(dialog)

        assertNull(focus.target(3))
        assertNull(focus.target(-1))
    }
}
