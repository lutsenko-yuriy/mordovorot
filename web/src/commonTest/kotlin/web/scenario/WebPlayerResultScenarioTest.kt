package web.scenario

import kotlin.test.Test

/** GH-273 scenarios: Mordie's line on the solution screen opens with how the player's game ended.
 *  Same setup as [WebGiveUpScenarioTest]: a 4x4 board two moves from solved. */
class WebPlayerResultScenarioTest {

    @Test
    fun `giving up after 3 moves puts the result in front of Mordie's line`() = webTest {
        // TODO: 1. Start on the board; Mordie finds its solution
        // TODO: 2. Make 3 moves
        // TODO: 3. Give Up, answer Yes
        // TODO: 4. Mordie's line reads "You gave up after 3 moves. Here's how I did it in 2 moves!"
        // TODO: 5. Moves: N stays hidden
        // TODO: 6. The screen_solution event is sent with player_result gave_up
    }

    @Test
    fun `Mordie's solution after a win puts the solved result in front of Mordie's line`() = webTest {
        // TODO: 1. Start on the board; Mordie finds its solution
        // TODO: 2. Solve the board in 2 moves
        // TODO: 3. Click Mordie's solution
        // TODO: 4. Mordie's line reads "You solved it in 2 moves. Here's how I did it in 2 moves!"
        // TODO: 5. The screen_solution event is sent with player_result solved
    }

    @Test
    fun `a count of 1 reads 1 move`() = webTest {
        // TODO: 1. Give up after exactly 1 move
        // TODO: 2. Mordie's line starts "You gave up after 1 move. "
    }

    @Test
    fun `giving up before any move says the player did not even start`() = webTest {
        // TODO: 1. Give up straight away, with 0 moves made
        // TODO: 2. Mordie's line reads "You did not even start this one. Here's how I did it in 2 moves!"
        // TODO: 3. The screen_solution event is sent with player_result gave_up
    }

    @Test
    fun `stepping the replay never counts moves, feeds Mordie or changes the result`() = webTest {
        // TODO: 1. Give up after 3 moves
        // TODO: 2. Press Next twice, then Restart
        // TODO: 3. The move count is still not shown
        // TODO: 4. Mordie's line is unchanged
        // TODO: 5. No board_solved event is sent
        // TODO: 6. The game has no unsaved progress
    }

    @Test
    fun `a solution-screen save keeps the result, and Load shows the same line`() = webTest {
        // TODO: 1. Give up after 3 moves, then Save as "s"
        // TODO: 2. Choose New, then Load "s"
        // TODO: 3. Mordie's line reads "You gave up after 3 moves. Here's how I did it in 2 moves!"
        // TODO: 4. The screen_solution event is sent with opened_from load and player_result gave_up
    }

    @Test
    fun `a solved result survives a reload and the startup restore shows the same line`() = webTest {
        // TODO: 1. Solve the board, open Mordie's solution, then Save as "s"
        // TODO: 2. Start a fresh page on the same store
        // TODO: 3. Choose "s" in the startup prompt
        // TODO: 4. Mordie's line reads "You solved it in 2 moves. Here's how I did it in 2 moves!"
        // TODO: 5. The screen_solution event is sent with opened_from startup_prompt and player_result solved
    }

    @Test
    fun `a solution-screen save from before this ticket keeps today's line`() = webTest {
        // TODO: 1. Seed a packed solution-screen save made before this ticket (a fixed string)
        // TODO: 2. Load it
        // TODO: 3. Mordie's line reads "Here's how I did it in 2 moves!"
        // TODO: 4. The screen_solution event is sent with player_result unknown
    }

    @Test
    fun `leaving the solution screen forgets the result`() = webTest {
        // TODO: 1. Give up after 3 moves
        // TODO: 2. Choose New, then pick a size
        // TODO: 3. Play a normal game: Mordie's lines carry no result text
        // TODO: 4. Moves: N counts as before
    }
}
