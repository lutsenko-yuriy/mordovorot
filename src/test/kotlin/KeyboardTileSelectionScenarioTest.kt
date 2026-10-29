import kotlin.test.Test

/** GH-170: keyboard mode on the shared keyboard scheme (ADR-0003) - a selected tile moved by
 *  WASD and shifted by HJKL/arrows, the digit toolbar, no F-keys, H/L and K/J on the solution
 *  screen. Replaces [KeyboardLineSelectionScenarioTest]. Stubs until WU1 makes them green. */
class KeyboardTileSelectionScenarioTest {

    @Test
    fun `keyboard mode marks the selected tile, its row and its column and shows the digit toolbar`() {
        // TODO: start keyboard mode on a scrambled 4x4 board
        // TODO: the tile at row 1, column 1 is reverse video; the rest of row 1 is on pale yellow and the rest of column 1 on light grey, black text on both
        // TODO: no ◀▶▲▼ arrows are drawn
        // TODO: the side hints read `W/A/S/D select`, `←/→ H/L row` and `↑/↓ K/J column`
        // TODO: the toolbar shows `[Give Up 5]`, `[New 1] [Save 2] [Load 3] [Exit Esc]` and `[Labels 4] [Controls 6]`
        // TODO: the controls-hint line reads `1 New · 2 Save · 3 Load · 4 Labels · 5 Give Up · 6 Controls · Esc Exit`
    }

    @Test
    fun `W A S D move the selected tile with wraparound`() {
        // TODO: press s, d - the tile at row 2, column 2 is selected, its row and column coloured
        // TODO: fresh session: press w, a - the tile at row 4, column 4 is selected (both wrap)
    }

    @Test
    fun `H L and the left and right arrows shift the selected row and the selection stays put`() {
        // TODO: press s, l - row 2 is shifted right
        // TODO: the selected tile is still at row 2, column 1, now showing a different tile
        // TODO: press ←, then h - row 2 is two shifts left of the start, one move counted, `Moves:` shown
    }

    @Test
    fun `K J and the up and down arrows shift the selected column`() {
        // TODO: press d, j - column 2 is shifted down
        // TODO: press ↑, then k - column 2 is shifted up
    }

    @Test
    fun `upper-case letters work too`() {
        // TODO: S D L leave the board and selection as s d l do
    }

    @Test
    fun `digits open the toolbar's dialogs in keyboard mode`() {
        // TODO: 1 opens the size picker; Escape
        // TODO: 2 opens Save; Escape
        // TODO: 3 opens Load; Escape
        // TODO: 4 opens Tile labels; Escape
        // TODO: Escape opens Exit
        // TODO: the size, save, load, labels (opened_from: shortcut) and exit screen views fired
    }

    @Test
    fun `function keys do nothing`() {
        // TODO: send the F4-F9 sequences
        // TODO: no dialog opens, the board is unchanged, no stray characters in the frame
    }

    @Test
    fun `digits typed in the Save dialog go into the name field`() {
        // TODO: press 2, type a12
        // TODO: the field reads `a12` and no other dialog opened
    }

    @Test
    fun `a smaller new game clamps the selection`() {
        // TODO: select row 4, column 4 (w, a)
        // TODO: 1, pick 3x3 - the tile at row 3, column 3 is selected
    }

    @Test
    fun `a solved board hides the selection and ignores the selection and shift keys`() {
        // TODO: start one shift away from solved, make the solving shift
        // TODO: no reverse-video or coloured tiles remain
        // TODO: w a s d h l j k leave the board and frame unchanged
        // TODO: the hints stay
    }

    @Test
    fun `H L step and K J scroll the solution screen`() {
        // TODO: give up with 5 and confirm
        // TODO: l applies step 1 (Next), h undoes it (Prev)
        // TODO: j / k scroll the step list as ↓ / ↑ do
        // TODO: the side hints list H/L and K/J
    }
}
