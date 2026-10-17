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
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
