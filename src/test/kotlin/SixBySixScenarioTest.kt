import kotlin.test.Test

/** GH-160 scenarios: 6x6 boards in the terminal, mouse and keyboard mode. Real BoardImpl,
 *  FileSaveRepository and FilePreferenceRepository over one temp dir, through a GameSession over a
 *  FakeTerminal, as in TileLabelsScenarioTest. Stubs from draft-scenarios; implement fills them in. */
class SixBySixScenarioTest {

    @Test
    fun `the size picker offers 6x6 and starts a 6x6 game`() {
        // TODO: launch a 4x4 game in mouse mode and click [ New ]
        // TODO: assert the size picker lists 3x3, 4x4, 5x5 and 6x6
        // TODO: click 6x6 - assert the board shows 6 rows of 6 tiles labelled 1-36
        // TODO: assert new_game_size_selected {size: 6, trigger: toolbar}
    }

    @Test
    fun `--size=6 launches straight into 6x6`() {
        // TODO: launch with the size chosen at launch as 6
        // TODO: assert the board is 6x6 and app_launched.board_size is 6
    }

    @Test
    fun `Mordie sits 6x6 out - its line for shifts 0-4, then nothing, and no search`() {
        // TODO: start a 6x6 game
        // TODO: assert the line is `Mordie: 6x6 is too much for me, you're on your own!` and FakeMordieSearchFactory started no search
        // TODO: shift 4 times - assert the line is still the sit-out line
        // TODO: shift a 5th time - assert the line is empty
        // TODO: shift on to 25 - assert the line stays empty, no progress line
        // TODO: assert [ Give Up ] is dimmed throughout
    }

    @Test
    fun `solving a 6x6 - Congratulations, no Mordie result`() {
        // TODO: load a 6x6 board one shift from solved and make that shift
        // TODO: assert the title is `Congratulations ✓` and the Give Up row is empty
        // TODO: assert board_solved has board_size 6 and no mordie_result or mordie_moves
    }

    @Test
    fun `a 6x6 save lists as 6x6 and loads with the sit-out line`() {
        // TODO: start a 6x6 game, shift 3 times, save it as `six`
        // TODO: start a 4x4 game, open Load - assert the list shows `six (6x6)`
        // TODO: load it - assert the board is 6x6 with the saved tiles and moves
        // TODO: assert the line is the sit-out line (not `Okay, so we stopped...`) and no search started
    }

    @Test
    fun `6x6 fits an 80x24 terminal in both modes`() {
        // TODO: start a 6x6 game on an 80x24 terminal in mouse mode
        // TODO: assert board, arrows, Mordie line, Give Up, toolbar, mode row and status line all fit within the 24 rows
        // TODO: switch to keyboard mode - assert the controls hint fits too
    }

    @Test
    fun `on 6x6 Letters is dimmed, and a Letters preference shows Numbers`() {
        // TODO: set the preference to Letters, start a 6x6 game
        // TODO: assert the tiles show Numbers
        // TODO: open [ Labels ] - assert Numbers is marked and the Letters row reads `Letters          up to 5x5`, dimmed
        // TODO: click the Letters row - assert nothing is marked
        // TODO: in keyboard mode, assert Tab and the arrow keys skip Letters
        // TODO: OK on Numbers - assert the preference is still Letters and no tile_labels_changed
        // TODO: start a 4x4 game - assert its tiles show letters
    }

    @Test
    fun `a 6x6 save with Letters warns, and the picked labels win`() {
        // TODO: write a 6x6 save file whose labels line says letters
        // TODO: load it - assert the Tile labels dialog opens with `Letters don't fit a 6x6 board (26 letters, 36 tiles) - pick other labels.`, Letters dimmed, Numbers marked
        // TODO: assert screen_labels_dialog {opened_from: letters_save}
        // TODO: pick Pairs and OK - assert the board shows pairs, the preference is Pairs, tile_labels_changed {from_source: letters_unavailable}
        // TODO: assert the status line then shows `Loaded '<name>'.`
    }

    @Test
    fun `cancelling the Letters warning keeps Numbers, and the next save drops Letters`() {
        // TODO: load the same Letters save and press Cancel
        // TODO: assert the tiles show Numbers, dialog_cancelled {dialog: labels}, the preference unchanged
        // TODO: save it again - assert the save file's labels line now says numbers
    }
}
