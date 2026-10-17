plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }
    // Compile-only (GH-41): no JS environment, so no Node download and no JS tests - it exists so
    // a JVM-only API in commonMain fails the build.
    js(IR)

    sourceSets {
        // api: GameViewModel's public surface exposes BoardModel, AnalyticsService,
        // SaveRepository, Mordie and coroutines' ReceiveChannel/CompletableDeferred.
        val commonMain by getting {
            dependencies {
                api(project(":shared:core"))
                api(project(":shared:analytics"))
                api(project(":shared:storage"))
                api(project(":shared:mordie"))
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":shared:testing"))
            }
        }
    }
}
