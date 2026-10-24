plugins {
    alias(libs.plugins.kotlin.jvm)
    // GH-41: the shared/ modules' plugin, declared here so they share this one version.
    alias(libs.plugins.kotlin.multiplatform) apply false
    application
}

version = "0.19.2"

kotlin {
    jvmToolchain(17)
}

dependencies {
    // GH-42: the project's first third-party runtime dependency - powers ViewModel's
    // View-request channel. Versions live in gradle/libs.versions.toml (GH-109).
    implementation(libs.kotlinx.coroutines.core)
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
