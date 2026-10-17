rootProject.name = "mordovorot"

// GH-41: the platform-independent game, as Kotlin Multiplatform modules a future Web/Android/iOS
// client can build on. The terminal app stays the root project.
include(
    ":shared:core", ":shared:analytics", ":shared:storage", ":shared:mordie", ":shared:viewmodel", ":shared:testing",
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
