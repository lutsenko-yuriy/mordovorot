package web.a11y

import org.w3c.dom.Element

// GH-201: every ARIA attribute the page sets, so the screen-reader markup is in one place.

internal const val TITLE_ID = "dialog-title"

/** [dialog] as a modal dialog named by its [title] (GH-163). */
internal fun asDialog(dialog: Element, title: Element) {
    dialog.setAttribute("role", "dialog")
    dialog.setAttribute("aria-modal", "true")
    dialog.setAttribute("aria-labelledby", TITLE_ID)
    title.id = TITLE_ID
}

internal fun named(element: Element, name: String) = element.setAttribute("aria-label", name)

/** [group] as a radio group named [name]. */
internal fun asRadioGroup(group: Element, name: String) {
    group.setAttribute("role", "radiogroup")
    named(group, name)
}

/** [group] as a radio group named by the element with [titleId]. */
internal fun asRadioGroupLabelledBy(group: Element, titleId: String) {
    group.setAttribute("role", "radiogroup")
    group.setAttribute("aria-labelledby", titleId)
}

/** [button] as a radio. [tabStop] makes it the group's one Tab stop (`true`) or none (`false`), as the arrows
 *  move the mark (GH-171); `null` leaves it a Tab stop like any button. */
internal fun asRadio(button: Element, checked: Boolean, tabStop: Boolean? = null) {
    button.setAttribute("role", "radio")
    button.setAttribute("aria-checked", checked.toString())
    if (tabStop != null) button.setAttribute("tabindex", if (tabStop) "0" else "-1")
}

/** [button] as a radio that can't be marked, as its scheme doesn't fit the board (GH-343). Not `disabled`, so it takes focus, and a press or an arrow
 *  shows why: its name carries the limit, which a cover over the row shows to the eye. [tabStop] while it shows. */
internal fun asUnavailableRadio(button: Element, tabStop: Boolean = false) {
    button.setAttribute("role", "radio")
    button.setAttribute("aria-checked", "false")
    button.setAttribute("aria-disabled", "true")
    // The row that shows its limit is a Tab stop while it does, so Tab and Shift+Tab leave it for its neighbours, not for the dialog's ends.
    button.setAttribute("tabindex", if (tabStop) "0" else "-1")
}

/** [header] as the button that opens a group of options (GH-323); its group is [expanded] while open, and the element with id [controls] when given. */
internal fun asDisclosure(header: Element, expanded: Boolean, controls: String? = null) {
    header.setAttribute("aria-expanded", expanded.toString())
    controls?.let { header.setAttribute("aria-controls", it) }
}

/** [button] as a check box, which its text names (GH-267); an [unavailable] one is disabled (GH-272). A button rather than a native check box: the page is rebuilt every
 *  frame, so a native one would lose its focus and state, as radios would (GH-139). */
internal fun asCheckbox(button: Element, checked: Boolean, unavailable: Boolean = false) {
    button.setAttribute("role", "checkbox")
    button.setAttribute("aria-checked", checked.toString())
    if (unavailable) {
        button.setAttribute("disabled", "")
        button.setAttribute("aria-disabled", "true")
    }
}

/** Seen, not read. */
internal fun <E : Element> hiddenFromReaders(element: E): E = element.apply { setAttribute("aria-hidden", "true") }

internal fun asList(list: Element) = list.setAttribute("role", "list")

/** [item] as a list item; the [current] one marks the step the player is on. */
internal fun asListItem(item: Element, current: Boolean) {
    item.setAttribute("role", "listitem")
    if (current) item.setAttribute("aria-current", "step")
}

/** The key that presses [element], heard as a shortcut. */
internal fun keyShortcut(element: Element, key: String) = element.setAttribute("aria-keyshortcuts", key)

/** The language picker (GH-275): a group named [name], whose toggle buttons say which is [pressed]. */
internal fun asPickerGroup(group: Element, name: String) {
    group.setAttribute("role", "group")
    named(group, name)
}

internal fun asToggle(button: Element, pressed: Boolean) = button.setAttribute("aria-pressed", pressed.toString())
