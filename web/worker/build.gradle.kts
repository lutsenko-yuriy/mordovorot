plugins {
    kotlin("multiplatform")
}

// GH-218: Mordie's search as a Web Worker script. It depends on the search alone, so the file
// holds none of the game's UI, and web.js's main() can't start inside it.
kotlin {
    jvmToolchain(17)
    js {
        browser {
            commonWebpackConfig { outputFileName = "mordie-worker.js" }
            testTask { enabled = false }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation(project(":shared:mordie"))
            }
        }
    }
}
