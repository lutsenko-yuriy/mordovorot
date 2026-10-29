plugins {
    alias(libs.plugins.kotlin.jvm)
    // GH-41: the shared/ modules' plugin, declared here so they share this one version.
    alias(libs.plugins.kotlin.multiplatform) apply false
    application
}

version = "0.26.2"

kotlin {
    jvmToolchain(17)
}

// GH-137: the Kotlin/JS test runner's mocha 11 still locks serialize-javascript 6 and diff 7,
// both with open advisories. Pin the patched majors until a Kotlin release bundles a mocha that
// needs them; mocha 11 calls only diff.createPatch and, in parallel mode, serialize().
// rootPackageJson doesn't see a change here: run `./gradlew rootPackageJson --rerun kotlinUpgradeYarnLock`.
// GH-152: a fix within a package's locked range needs no pin: yarn keeps a lock entry that still
// matches, so delete that package's entry in kotlin-js-store/yarn.lock, then run kotlinUpgradeYarnLock.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("serialize-javascript", "^7.1.4")
        resolution("diff", "^8.0.4")
    }
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
