package web

import texts.texts
import viewmodel.KeyboardLayout
import viewmodel.TileLabelGroup
import viewmodel.TileLabels
import web.a11y.FocusTarget

/** A control the page focuses once a frame is drawn (GH-163), or keeps focused across one (GH-164);
 *  [key] is its `data-focus`. */
sealed class Focus(override val key: String) : FocusTarget {
    /** The [arrow] shifting [line], 0-based. */
    data class ArrowButton(val arrow: Arrow, val line: Int) : Focus("arrow-${arrow.name.lowercase()}-$line")
    /** The hidden Read board button (GH-167). */
    data object ReadBoard : Focus("read-board")
    data class SizeChoice(val side: Int) : Focus("size-$side")
    data object SaveName : Focus("save-name")
    /** The load button of the Load dialog's row at [index]. */
    data class SaveChoice(val index: Int) : Focus("save-$index")
    /** The trash button of the Load dialog's row at [index] (GH-251). */
    data class DeleteSave(val index: Int) : Focus("delete-save-$index")
    /** The Delete confirmation's Cancel, where it opens (GH-251). */
    data object DeleteCancel : Focus("delete-cancel")
    /** A dialog's Cancel or Close, and Load's when it has no saves to focus. Every dialog button has a key, so a
     *  redraw under an open dialog finds the one that had focus again (GH-289); unique only within one dialog. */
    data object DialogCancel : Focus("dialog-cancel")
    /** A dialog's primary button (OK, Save, Delete, Open, Update, Yes) where it has no key of its own (GH-289). */
    data object DialogOk : Focus("dialog-ok")
    /** The save question's Save and Don't save, before the tutorial replays (GH-289); its Cancel is [DialogCancel]. */
    data object ReplaySaveSave : Focus("replay-save")
    data object ReplayDontSave : Focus("replay-dont-save")
    /** The toolbar's `[ Restart ]` (GH-349). */
    data object RestartButton : Focus("restart")
    /** The Give Up row's `[ Hint ]` (GH-332). */
    data object AlphabetHintButton : Focus("alphabet-hint")
    /** The Give Up row's `[ Tutorial ]` (GH-289). */
    data object TutorialReplay : Focus("tutorial-replay")
    data object Repair : Focus("repair")
    data object LinkRejectedOk : Focus("link-rejected-ok")
    data object OpenSharedCancel : Focus("open-shared-cancel")
    /** The shared labels prompt's Yes, where it opens, and its No (GH-267). */
    data object SharedLabelsYes : Focus("shared-labels-yes")
    data object SharedLabelsNo : Focus("shared-labels-no")
    data object CopyLink : Focus("copy-link")
    data object ShareLinkField : Focus("share-link")
    /** The Share dialog's `Include my tile labels` toggle (GH-267). */
    data object ShareLabels : Focus("share-labels")
    /** The Share dialog's `Include my progress` toggle (GH-272). */
    data object ShareProgress : Focus("share-progress")
    data class Label(val labels: TileLabels) : Focus("label-${labels.name.lowercase()}")
    /** A Tile labels group's header, which opens it (GH-323). */
    data class LabelGroup(val group: TileLabelGroup) : Focus("label-group-${group.name.lowercase()}")
    data object No : Focus("no")
    data object NewButton : Focus("new")
    data object SaveButton : Focus("save")
    data object LoadButton : Focus("load")
    data object LabelsButton : Focus("labels")
    data object ControlsButton : Focus("controls")
    data object ShareButton : Focus("share")
    /** The board, one Tab stop on a computer (GH-171). */
    data object Board : Focus("board") {
        override val fallsBack get() = true
    }
    data class LayoutOption(val layout: KeyboardLayout) : Focus("layout-${layout.wireName}")
    /** Back on the control a digit was pressed on, whichever it was (GH-171), or what [fallbackFocus] names. */
    data class Restore(val control: String) : Focus(control) {
        override val fallsBack get() = true
    }
    /** The tutorial's panel buttons (GH-288). */
    data object TutorialControls : Focus("tutorial-controls") {
        override val fallsBack get() = true
    }
    data object TutorialLabels : Focus("tutorial-labels") {
        override val fallsBack get() = true
    }
    data object TutorialSkip : Focus("tutorial-skip") {
        override val fallsBack get() = true
    }
    /** The footer's Update button (GH-262), outside `#app`. */
    data object UpdateLink : Focus("update")
    /** The update confirmation's Cancel, where it opens (GH-262). */
    data object UpdateCancel : Focus("update-cancel")
    /** The footer's link at [index], outside `#app`. */
    data class FooterLink(val index: Int) : Focus("footer-$index")
    /** Give Up, or Mordie's solution after a win. */
    data object GiveUpRow : Focus("give-up")
    data object ReplayNext : Focus("replay-next")
    data object ReplayPrev : Focus("replay-prev")
    data object ReplayRestart : Focus("replay-restart")
    /** No control: the start of the page, as on a fresh load. */
    data object PageStart : Focus("page-start")
}

