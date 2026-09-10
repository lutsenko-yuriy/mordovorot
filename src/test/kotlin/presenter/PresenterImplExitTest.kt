package presenter

import kotlin.test.Test

class PresenterImplExitTest {

    @Test
    fun `exit, save declined - ends play without saving`() {
        // TODO: 1. FakeView(confirmSaveBeforeExitResponses = [false]) scripted so processCommand triggers exitGame()
        // TODO: 2. presenter.play()
        // TODO: 3. Verify no save occurred (FakeSaveRepository received no save call)
        // TODO: 4. Verify analytics.events == [Event("exit_command_used", {"save_choice": "declined"})]
    }

    @Test
    fun `exit, save confirmed with a name - saves then ends play`() {
        // TODO: 1. FakeView(confirmSaveBeforeExitResponses = [true], promptSaveNameResponses = ["foo"]) scripted so processCommand triggers exitGame()
        // TODO: 2. presenter.play()
        // TODO: 3. Verify the save repository received a save for "foo"
        // TODO: 4. Verify analytics.events == [Event("save_command_used", {...}), Event("exit_command_used", {"save_choice": "saved"})]
    }

    @Test
    fun `exit, save confirmed but a blank or EOF name - ends play without saving`() {
        // TODO: 1. FakeView(confirmSaveBeforeExitResponses = [true], promptSaveNameResponses = [null]) scripted so processCommand triggers exitGame()
        // TODO: 2. presenter.play()
        // TODO: 3. Verify no save occurred
        // TODO: 4. Verify analytics.events == [Event("exit_command_used", {"save_choice": "declined"})]
    }
}
