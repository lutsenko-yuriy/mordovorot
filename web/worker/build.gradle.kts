import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

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

// Copies the bundle into :web's resources (see workerResources there).
tasks.register<Sync>("shipToWeb") {
    from(tasks.named<KotlinWebpack>("jsBrowserProductionWebpack").flatMap { it.outputDirectory }) { include("mordie-worker.js") }
    into(project(":web").layout.buildDirectory.dir("generated/worker/resources"))
}
