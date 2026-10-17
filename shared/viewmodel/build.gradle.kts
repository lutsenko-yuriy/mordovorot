plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }
    // GH-103: a real JS target, running the common tests on Node.js.
    js { nodejs() }

    sourceSets {
        // api: GameViewModel's public surface exposes BoardModel, AnalyticsService,
        // SaveRepository, Mordie and coroutines' ReceiveChannel/CompletableDeferred.
        commonMain {
            dependencies {
                api(project(":shared:core"))
                api(project(":shared:analytics"))
                api(project(":shared:storage"))
                api(project(":shared:mordie"))
                api(libs.kotlinx.coroutines.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":shared:testing"))
            }
        }
    }
}