/** The control [dialog] opens focused on. */
fun defaultFocus(dialog: WebDialog): Focus = when (dialog) {
    is WebDialog.Size -> Focus.SizeChoice(dialog.current)
    is WebDialog.Save -> Focus.SaveName
    is WebDialog.Load -> dialog.focus ?: if (dialog.saves.isEmpty()) Focus.DialogCancel else Focus.SaveChoice(0)
    is WebDialog.DeleteSave -> Focus.DeleteCancel
    is WebDialog.Unsolvable -> Focus.Repair
    is WebDialog.Labels -> labelsFocus(dialog)
    WebDialog.GiveUp -> Focus.No
    is WebDialog.Controls -> Focus.LayoutOption(dialog.layout)
    is WebDialog.LinkRejected -> Focus.LinkRejectedOk
    is WebDialog.OpenShared -> Focus.OpenSharedCancel
    is WebDialog.SharedLabels -> Focus.SharedLabelsYes
    is WebDialog.Share -> Focus.CopyLink
    WebDialog.Update -> Focus.UpdateCancel
    WebDialog.ReplaySave, WebDialog.RestartSave -> Focus.DialogCancel
    is WebDialog.AlphabetHint -> Focus.DialogOk
}

/** The marked scheme when the open group holds it, else the open group's first scheme that fits the board (GH-323). */
private fun labelsFocus(dialog: WebDialog.Labels): Focus {
    if (TileLabelGroup.of(dialog.selected) == dialog.open) return Focus.Label(dialog.selected)
    val first = dialog.openOptions.firstOrNull { it.available }
    return if (first != null) Focus.Label(first.labels) else Focus.LabelGroup(dialog.open)
}

/** The Very large section's title and warning line in the size picker (GH-207). */
val VERY_LARGE_TITLE: String get() = texts().dialogs.veryLargeTitle
val VERY_LARGE_WARNING: String get() = texts().dialogs.veryLargeWarning

/** The dialog's title, which also names it for screen readers. */
val WebDialog.title: String get() = with(texts().dialogs) {
    when (this@title) {
        is WebDialog.Size -> sizeTitle
        is WebDialog.Save -> saveTitle
        is WebDialog.Load -> if (restore) restoreTitle else loadTitle
        is WebDialog.DeleteSave -> deleteTitle(name)
        is WebDialog.Unsolvable -> unsolvableTitle
        is WebDialog.Labels -> labelsTitle
        WebDialog.GiveUp -> giveUpTitle
        is WebDialog.Controls -> controlsTitle
        is WebDialog.LinkRejected -> linkRejectedTitle
        is WebDialog.OpenShared -> openSharedTitle
        is WebDialog.SharedLabels -> sharedLabelsTitle(labels.title)
        is WebDialog.Share -> shareTitle
        WebDialog.Update -> updateTitle
        WebDialog.ReplaySave, WebDialog.RestartSave -> replaySaveTitle
        is WebDialog.AlphabetHint -> alphabetTitle
    }
}
