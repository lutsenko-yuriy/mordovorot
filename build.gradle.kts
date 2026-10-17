plugins {
    kotlin("jvm") version "2.4.21"
    // GH-41: the shared/ modules' plugin, declared here so they share this one version.
    kotlin("multiplatform") version "2.4.21" apply false
    application
}

version = "0.16.3"

kotlin {
    jvmToolchain(17)
}

dependencies {
    // GH-42: the project's first third-party runtime dependency - powers ViewModel's
    // View-request channel. Keep in step with the shared modules' copies (GH-109).
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation(project(":shared:core"))
    implementation(project(":shared:analytics"))
    implementation(project(":shared:storage"))
    implementation(project(":shared:mordie"))
    implementation(project(":shared:viewmodel"))
    testImplementation(kotlin("test"))
    testImplementation(project(":shared:testing"))
}

application {
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    // Gradle pipes this process's stdout through its own logger, so System.console() is
    // always null here - `./gradlew run` only prints the no-terminal message (GH-79). Use
    // `./gradlew installDist && ./build/install/mordovorot/bin/mordovorot` to play.
}
