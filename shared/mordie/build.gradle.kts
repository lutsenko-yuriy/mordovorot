plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }
    // GH-103: a real JS target, running the common tests on Node.js. Mocha's 2 s default per
    // test is too short for the solver tests on a CI runner.
    js { nodejs { testTask { useMocha { timeout = "30s" } } } }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":shared:core"))
                implementation(project(":shared:testing"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
