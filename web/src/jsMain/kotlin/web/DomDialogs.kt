package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import texts.texts
import viewmodel.KeyboardLayout
import viewmodel.UnsolvableSaveChoice
import web.a11y.asCheckbox
import web.a11y.asDialog
import web.a11y.asRadio
import web.a11y.asRadioGroup
import web.a11y.asRadioGroupLabelledBy
import web.a11y.focusKey
import web.a11y.hiddenFromReaders
import web.a11y.named

/** The modal dialog for [dialog], its answers going to [game] (GH-110). [onSaveWarning] gets the
 *  Save dialog's warning line, which [DomRenderer] updates in place while the player types. */
internal fun dialogElement(dialog: WebDialog, game: WebGame, onSaveWarning: (Element) -> Unit): Element =
    element("div", "dialog").apply {
        val title = text("title", dialog.title)
        asDialog(this, title)
        append(title)
        when (dialog) {
            is WebDialog.Size -> {
                fun sizeButtons(sizes: List<Int>) =
                    choices(sizes.map { size -> button(texts().boardSize(size)) { game.onSizeChosen(size) }.focusKey(Focus.SizeChoice(size)) })
                append(sizeButtons(dialog.sizes))
                if (dialog.veryLarge.isNotEmpty()) append(
                    element("div", "section").apply {
                        append(text("section-title", VERY_LARGE_TITLE), text("section-note", VERY_LARGE_WARNING), sizeButtons(dialog.veryLarge))
                    },
                )
                append(actions(button(texts().dialogs.cancel, "secondary") { game.onSizeChosen(null) }.focusKey(Focus.DialogCancel)))
            }
            is WebDialog.Save -> {
                val name = (element("input") as HTMLInputElement).apply {
                    value = dialog.name
                    named(this, texts().dialogs.saveNameName)
                    focusKey(Focus.SaveName)
                    addEventListener("input", { game.onSaveNameTyped(value) })
                    addEventListener("keydown", {
                        // An Enter that confirms an IME composition isn't the player's Save.
                        val key = it as KeyboardEvent
                        if (key.key == "Enter" && !key.composing) game.onSaveAnswered(value)
                    })
                }
                val warning = text("warning", dialog.warning ?: "")
                onSaveWarning(warning)
                append(
                    name,
                    warning,
                    actions(
                        button(texts().dialogs.cancel, "secondary") { game.onSaveAnswered(null) }.focusKey(Focus.DialogCancel),
                        button(texts().dialogs.save) { game.onSaveAnswered(name.value) }.focusKey(Focus.DialogOk),
                    ),
                )
            }
            is WebDialog.Load -> {
                dialog.message?.let { append(text("message", it)) }
                if (dialog.saves.isEmpty()) append(text("message", texts().dialogs.noSaves))
                else append(choices(dialog.saves.mapIndexed { i, save ->
                    element("div", "save-row").apply {
                        append(
                            button(save.label) { game.onLoadChosen(save.name) }.focusKey(Focus.SaveChoice(i)),
                            iconButton(save.deleteName, TRASH_ICON) { game.onDeletePressed(i) }.focusKey(Focus.DeleteSave(i)),
                        )
                    }
                }, "list"))
                append(actions(button(texts().dialogs.cancel, "secondary") { game.onLoadChosen(null) }.focusKey(Focus.DialogCancel)))
            }
            is WebDialog.DeleteSave -> append(
                text("message", texts().dialogs.deleteMessage),
                actions(
                    button(texts().dialogs.delete) { game.onDeleteAnswered(true) }.focusKey(Focus.DialogOk),
                    button(texts().dialogs.cancel, "secondary") { game.onDeleteAnswered(false) }.focusKey(Focus.DeleteCancel),
                ),
            )
            is WebDialog.Unsolvable -> append(
                text("message", dialog.message),
                actions(
                    button(texts().dialogs.newGame, "secondary") { game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME) }.focusKey(Focus.DialogCancel),
                    button(texts().dialogs.repairAndLoad) { game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR) }.focusKey(Focus.Repair),
                ),
            )
            is WebDialog.Labels -> append(
                *listOfNotNull(dialog.message?.let { text("message", it) }).toTypedArray(),
                choices(dialog.options.map { labelRow(it, it.labels == dialog.selected, game) }, "list labels").also { asRadioGroup(it, texts().dialogs.tileLabelsName) },
                actions(
                    button(texts().dialogs.cancel, "secondary") { game.labels.onAnswered(ok = false) }.focusKey(Focus.DialogCancel),
                    button(texts().dialogs.ok) { game.labels.onAnswered(ok = true) }.focusKey(Focus.DialogOk),
                ),
            )
            is WebDialog.Controls -> append(
                radios(texts().dialogs.keyboardKeysName, KeyboardLayout.entries.map { layout ->
                    radio(layout.title, layout == dialog.layout, Focus.LayoutOption(layout)) { game.controls.onLayout(layout) }
                }),
                element("div", "key-list").apply { dialog.keyList.forEach { append(text("key-line", it)) } },
                actions(
                    button(texts().dialogs.ok) { game.controls.onAnswered(ok = true) }.focusKey(Focus.DialogOk),
                    button(texts().dialogs.cancel, "secondary") { game.controls.onAnswered(ok = false) }.focusKey(Focus.DialogCancel),
                ),
            )
            is WebDialog.Share -> {
                val field = (element("input") as HTMLInputElement).apply {
                    value = dialog.link
                    readOnly = true
                    named(this, texts().dialogs.shareLinkName)
                    focusKey(Focus.ShareLinkField)
                    addEventListener("focus", { select() })
                }
                append(
                    text("message", dialog.note),
                    field,
                    *listOfNotNull(dialog.qr?.let(::qrElement)).toTypedArray(),
                    toggle(dialog.labelsToggle, dialog.includeLabels, enabled = true, Focus.ShareLabels) { game.share.onLabelsToggled() },
                    toggle(dialog.progressToggle, dialog.includeProgress, dialog.progressAvailable, Focus.ShareProgress) { game.share.onProgressToggled() },
                    actions(
                        // A click with no pointer is the keyboard's Enter or Space.
                        (button(if (dialog.copied) texts().dialogs.copied else texts().dialogs.copyLink) {}).apply {
                            addEventListener("click", { game.share.onCopyLink(fromKeyboard = (it as MouseEvent).detail == 0) })
                        }.focusKey(Focus.CopyLink),
                        button(texts().dialogs.close, "secondary") { game.share.onClosed() }.focusKey(Focus.DialogCancel),
                    ),
                )
            }
            is WebDialog.LinkRejected -> append(
                text("message", dialog.message),
                actions(button(texts().dialogs.ok) { game.share.onNoticeAnswered() }.focusKey(Focus.LinkRejectedOk)),
            )
            is WebDialog.OpenShared -> append(
                text("message", texts().dialogs.openSharedMessage),
                actions(
                    button(texts().dialogs.cancel, "secondary") { game.share.onOpenAnswered(false) }.focusKey(Focus.OpenSharedCancel),
                    button(texts().dialogs.open) { game.share.onOpenAnswered(true) }.focusKey(Focus.DialogOk),
                ),
            )
            is WebDialog.SharedLabels -> append(
                text("message", dialog.message),
                actions(
                    button(texts().dialogs.no, "secondary") { game.share.onSharedLabelsAnswered(false) }.focusKey(Focus.SharedLabelsNo),
                    button(texts().dialogs.yes) { game.share.onSharedLabelsAnswered(true) }.focusKey(Focus.SharedLabelsYes),
                ),
            )
            WebDialog.Update -> append(
                text("message", texts().dialogs.updateMessage),
                actions(
                    button(texts().dialogs.cancel, "secondary") { game.updates.onUpdateAnswered(false) }.focusKey(Focus.UpdateCancel),
                    button(texts().dialogs.update) { game.updates.onUpdateAnswered(true) }.focusKey(Focus.DialogOk),
                ),
            )
            WebDialog.ReplaySave -> append(
                text("message", WebDialog.ReplaySave.message),
                actions(
                    button(texts().dialogs.cancel, "secondary") { game.tutorial.onReplayAnswered(ReplaySaveChoice.CANCEL) }.focusKey(Focus.DialogCancel),
                    button(texts().dialogs.dontSave, "secondary") { game.tutorial.onReplayAnswered(ReplaySaveChoice.DONT_SAVE) }.focusKey(Focus.ReplayDontSave),
                    button(texts().dialogs.save) { game.tutorial.onReplayAnswered(ReplaySaveChoice.SAVE) }.focusKey(Focus.ReplaySaveSave),
                ),
            )
            WebDialog.ReplaySave -> append(
                text("message", WebDialog.ReplaySave.message),
                actions(
                    button("Cancel", "secondary") { game.tutorial.onReplayAnswered(ReplaySaveChoice.CANCEL) }.focusKey(Focus.DialogCancel),
                    button("Don't save", "secondary") { game.tutorial.onReplayAnswered(ReplaySaveChoice.DONT_SAVE) }.focusKey(Focus.ReplayDontSave),
                    button("Save") { game.tutorial.onReplayAnswered(ReplaySaveChoice.SAVE) }.focusKey(Focus.ReplaySaveSave),
                ),
            )
            WebDialog.GiveUp -> append(
                actions(
                    button(texts().dialogs.no, "secondary") { game.solution.onGiveUpAnswered(false) }.focusKey(Focus.No),
                    button(texts().dialogs.yes) { game.solution.onGiveUpAnswered(true) }.focusKey(Focus.DialogOk),
                ),
            )
        }
    }

