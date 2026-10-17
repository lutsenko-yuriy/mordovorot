plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)
    jvm()
    // GH-103: JS too, so the shared modules' common tests can use these doubles on Node.js.
    js { nodejs() }

    // GH-41: test doubles shared by the shared modules' tests and the terminal app's tests.
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":shared:core"))
                implementation(project(":shared:analytics"))
                implementation(project(":shared:storage"))
                implementation(project(":shared:mordie"))
                implementation(project(":shared:viewmodel"))
            }
        }
    }
}
