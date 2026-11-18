package web

import org.w3c.dom.Element
import viewmodel.TileLabelGroup
import web.a11y.asDisclosure
import web.a11y.asRadio
import web.a11y.asRadioGroup
import web.a11y.focusKey
import web.a11y.hiddenFromReaders
import web.a11y.named
import web.a11y.waveSpans

/** The Tile labels dialog's body (GH-323): the message, the preview strip and the five groups, only the open one listing
 *  its schemes. */
internal fun labelsBody(dialog: WebDialog.Labels, game: WebGame, motion: LabelMotion): List<Element> =
    listOfNotNull(dialog.message?.let { text("message", it) }, previewStrip(dialog, motion)) +
        TileLabelGroup.entries.map { groupElement(it, dialog, game, motion) }

/** The marked scheme's first labels as tiles, seen but not read: the scheme's name says it all. */
private fun previewStrip(dialog: WebDialog.Labels, motion: LabelMotion) = hiddenFromReaders(element("div", "label-preview")).apply {
    // Row 0, columns 0 to 2 of a 3x3 board, on every board size as the strip is: the board's own shade ramp spans its side, so a larger board's first tiles differ slightly.
    dialog.preview.forEachIndexed { col, label ->
        append(element("span", "swatch").apply {
            // A label changing as the strip waves (GH-342) is the two spans of a wave.
            val waving = motion.swatch(col)
            if (waving == null) textContent = label else append(*waveSpans(label, waving, read = true).toTypedArray())
            setAttribute("style", "--row: 0; --col: $col; --side: 3")
        })
    }
}

private fun groupElement(group: TileLabelGroup, dialog: WebDialog.Labels, game: WebGame, motion: LabelMotion): Element {
    val open = group == dialog.open
    val sliding = motion.leavingGroup() != null && (open || group == motion.leavingGroup())
    return element("div", listOfNotNull("label-group", "open".takeIf { open }, "sliding".takeIf { sliding }).joinToString(" ")).apply {
        append(
            button(group.title, "group-header") { game.labels.onGroupOpened(group) }.focusKey(Focus.LabelGroup(group)).apply {
                asDisclosure(this, open, controls = if (open) "label-group-options" else null)
                if (!open) named(this, dialog.headerName(group))
            },
        )
        if (!open) {
            // The group that was open slides shut, as a copy of its rows that can't be reached (GH-342).
            if (group == motion.leavingGroup()) append(leavingBody(group, dialog, motion.slideDelayMs()))
            return@apply
        }
        val stopAt = dialog.openOptions.firstOrNull { it.labels == dialog.selected }?.labels ?: dialog.openOptions.firstOrNull { it.available }?.labels
        val choices = element("div", "choices list labels").apply {
            id = "label-group-options"
            asRadioGroup(this, group.title)
            dialog.openOptions.forEach { append(labelRow(it, it.labels == dialog.selected, it.labels == stopAt, game)) }
        }
        // The new group's rows slide in while the old one's slide out (GH-342).
        append(if (motion.leavingGroup() != null) enteringBody(choices, motion.slideDelayMs()) else choices)
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
