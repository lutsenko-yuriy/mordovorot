plugins {
    kotlin("jvm") version "1.9.24"
    application
}

version = "0.7.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // GH-42: the project's first third-party runtime dependency - powers Presenter's
    // View-request channel. 1.8.1 is the last kotlinx.coroutines line built against
    // Kotlin 1.9.x (1.9.0 moved to Kotlin 2.0); confirmed empirically by this build.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation(kotlin("test"))
    // Test-only, added ahead of WU2 (round-2 audit finding on PR #43: unused by WU1 itself -
    // this WU has no request-handler coroutine yet for a test to launch/cancel/deadlock on).
    // WU2's `runTest` will give a timeout-failure instead of a silent hang on a deadlocked
    // coroutine test - this refactor's actual failure mode there. No real delay() anywhere in
    // this codebase, so its virtual-time skipping isn't the point of adding it.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
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
