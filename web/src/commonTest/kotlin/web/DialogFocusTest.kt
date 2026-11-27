package web

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.AlphabetHint
import viewmodel.KeyboardLayout
import viewmodel.TileLabelGroup
import viewmodel.TileLabels

/** GH-163: each dialog's control focused on open, and its title, the dialog's accessible name. */
class DialogFocusTest {
    private val dialogs = texts().dialogs

    private val labels = WebDialog.Labels(listOf(LabelOption(TileLabels.PAIRS, "Pairs")), TileLabels.PAIRS)
    private val saves = listOf(SaveEntry("foo", "foo (4x4)"))
    private val controls = WebDialog.Controls(KeyboardLayout.LEFT_HANDED, emptyList())

    @Test
    fun `each dialog focuses its default control`() {
        assertEquals(Focus.SizeChoice(5), defaultFocus(WebDialog.Size(listOf(3, 4, 5, 6), current = 5)))
        assertEquals(Focus.SaveName, defaultFocus(WebDialog.Save("foo", null)))
        assertEquals(Focus.SaveChoice(0), defaultFocus(WebDialog.Load(restore = false, saves)))
        assertEquals(Focus.SaveChoice(0), defaultFocus(WebDialog.Load(restore = true, saves)))
        assertEquals(Focus.SaveChoice(2), defaultFocus(WebDialog.Load(restore = false, saves, focus = Focus.SaveChoice(2))))
        assertEquals(Focus.DeleteSave(1), defaultFocus(WebDialog.Load(restore = false, saves, focus = Focus.DeleteSave(1))))
        assertEquals(Focus.DeleteCancel, defaultFocus(WebDialog.DeleteSave("foo")))
        assertEquals(Focus.DialogCancel, defaultFocus(WebDialog.Load(restore = false, emptyList())))
        assertEquals(Focus.Repair, defaultFocus(WebDialog.Unsolvable("x")))
        assertEquals(Focus.Label(TileLabels.PAIRS), defaultFocus(labels))
        assertEquals(Focus.No, defaultFocus(WebDialog.GiveUp))
        assertEquals(Focus.LayoutOption(KeyboardLayout.LEFT_HANDED), defaultFocus(controls))
        assertEquals(Focus.UpdateCancel, defaultFocus(WebDialog.Update))
        assertEquals(Focus.DialogCancel, defaultFocus(WebDialog.RestartSave))
        assertEquals(Focus.DialogOk, defaultFocus(WebDialog.AlphabetHint(AlphabetHint.of(TileLabels.GREEK_LETTERS, 4)!!)))
        assertEquals(Focus.SharedLabelsYes, defaultFocus(WebDialog.SharedLabels(TileLabels.PAIRS)))
        assertEquals(Focus.DialogCancel, defaultFocus(WebDialog.Install(InstallDevice.MAC)))
    }

    @Test
    fun `each dialog is named by its title`() {
        assertEquals(dialogs.sizeTitle, WebDialog.Size(listOf(4), current = 4).title)
        assertEquals(dialogs.saveTitle, WebDialog.Save("", null).title)
        assertEquals(dialogs.loadTitle, WebDialog.Load(restore = false, saves).title)
        assertEquals(dialogs.restoreTitle, WebDialog.Load(restore = true, saves).title)
        assertEquals(dialogs.deleteTitle("foo"), WebDialog.DeleteSave("foo").title)
        assertEquals(dialogs.unsolvableTitle, WebDialog.Unsolvable("x").title)
        assertEquals(dialogs.labelsTitle, labels.title)
        assertEquals(dialogs.giveUpTitle, WebDialog.GiveUp.title)
        assertEquals(dialogs.controlsTitle, controls.title)
        assertEquals(dialogs.updateTitle, WebDialog.Update.title)
        assertEquals(dialogs.installTitle, WebDialog.Install(InstallDevice.PHONE_OR_TABLET).title)
        assertEquals(texts().page.footerInstall, WebDialog.Install(InstallDevice.MAC).title)
        assertEquals(dialogs.replaySaveTitle, WebDialog.RestartSave.title)
        assertEquals(dialogs.alphabetLatin, WebDialog.AlphabetHint(AlphabetHint.of(TileLabels.LETTERS, 4)!!).title)
        assertEquals(dialogs.alphabetCyrillic, WebDialog.AlphabetHint(AlphabetHint.of(TileLabels.CYRILLIC_LETTER_PAIRS, 4)!!).title)
        assertEquals(dialogs.alphabetGreek, WebDialog.AlphabetHint(AlphabetHint.of(TileLabels.GREEK_LETTERS, 4)!!).title)
        assertEquals(dialogs.sharedLabelsTitle(texts().page.labelPairsFrom0), WebDialog.SharedLabels(TileLabels.PAIRS_FROM_0).title)
    }

    @Test
    fun `a save entry names its trash button after the save`() {
        assertEquals(dialogs.deleteName("foo"), SaveEntry("foo", "foo (4x4)").deleteName)
    }

    @Test
    fun `an open group focuses its first scheme that fits when it holds no mark`() {
        val options = TileLabels.entries.map { LabelOption(it, TileLabelGroup.of(it).rowTitle(it), if (it == TileLabels.LETTERS) "up to 5x5" else null) }
        assertEquals(Focus.Label(TileLabels.LETTER_PAIRS), defaultFocus(WebDialog.Labels(options, TileLabels.NUMBERS, open = TileLabelGroup.LATIN)))
        assertEquals(Focus.Label(TileLabels.CYRILLIC_LETTERS), defaultFocus(WebDialog.Labels(options, TileLabels.NUMBERS, open = TileLabelGroup.CYRILLIC)))
        assertEquals(Focus.Label(TileLabels.PAIRS), defaultFocus(WebDialog.Labels(options, TileLabels.PAIRS)))
    }

    @Test
    fun `focus keys are distinct`() {
        val all = listOf(
            Focus.SizeChoice(3), Focus.SizeChoice(4), Focus.SaveName, Focus.SaveChoice(0), Focus.SaveChoice(1), Focus.DeleteSave(0), Focus.DeleteSave(1), Focus.DeleteCancel, Focus.DialogCancel, Focus.Repair,
            Focus.No, Focus.NewButton, Focus.SaveButton, Focus.LoadButton, Focus.LabelsButton, Focus.GiveUpRow, Focus.AlphabetHintButton, Focus.TutorialReplay,
            Focus.ReplayNext, Focus.ReplayPrev, Focus.ReplayRestart, Focus.PageStart, Focus.ControlsButton, Focus.Board,
            Focus.UpdateLink, Focus.UpdateCancel,
        ) + TileLabels.entries.map { Focus.Label(it) } + TileLabelGroup.entries.map { Focus.LabelGroup(it) } +
            KeyboardLayout.entries.map { Focus.LayoutOption(it) } +
            Arrow.entries.flatMap { arrow -> (0 until 6).map { Focus.ArrowButton(arrow, it) } }
        assertEquals(all.size, all.map { it.key }.toSet().size)
    }
}
