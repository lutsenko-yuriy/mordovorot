plugins {
    kotlin("jvm") version "1.9.24"
    application
}

version = "0.9.2"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // GH-42: the project's first third-party runtime dependency - powers ViewModel's
    // View-request channel. 1.8.1 is the last kotlinx.coroutines line built against
    // Kotlin 1.9.x (1.9.0 moved to Kotlin 2.0); confirmed empirically by this build.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    // The application plugin doesn't wire stdin by default; ViewImpl reads
    // console commands from a BufferedReader over System.in, so without this
    // the game hits EOF on its first read.
    standardInput = System.`in`
    // Gradle pipes this process's stdout through its own logger, so System.console() is
    // always null here - the mouse TUI (GH-3) is unreachable via `./gradlew run`. Use
    // `./gradlew installDist && ./build/install/mordovorot/bin/mordovorot` to launch it.
}