/** One of the Share dialog's toggles (GH-267, GH-272): a switch drawn in CSS, hidden from readers, and [text] as its name.
 *  Dimmed and out of the Tab order when not [enabled]. */
private fun toggle(text: String, on: Boolean, enabled: Boolean, focus: Focus, onClick: () -> Unit) =
    button("", "toggle") { onClick() }.focusKey(focus).apply {
        asCheckbox(this, on, unavailable = !enabled)
        append(
            element("span", "name").apply { textContent = text },
            hiddenFromReaders(element("span", if (on) "switch on" else "switch")).apply { append(element("span", "knob")) },
        )
    }

/** A trash can, drawn in the button's text colour and hidden from readers by iconButton: the button carries the name. */
private const val TRASH_ICON =
    """<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" """ +
        """stroke-linecap="round" stroke-linejoin="round" focusable="false">""" +
        """<path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M6 6l1 14h10l1-14"/><path d="M10 10v6"/><path d="M14 10v6"/></svg>"""

/** A radio-role button, since the page is rebuilt every frame and a native radio would lose its state. */
private fun labelRow(option: LabelOption, checked: Boolean, game: WebGame) =
    button("", if (checked) "checked" else null) { game.labels.onSelected(option.labels) }.focusKey(Focus.Label(option.labels)).apply {
        asRadio(this, checked, unavailable = !option.available)
        append(
            element("span", "mark").apply { textContent = if (checked) "●" else "○" },
            element("span", "name").apply { textContent = option.title },
            element("span", "sample").apply { textContent = option.sample.joinToString(" ") { it.padEnd(2) } },
        )
    }

