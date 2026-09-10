package analytics

import kotlin.test.Test

class NoopAnalyticsServiceTest {

    @Test
    fun `track does nothing and never throws, with or without properties`() {
        val service = NoopAnalyticsService()

        service.track("save_command_used")
        service.track("load_command_used", mapOf("trigger" to "command", "result" to "success"))
    }
}
