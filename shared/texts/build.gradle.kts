plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }
    js { nodejs { testTask { useMocha { timeout = "30s" } } } }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
