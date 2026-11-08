import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.security.MessageDigest

plugins {
    kotlin("multiplatform")
}

// GH-262: the offline worker, a service worker that keeps the whole game in a cache. It depends on
// nothing but the standard library, so the file holds none of the game and web.js's main() can't
// start inside it. Its file name is fixed for good: an installed worker only re-checks its own URL.
kotlin {
    jvmToolchain(17)
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }
    js {
        browser {
            commonWebpackConfig { outputFileName = "offline-worker.js" }
            testTask { enabled = false }
        }
        nodejs { testTask { useMocha { timeout = "30s" } } }
        binaries.executable()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
