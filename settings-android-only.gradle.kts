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
// :domain hosts:
//   • AppSettings + AppSettingsRepository + Migrator (settings agent Wave 2)
//   • AnalyticsService contract (PostHog agent Wave 2)
//   • SensorService + StylusInputManager actuals (sensor + stylus agents Wave 2)
// All of androidApp's new ViewModels + analytics depend on :domain.
include(":shared", ":domain", ":androidApp")
