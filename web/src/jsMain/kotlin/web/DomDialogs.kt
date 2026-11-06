package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import viewmodel.KeyboardLayout
import viewmodel.UnsolvableSaveChoice
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
                    choices(sizes.map { size -> button("${size}x$size") { game.onSizeChosen(size) }.focusKey(Focus.SizeChoice(size)) })
                append(sizeButtons(dialog.sizes))
                if (dialog.veryLarge.isNotEmpty()) append(
                    element("div", "section").apply {
                        append(text("section-title", VERY_LARGE_TITLE), text("section-note", VERY_LARGE_WARNING), sizeButtons(dialog.veryLarge))
                    },
                )
                append(actions(button("Cancel", "secondary") { game.onSizeChosen(null) }))
            }
            is WebDialog.Save -> {
                val name = (element("input") as HTMLInputElement).apply {
                    value = dialog.name
                    named(this, "Save name")
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
                        button("Cancel", "secondary") { game.onSaveAnswered(null) },
                        button("Save") { game.onSaveAnswered(name.value) },
                    ),
                )
            }
            is WebDialog.Load -> {
                dialog.message?.let { append(text("message", it)) }
                if (dialog.saves.isEmpty()) append(text("message", "No saves found."))
                else append(choices(dialog.saves.mapIndexed { i, save ->
                    element("div", "save-row").apply {
                        append(
                            button(save.label) { game.onLoadChosen(save.name) }.focusKey(Focus.SaveChoice(i)),
                            iconButton(save.deleteName, TRASH_ICON) { game.onDeletePressed(i) }.focusKey(Focus.DeleteSave(i)),
                        )
                    }
                }, "list"))
                append(actions(button("Cancel", "secondary") { game.onLoadChosen(null) }.focusKey(Focus.DialogCancel)))
            }
            is WebDialog.DeleteSave -> append(
                text("message", "This can't be undone."),
                actions(
                    button("Delete") { game.onDeleteAnswered(true) },
                    button("Cancel", "secondary") { game.onDeleteAnswered(false) }.focusKey(Focus.DeleteCancel),
                ),
            )
            is WebDialog.Unsolvable -> append(
                text("message", dialog.message),
                actions(
                    button("New game", "secondary") { game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME) },
                    button("Repair & load") { game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR) }.focusKey(Focus.Repair),
                ),
            )
            is WebDialog.Labels -> append(
                *listOfNotNull(dialog.message?.let { text("message", it) }).toTypedArray(),
                choices(dialog.options.map { labelRow(it, it.labels == dialog.selected, game) }, "list labels").also { asRadioGroup(it, "Tile labels") },
                actions(
                    button("Cancel", "secondary") { game.labels.onAnswered(ok = false) },
                    button("OK") { game.labels.onAnswered(ok = true) },
                ),
            )
            is WebDialog.Controls -> append(
                radios("Play with", PlayMode.entries.map { mode ->
                    radio(mode.title, mode == dialog.mode, Focus.ModeOption(mode)) { game.controls.onMode(mode) }
                }),
                radios("Keyboard keys", KeyboardLayout.entries.map { layout ->
                    radio(layout.title, layout == dialog.layout, Focus.LayoutOption(layout)) { game.controls.onLayout(layout) }
                }),
                element("div", "key-list").apply { dialog.keyList.forEach { append(text("key-line", it)) } },
                actions(
                    button("OK") { game.controls.onAnswered(ok = true) },
                    button("Cancel", "secondary") { game.controls.onAnswered(ok = false) },
                ),
            )
            is WebDialog.Share -> {
                val field = (element("input") as HTMLInputElement).apply {
                    value = dialog.link
                    readOnly = true
                    named(this, "Share link")
                    focusKey(Focus.ShareLinkField)
                    addEventListener("focus", { select() })
                }
                append(
                    text("message", dialog.note),
                    field,
                    *listOfNotNull(dialog.qr?.let(::qrElement)).toTypedArray(),
                    actions(
                        // A click with no pointer is the keyboard's Enter or Space.
                        (button(if (dialog.copied) "Copied!" else "Copy link") {}).apply {
                            addEventListener("click", { game.share.onCopyLink(fromKeyboard = (it as MouseEvent).detail == 0) })
                        }.focusKey(Focus.CopyLink),
                        button("Close", "secondary") { game.share.onClosed() },
                    ),
                )
            }
            is WebDialog.LinkRejected -> append(
                text("message", dialog.message),
                actions(button("OK") { game.share.onNoticeAnswered() }.focusKey(Focus.LinkRejectedOk)),
            )
            is WebDialog.OpenShared -> append(
                text("message", "Your moves since the last save will be lost."),
                actions(
                    button("Cancel", "secondary") { game.share.onOpenAnswered(false) }.focusKey(Focus.OpenSharedCancel),
                    button("Open") { game.share.onOpenAnswered(true) },
                ),
            )
            WebDialog.GiveUp -> append(
                actions(
                    button("No", "secondary") { game.solution.onGiveUpAnswered(false) }.focusKey(Focus.No),
                    button("Yes") { game.solution.onGiveUpAnswered(true) },
                ),
            )
        }
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
