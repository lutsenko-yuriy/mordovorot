package web.scenario

import kotlin.test.Test

/** GH-166 scenarios: what a screen reader hears from the status line, New, Load, startup and the
 *  solution screen. */
class WebAnnounceFlowsScenarioTest {

    @Test
    fun `the same status message twice is announced twice`() = webTest {
        // TODO: Save as `a` - says `Saved as 'a'.`
        // TODO: Save as `a` again, confirm the overwrite - says `Saved as 'a'.` again, a second announcement
    }

    @Test
    fun `a rejected save name is announced`() = webTest {
        // TODO: Save as `a b` - the status line's rejection message is said
    }

    @Test
    fun `toolbar New announces the new game, and Cancel announces nothing`() = webTest {
        // TODO: New, then 5x5 - says `New 5x5 game`
        // TODO: New, then Cancel - nothing new said
    }

    @Test
    fun `startup with no saves announces the new game`() = webTest {
        // TODO: start with no saves, pick 4x4 - says `New 4x4 game`
        // TODO: on a fresh page, cancel the startup size picker instead - also says `New 4x4 game`
    }

    @Test
    fun `a load announces the save's name, size and moves`() = webTest {
        // TODO: seed `foo`, 5x5, 12 moves
        // TODO: Load `foo` - says `Loaded 'foo', 5x5, 12 moves` and nothing else (not the plain `Loaded 'foo'.`)
    }

    @Test
    fun `loading a solved save announces it solved`() = webTest {
        // TODO: seed a solved 4x4 `done` with 14 moves
        // TODO: Load it - says `Loaded 'done', 4x4, solved in 14 moves`
    }

    @Test
    fun `the startup restore announces the load`() = webTest {
        // TODO: seed `foo`
        // TODO: start, yes to the restore offer - says `Loaded 'foo', 4x4, 0 moves`
    }

    @Test
    fun `a load error is announced`() = webTest {
        // TODO: put a corrupt save in the store and load it - the status line's error is said
    }

    @Test
    fun `Can't be solved - New game announces the new game, Repair announces its message`() = webTest {
        // TODO: seed an unsolvable 3x3 and load it
        // TODO: New game, then 3x3 - says `New 3x3 game`
        // TODO: load it again, Repair & load - the repair status message is said
    }

    @Test
    fun `Give Up's solution screen announces itself and each step`() = webTest {
        // TODO: Give Up, Yes - says `Solution, N moves`
        // TODO: Next - says `Step 1 of N: ...`, the step in words (e.g. `Row 1 left 1`)
        // TODO: on a two-shift step, Next again re-reads the same step line
        // TODO: Prev back to the start - says `Start, step 0 of N`
        // TODO: Next, then Restart - says `Start, step 0 of N`
    }

    @Test
    fun `Mordie's solution after a win announces the solution screen`() = webTest {
        // TODO: let Mordie find his solution, then solve the board
        // TODO: press `Mordie's solution` - says `Solution, N moves`
    }
}
