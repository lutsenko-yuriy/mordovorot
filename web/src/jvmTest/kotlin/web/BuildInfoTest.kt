package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-111: the version the footer and `app_launched` show is the one `build.gradle.kts` declares. */
class BuildInfoTest {

    @Test
    fun `BuildInfo VERSION is the root build's version`() {
        val declared = Regex("""^version = "(.+)"$""", RegexOption.MULTILINE)
            .find(File("../build.gradle.kts").readText())?.groupValues?.get(1)

        assertEquals(declared, BuildInfo.VERSION)
    }
}
