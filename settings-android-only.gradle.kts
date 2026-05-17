// Temporary settings file used only by the android-theme agent to validate the
// androidApp/ module without depending on composeApp/ (which has a pre-existing
// config-time DSL issue another agent is fixing in parallel).
//
// Usage:
//   ./gradlew.bat -c settings-android-only.gradle.kts :androidApp:assembleDebug
//
// This file is NOT committed permanently — see build-report.md.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SudokuKMP"
// :domain hosts AppSettings + AppSettingsRepository + Migrator (Wave 1 contracts the
// settings agent wires up in Wave 2). Without :domain the new SettingsViewModel +
// AppSettingsDataStoreRepository cannot resolve their imports.
include(":shared", ":domain", ":androidApp")
