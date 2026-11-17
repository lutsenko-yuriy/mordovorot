package web.scenario

import kotlin.test.Test

/**
 * GH-332: the alphabet hint, `[ Hint 8 ]` between Give Up and Tutorial (Tutorial moves to 9). WU0's stubs:
 * `// TODO:` steps only, no driver calls or assertions yet; WU2 fills them in, red first.
 */
class WebAlphabetHintScenarioTest {
    @Test
    fun hintButtonIsLiveForEachLetterScheme() {
        // TODO: 1. Start a new 4x4 game
        // TODO: 2. For each of Letters, Letter pairs, Cyrillic letters, Cyrillic letter pairs, Greek letters and Greek letter pairs: mark the scheme
        // TODO: 3. The Hint button is live
    }

    @Test
    fun hintButtonIsDimmedForNumberSchemes() {
        // TODO: 1. Start a new 4x4 game
        // TODO: 2. For each of Numbers, Numbers from 0, Pairs and Pairs from 0: mark the scheme
        // TODO: 3. The Hint button is dimmed
    }

    @Test
    fun hintButtonIsDimmedWhenLettersStandInAsNumbers() {
        // TODO: 1. Mark Letters
        // TODO: 2. Start a new 6x6 game; the board shows Numbers
        // TODO: 3. The Hint button is dimmed
    }

    @Test
    fun hintButtonIsHiddenOnTheSolutionAndDuringTheTutorial() {
        // TODO: 1. Give up and open Mordie's solution; the Hint button is hidden
        // TODO: 2. Start a new game and begin the tutorial replay; the Hint button is hidden
    }

    @Test
    fun hintButtonSurvivesAWinAndTheTutorialSwitch() {
        // TODO: 1. Mark Letters and solve a 3x3 game; the Hint button is live
        // TODO: 2. Turn the tutorial switch off; the Hint button is still live
    }

    @Test
    fun hintHasDigit8OnAComputerAndNoneOnTouch() {
        // TODO: 1. On a computer the Hint button's digit is 8
        // TODO: 2. On touch it has no digit
    }

    @Test
    fun hintOpensWithTheLatinAlphabet() {
        // TODO: 1. Mark Letters on a 4x4 game
        // TODO: 2. Click Hint
        // TODO: 3. The dialog is titled `Latin alphabet`
        // TODO: 4. It lists A to Z, with 16 letters in use and 10 greyed
    }

    @Test
    fun hintOpensWithTheFullGreekAlphabetOn5x5() {
        // TODO: 1. Mark Greek letters on a 5x5 game
        // TODO: 2. Click Hint
        // TODO: 3. All 25 letters are in use, including U+037E (compared by code point)
    }

    @Test
    fun greekPairsKeepTheQuestionMarkGrey() {
        // TODO: 1. Mark Greek letter pairs on a 5x5 game
        // TODO: 2. Click Hint
        // TODO: 3. 5 letters are in use and U+037E is greyed
    }

    @Test
    fun hintDialogIsTitledInTheGameLanguage() {
        // TODO: 1. Switch to German and open Hint on Cyrillic letters: `Kyrillisches Alphabet`
        // TODO: 2. Switch to Russian and open it on Greek letters: `Греческий алфавит`
    }

    @Test
    fun okAndEscapeCloseTheHintWithoutChangingTheGame() {
        // TODO: 1. Make two moves, open Hint, press OK
        // TODO: 2. The board, moves and labels are unchanged
        // TODO: 3. Open Hint again and press Escape; the same holds
    }

    @Test
    fun dimmedHintOpensNothing() {
        // TODO: 1. Mark Numbers
        // TODO: 2. Click Hint; no dialog opens
        // TODO: 3. Press 8; no dialog opens and no screen view is sent
    }

    @Test
    fun key8OpensTheHintAndKey9ReplaysTheTutorial() {
        // TODO: 1. Press 8 on a Letters board; the hint opens
        // TODO: 2. Close it, then press 9; the tutorial replay starts
    }

    @Test
    fun keys8And9DoNothingDuringTheTutorial() {
        // TODO: 1. Start the tutorial
        // TODO: 2. Press 8; nothing opens
        // TODO: 3. Press 9; nothing changes
    }

    @Test
    fun focusReturnsToHintAfterAClick() {
        // TODO: 1. Click Hint, then OK; focus is on the Hint button
        // TODO: 2. Press 8 there; the dialog opens again
    }

    @Test
    fun focusReturnsToTheBoardAfterKey8() {
        // TODO: 1. Press 8 on the board, then Escape; focus is on the board
        // TODO: 2. A shift key still shifts
    }

    @Test
    fun openingTheHintSendsOneScreenView() {
        // TODO: 1. Click Hint; `screen_alphabet_hint_dialog` is sent once with `opened_from: button`
        // TODO: 2. Close it, press 8; `screen_alphabet_hint_dialog` is sent with `opened_from: shortcut`
    }

    @Test
    fun closingTheHintSendsDialogCancelled() {
        // TODO: 1. Press Escape; `dialog_cancelled` is sent with `dialog: alphabet_hint` and `via: escape`
        // TODO: 2. Open again and press OK; `dialog_cancelled` is sent with `via: button`
    }
}
