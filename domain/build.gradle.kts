plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // Suppress the "expect/actual classes in Beta" warning project-wide.
    // The classes are intentional KMP boundaries; see commonMain expect declarations.
    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += listOf("-Xexpect-actual-classes")
            }
        }
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        publishLibraryVariants("release")
    }

    // JVM target for fast unit tests without booting Android instrumentation.
    // Lets `:domain:jvmTest` run AppSettingsTest on plain JVM (CI + dev loop).
    // NOTE: disabled until Kotlin 2.0.21 JVM-IR + kotlinx.serialization regression is
    // worked around (compileKotlinJvm hits IrFakeOverrideSymbolBase.shouldNotBeCalled).
    // Android unit tests cover the same surface for now — see build-report.md.
    // jvm()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Domain"
            isStatic = true
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            // NOTE: Plan calls for `api(project(":shared"))` so consumers can see Difficulty,
            // SudokuGrid, etc. transitively through :domain. We hold that until Wave 2 because
            // :shared currently does not declare a `jvm()` target, and adding it here would
            // block the JVM unit-test loop on :domain. None of the Wave-1 files in :domain
            // (AppSettings, StylusInputManager expect, SensorService expect, PlatformHaptics
            // expect) actually need :shared types yet — they only become necessary when
            // ViewModels move into :domain (Wave 2+). When that happens, either:
            //   (a) add `jvm()` target to :shared as well, or
            //   (b) restrict the :shared dep to non-jvm targets via `androidMain` / `iosMain`.
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            // :shared is hosted here (it has an Android target) so Android consumers
            // of :domain still get :shared types transitively when they need them.
            api(project(":shared"))
        }
        iosMain.dependencies {
            api(project(":shared"))
        }
    }
}

android {
    namespace = "com.nextjedi.sudokustreak.domain"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
