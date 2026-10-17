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
