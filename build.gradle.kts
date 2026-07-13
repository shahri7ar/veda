// Top-level build file: declares the plugin versions used by the `app` module
// (via `apply false` — the app module applies them for real in app/build.gradle.kts).
// This file must NOT contain an `android { ... }` block — that DSL only exists
// inside a module that has actually applied the Android Gradle Plugin (i.e. app/build.gradle.kts).
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
