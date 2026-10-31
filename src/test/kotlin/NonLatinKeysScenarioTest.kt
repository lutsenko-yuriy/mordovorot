import kotlin.test.Test

/** GH-200: keyboard mode's letter keys on Ukrainian and Russian layouts, and those letters in save
 *  names. Real UTF-8 bytes go through `TerminalInputParser`, so its decoding is under test too. */
class NonLatinKeysScenarioTest {

    @Test
    fun `Cyrillic keys select and shift in the right-handed layout`() {
        // TODO: 1. Start keyboard mode on a scrambled 4x4.
        // TODO: 2. Send в and ф, then ф: the selection moves right, then left twice, wrapping to column 4.
        // TODO: 3. Send р: the selected row shifts left, and the selection stays put.
        // TODO: 4. Send Л: the selected column shifts up, so uppercase works too.
    }

    @Test
    fun `Cyrillic keys swap jobs in the left-handed layout`() {
        // TODO: 1. Start with the left-handed layout.
        // TODO: 2. Send д: the selection moves right.
        // TODO: 3. Send ф: the selected row shifts left.
        // TODO: 4. Send ц: the selected column shifts up.
    }

    @Test
    fun `Ukrainian і and Russian ы both act as S`() {
        // TODO: 1. Send і: the selection moves down.
        // TODO: 2. Send ы: the selection moves down again.
    }

    @Test
    fun `a shift with р counts as hjkl in analytics`() {
        // TODO: 1. Start one shift from solved, right-handed.
        // TODO: 2. Send р or л, whichever solves it.
        // TODO: 3. board_solved.shift_keys is hjkl.
    }

    @Test
    fun `Cyrillic keys step through the solution screen`() {
        // TODO: 1. Open a solution-screen save.
        // TODO: 2. Send д: Next.
        // TODO: 3. Send ф: Prev.
        // TODO: 4. Send д then к: Restart, back at the original arrangement.
        // TODO: 5. Send ы: the step list scrolls down, on a screen short enough to scroll.
    }

    @Test
    fun `other non-Latin letters do nothing`() {
        // TODO: 1. Send ω, and then Hebrew ש.
        // TODO: 2. The board, selection and move count don't change, and no junk appears.
    }

    @Test
    fun `a Cyrillic save name is saved, listed and loaded`() {
        // TODO: 1. Press 2 and send ц, й.
        // TODO: 2. The field shows цй.
        // TODO: 3. Press Enter: the status line says it's saved.
        // TODO: 4. Shift once, press 3: the list shows цй (4x4).
        // TODO: 5. Pick it: the board returns to the saved arrangement.
    }

    @Test
    fun `Backspace removes a whole Cyrillic letter`() {
        // TODO: 1. Press 2 and send ц, й, then Backspace.
        // TODO: 2. The field shows ц.
    }

    @Test
    fun `other non-Latin letters don't enter the Save name`() {
        // TODO: 1. Press 2 and send ц, ω, ü.
        // TODO: 2. The field shows ц.
    }

    @Test
    fun `the startup restore lists a Cyrillic save`() {
        // TODO: 1. Seed a save called ц and launch with no --size.
        // TODO: 2. Restore a saved game? lists ц (4x4).
        // TODO: 3. Confirm it: the save loads.
    }

    @Test
    fun `the Controls key list ends with the layouts line, in both modes`() {
        // TODO: 1. Press 6 in keyboard mode: the last key-list line is `Letters: Latin, Ukrainian or Russian layout`.
        // TODO: 2. Mark Left-handed: it's still the last line.
        // TODO: 3. Repeat in mouse mode.
    }
}
