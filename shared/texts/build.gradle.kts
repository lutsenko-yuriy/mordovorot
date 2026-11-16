plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }
    js { nodejs { testTask { useMocha { timeout = "30s" } } } }

    // AvoidWordsTest reads the glossary, so a change to it must rerun the test.
    jvm().testRuns["test"].executionTask.configure { inputs.file(rootProject.file("docs/GLOSSARY.md")) }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
