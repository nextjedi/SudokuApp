// Test-only settings file. Loads only :shared and :domain so we can compile
// and unit-test :domain without :composeApp's pre-existing build break blocking us.
// Lives at the repo root so subproject paths resolve correctly.
//
// Used by ./gradlew.bat -c settings-domain-only.gradle.kts :domain:allTests
//
// DO NOT use for normal builds — settings.gradle.kts is the canonical one.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS rather than FAIL_ON_PROJECT_REPOS so the wasmJs target's
    // Node.js distribution repo (added by the Kotlin Node.js plugin during configuration)
    // does not crash the test-only build.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
    // Auto-discovered from gradle/libs.versions.toml
}

rootProject.name = "SudokuKMP"
include(":shared", ":domain")