/** A group of radios, one Tab stop: the arrows move its mark (DomKeyboard). */
private fun radios(title: String, options: List<Element>) = element("div", "radios").apply {
    val titleId = "radios-${title.lowercase().replace(' ', '-')}"
    asRadioGroupLabelledBy(this, titleId)
    append(text("radios-title", title).apply { id = titleId })
    append(element("div", "options").apply { options.forEach { append(it) } })
}

/** Only the [checked] radio is a Tab stop. */
private fun radio(title: String, checked: Boolean, focus: Focus, onClick: () -> Unit) =
    button("", if (checked) "checked" else null, onClick).focusKey(focus).apply {
        asRadio(this, checked, rovingTabStop = true)
        append(
            hiddenFromReaders(element("span", "mark").apply { textContent = if (checked) "●" else "○" }),
            element("span", "name").apply { textContent = title },
        )
    }

/** Escape cancels the open dialog. An Escape that drops an IME composition isn't the player's, and
 *  a held one would cancel the next dialog too (Restore -> the size picker). [game] is read only
 *  on a key, as in [web.replayButtons]. */
internal fun listenForEscape(root: Element, game: () -> WebGame) {
    document.addEventListener("keydown", { event ->
        val key = event as KeyboardEvent
        if (key.key == "Escape" && !key.composing && !key.repeat && root.querySelector(".dialog") != null) {
            key.preventDefault()
            game().onEscape()
        }
    })
}

private fun choices(buttons: List<Element>, kind: String? = null) =
    element("div", listOfNotNull("choices", kind).joinToString(" ")).apply { buttons.forEach { append(it) } }

private fun actions(vararg buttons: Element) = element("div", "actions").apply { append(*buttons) }
