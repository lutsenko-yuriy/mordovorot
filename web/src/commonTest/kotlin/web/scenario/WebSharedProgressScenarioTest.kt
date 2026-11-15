package web.scenario

import kotlin.test.Test

/** GH-272 scenarios: opening a share link that carries the sender's progress, through [WebHarness]. Stubs until WU1. */
class WebSharedProgressScenarioTest {

    @Test
    fun `a link that says the sender is still solving puts that status in Mordie's opening`() = webTest {
        // TODO: 1. Open a link with `p=n12`
        // TODO: 2. Moves is 0 and the board is fresh
        // TODO: 3. Mordie says `Okay, so your friend made 12 moves and did not solve the puzzle yet. Maybe we could help?`
    }

    @Test
    fun `a link that says the sender solved it puts that status in Mordie's opening`() = webTest {
        // TODO: 1. Open a link with `p=s12`
        // TODO: 2. Mordie says `Okay, so your friend solved this one in 12 moves. Wanna beat this?`
    }

    @Test
    fun `a link that says the sender gave up puts that status in Mordie's opening`() = webTest {
        // TODO: 1. Open a link with `p=g12`
        // TODO: 2. Mordie says `So after making 12 moves your friend gave up before finding the solution. Can we find one?`
    }

    @Test
    fun `a gave-up link with no count drops the count from Mordie's line`() = webTest {
        // TODO: 1. Open a link with `p=g`
        // TODO: 2. Mordie says `So your friend gave up before finding the solution. Can we find one?`
    }

    @Test
    fun `a count of 1 reads 1 move`() = webTest {
        // TODO: 1. Open a link with `p=n1`
        // TODO: 2. Mordie says `Okay, so your friend made 1 move and did not solve the puzzle yet. Maybe we could help?`
    }

    @Test
    fun `the sender's status stays through shift 4`() = webTest {
        // TODO: 1. Open a link with `p=n12`
        // TODO: 2. Make shifts 1 to 4, checking Mordie's line after each
        // TODO: 3. The friend line is still shown
    }

    @Test
    fun `from shift 5 the normal Mordie rules apply`() = webTest {
        // TODO: 1. Open a link with `p=n12`
        // TODO: 2. Make 5 shifts
        // TODO: 3. Mordie's line is no longer the friend line
    }

    @Test
    fun `a link without progress opens with Mordie's usual line`() = webTest {
        // TODO: 1. Open a link with no `p=`
        // TODO: 2. Mordie says `Alright! Let's sort this one out!`
    }

    @Test
    fun `progress the page can't read is ignored without a word`() = webTest {
        // TODO: 1. Open links with `p=x`, `p=n0`, `p=n012`, `p=n10000000` and `p=s`, one after the other
        // TODO: 2. Each opens the board, Mordie shows the usual line, and no dialog or status message mentions it
    }

    @Test
    fun `a new game after a progress link opens with Mordie's usual line`() = webTest {
        // TODO: 1. Open a link with `p=n12`
        // TODO: 2. Start a new game
        // TODO: 3. Mordie says the usual line
    }

    @Test
    fun `a load at 0 moves after a progress link opens with Mordie's usual line`() = webTest {
        // TODO: 1. Open a link with `p=n12`
        // TODO: 2. Load a save that has 0 moves
        // TODO: 3. Mordie says the usual line
    }

    @Test
    fun `a link with labels and progress shows both`() = webTest {
        // TODO: 1. Open a link with `l=pairs` and `p=g3`
        // TODO: 2. The labels prompt shows
        // TODO: 3. Answer Yes
        // TODO: 4. Mordie shows the gave-up line
    }

    @Test
    fun `a progress link met in an open tab works like one at load`() = webTest {
        // TODO: 1. Start a game
        // TODO: 2. A link with `p=s12` arrives (`hashchange`)
        // TODO: 3. Confirm `Open the shared board?`
        // TODO: 4. Mordie shows the solved line
    }

    @Test
    fun `shared_board_opened tells what the link carried`() = webTest {
        // TODO: 1. Open links with `p=n3`, `p=s3`, `p=g3`, `p=g`, no `p=` and `p=x`
        // TODO: 2. The `shared_board_opened` events carry `progress` as `solving`, `solved`, `gave_up`, `gave_up`, `none` and `unreadable`
    }
}
