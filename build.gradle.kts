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
}
