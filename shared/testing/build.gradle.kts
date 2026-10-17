plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

// GH-41: test doubles shared by the shared modules' tests and the terminal app's tests.
dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:analytics"))
    implementation(project(":shared:storage"))
    implementation(project(":shared:mordie"))
    implementation(project(":shared:viewmodel"))
}
