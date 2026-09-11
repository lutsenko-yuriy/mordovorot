plugins {
    kotlin("jvm") version "1.9.24"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
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
    // Gradle's JavaExec pipes the child process's stdout through Gradle's own logger, so
    // System.console() is always null here even with standardInput wired above (it requires
    // both stdin AND stdout to be a real terminal) - LaunchMode.resolve (GH-3) therefore
    // always falls back to CONSOLE under `./gradlew run`, regardless of `--console`. To
    // launch the mouse-driven TUI, run the built distribution directly instead, e.g.
    // `./gradlew installDist && ./build/install/mordovorot/bin/mordovorot`.
}
