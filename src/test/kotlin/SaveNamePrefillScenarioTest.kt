import kotlin.test.Test

/** GH-99: the Save dialog opens prefilled with the current save name - set by a save, a load
 *  (Load dialog, startup restore, Repair & load), cleared by a new game. Real BoardImpl +
 *  ViewModelImpl + FileSaveRepository (temp dir), driven through FakeTerminal in keyboard mode,
 *  as in UnsolvableSaveScenarioTest. */
class SaveNamePrefillScenarioTest {

    @Test
    fun `load then save prefills the loaded name and overwrites it`() {
        // TODO: seed foo.save, launch with --size so the startup restore is skipped
        // TODO: F6 -> load foo
        // TODO: one shift
        // TODO: F5 -> the Save dialog shows `Name: foo_` and the "already exists - it will be overwritten" warning
        // TODO: Enter -> foo.save holds the shifted board, no other save file appears
        // TODO: screen_save_dialog.prefilled == true, save_command_used.name_source == "prefill"
    }

    @Test
    fun `saving under a new name makes it the next prefill`() {
        // TODO: seed foo.save, launch with --size, F6 -> load foo
        // TODO: F5, type x -> the field reads `foox`; Enter saves foox.save, name_source == "edited"
        // TODO: F5 -> the field shows `Name: foox_`
    }

    @Test
    fun `startup restore prefills the restored name`() {
        // TODO: seed foo.save, launch without --size
        // TODO: restore foo from the startup restore dialog
        // TODO: F5 -> the field shows `Name: foo_`
    }

    @Test
    fun `repair and load prefills the repaired save name`() {
        // TODO: seed the unsolvable 3x3 odd.save, launch without --size
        // TODO: restore odd at startup, choose Repair & load
        // TODO: F5 -> the field shows `Name: odd_`
    }

    @Test
    fun `new game clears the prefill`() {
        // TODO: seed foo.save, launch with --size, F6 -> load foo
        // TODO: F9 -> pick a size
        // TODO: F5 -> the field shows `Name: _`; Esc
        // TODO: the last screen_save_dialog has prefilled == false
    }

    @Test
    fun `new game from the unsolvable dialog clears the prefill`() {
        // TODO: seed foo.save and the unsolvable odd.save, launch with --size, F6 -> load foo
        // TODO: F6 -> load odd, choose New game, pick a size
        // TODO: F5 -> the field shows `Name: _`
    }

    @Test
    fun `exit flow save dialog is prefilled`() {
        // TODO: seed foo.save, launch with --size, F6 -> load foo, one shift
        // TODO: Esc -> Yes -> the Save dialog shows `Name: foo_`
        // TODO: Enter -> foo.save holds the shifted board and the session ends
    }

    @Test
    fun `exit re-prompt shows the explanation and the overwrite warning together`() {
        // TODO: seed foo.save, launch with --size, F6 -> load foo
        // TODO: Esc -> Yes, edit the name to `foo b`, Enter
        // TODO: the Save dialog reopens prefilled with `foo`, showing both "isn't a usable save name" and "already exists"
    }

    @Test
    fun `clearing the prefilled field cancels without saving`() {
        // TODO: seed foo.save, launch with --size, F6 -> load foo, one shift
        // TODO: F5, Backspace x3 -> the field is empty
        // TODO: Enter -> foo.save is unchanged, no save_command_used event
    }

    @Test
    fun `switching input mode keeps the prefill`() {
        // TODO: seed foo.save, launch with --size, F6 -> load foo
        // TODO: F7 -> mouse mode
        // TODO: click [ Save ] -> the field shows `Name: foo_`
    }

    @Test
    fun `a failed load leaves the prefill unchanged`() {
        // TODO: seed foo.save and a corrupted bad.save, launch with --size, F6 -> load foo
        // TODO: F6 -> load bad -> the error message shows
        // TODO: F5 -> the field shows `Name: foo_`
    }
}
