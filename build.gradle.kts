plugins {
    kotlin("jvm") version "1.9.24"
    application
}

version = "0.7.6"

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
    // Test-only, still unused after WU2 (round-2 audit finding on PR #43 first flagged this for
    // WU1; WU2 kept every test on plain `runBlocking`, matching the rest of the suite, rather
    // than mixing `runTest` in ahead of a project-wide decision). `runTest` would give a
    // timeout-failure instead of a silent hang on a deadlocked coroutine test - the actual
    // failure mode WU2's request-channel tests risk - so it's kept for WU3's checklist to
    // settle explicitly: adopt it (with `backgroundScope` for any test that launches a handler
    // coroutine, per the Research-WU's finding), or drop the dependency and rely on Gradle's
    // test timeout instead.
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
