plugins {
    kotlin("jvm") version "1.9.24"
    // GH-41: the shared/ modules' plugin, declared here so they share this one version.
    kotlin("multiplatform") version "1.9.24" apply false
    application
}

version = "0.16.2"

kotlin {
    jvmToolchain(17)
}

dependencies {
    // GH-42: the project's first third-party runtime dependency - powers ViewModel's
    // View-request channel. 1.8.1 is the last kotlinx.coroutines line built against
    // Kotlin 1.9.x (1.9.0 moved to Kotlin 2.0); confirmed empirically by this build.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
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
