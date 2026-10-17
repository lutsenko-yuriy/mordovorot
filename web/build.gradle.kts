plugins {
    kotlin("multiplatform")
}

// GH-110: the browser client. DOM-free code goes in commonMain, so its tests run on the JVM and
// Node.js; the page itself is in jsMain.
kotlin {
    jvmToolchain(17)
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }
    js {
        browser {
            // Saves live in localStorage, which is per origin, so a moving port would hide them.
            commonWebpackConfig { devServer = devServer?.copy(port = 8080) }
            // No headless browser in CI: the common tests run on Node.js instead.
            testTask { enabled = false }
        }
        nodejs { testTask { useMocha { timeout = "30s" } } }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":shared:viewmodel"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.register("serve") {
    group = "application"
    description = "Serves the browser game on http://localhost:8080."
    dependsOn("jsBrowserDevelopmentRun")
}
