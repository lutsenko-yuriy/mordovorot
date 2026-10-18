rootProject.name = "mordovorot"

// GH-41: the platform-independent game, as Kotlin Multiplatform modules a future Web/Android/iOS
// client can build on. The terminal app stays the root project.
include(
    ":shared:core", ":shared:analytics", ":shared:storage", ":shared:mordie", ":shared:viewmodel", ":shared:testing",
    // GH-110: the browser client.
    ":web",
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// GH-124: serve only rebuilds on change under continuous build, and keeps its outputs in
// build/serve so check/build can run alongside it.
if (gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "serve" }) {
    if (!gradle.startParameter.isContinuous) {
        throw GradleException("Run it as ./gradlew :web:serve -t (continuous build, so it rebuilds on change)")
    }
    gradle.lifecycle.beforeProject { layout.buildDirectory.set(layout.projectDirectory.dir("build/serve")) }
}
