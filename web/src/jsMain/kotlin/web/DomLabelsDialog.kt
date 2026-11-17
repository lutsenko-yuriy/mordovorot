package web

import org.w3c.dom.Element
import viewmodel.TileLabelGroup
import web.a11y.asDisclosure
import web.a11y.asRadio
import web.a11y.asRadioGroup
import web.a11y.focusKey
import web.a11y.hiddenFromReaders
import web.a11y.named

/** The Tile labels dialog's body (GH-323): the message, the preview strip and the four groups, only the open one listing
 *  its schemes. */
internal fun labelsBody(dialog: WebDialog.Labels, game: WebGame): List<Element> =
    listOfNotNull(dialog.message?.let { text("message", it) }, previewStrip(dialog)) +
        TileLabelGroup.entries.map { groupElement(it, dialog, game) }

/** The marked scheme's first labels as tiles, seen but not read: the scheme's name says it all. */
private fun previewStrip(dialog: WebDialog.Labels) = hiddenFromReaders(element("div", "label-preview")).apply {
    // Row 0, columns 0 to 2 of a 3x3 board, on every board size as the strip is: the board's own shade ramp spans its side, so a larger board's first tiles differ slightly.
    dialog.preview.forEachIndexed { col, label ->
        append(element("span", "swatch").apply {
            textContent = label
            setAttribute("style", "--row: 0; --col: $col; --side: 3")
        })
    }
}

private fun groupElement(group: TileLabelGroup, dialog: WebDialog.Labels, game: WebGame): Element {
    val open = group == dialog.open
    return element("div", if (open) "label-group open" else "label-group").apply {
        append(
            button(group.title, "group-header") { game.labels.onGroupOpened(group) }.focusKey(Focus.LabelGroup(group)).apply {
                asDisclosure(this, open, controls = if (open) "label-group-options" else null)
                if (!open) named(this, dialog.headerName(group))
            },
        )
        if (!open) return@apply
        val stopAt = dialog.openOptions.firstOrNull { it.labels == dialog.selected }?.labels ?: dialog.openOptions.firstOrNull { it.available }?.labels
        append(
            element("div", "choices list labels").apply {
                id = "label-group-options"
                asRadioGroup(this, group.title)
                dialog.openOptions.forEach { append(labelRow(it, it.labels == dialog.selected, it.labels == stopAt, game)) }
            },
        )
    }
}

/** A radio-role button, since the page is rebuilt every frame and a native radio would lose its state. */
private fun labelRow(option: LabelOption, checked: Boolean, tabStop: Boolean, game: WebGame) =
    button("", if (checked) "checked" else null) { game.labels.onSelected(option.labels) }.focusKey(Focus.Label(option.labels)).apply {
        asRadio(this, checked, tabStop = tabStop && option.available, unavailable = !option.available)
        append(
            element("span", "name").apply { textContent = option.title },
            *listOfNotNull(option.limit?.let { element("span", "limit").apply { textContent = it } }).toTypedArray(),
        )
    }
