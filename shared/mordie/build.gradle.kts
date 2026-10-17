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
    js()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":shared:core"))
                implementation(project(":shared:testing"))
            }
        }
    }
}
