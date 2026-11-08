plugins {
    // GH-41: the shared/ modules' plugin, declared here so they share this one version.
    alias(libs.plugins.kotlin.multiplatform) apply false
}

// GH-228: the root project only aggregates shared/ and web; it holds no code.
version = "0.40.0"

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
