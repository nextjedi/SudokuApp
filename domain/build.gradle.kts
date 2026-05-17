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

            // ML Kit Digital Ink Recognition (Wave 2 — Android StylusInputManager actual).
            // Bundled `en` text base model loaded at first recognize() call; no runtime
            // download required because the model artifact ships inside the AAR.
            // Filter logic restricts candidates to "1".."9" per StylusInputManager.android.kt.
            // Stylus reviewer PC-7: no WiFi gate; ~20MB APK delta documented in build report.
            implementation(libs.mlkit.digital.ink)
            implementation(libs.mlkit.common)

            // Bridges com.google.android.gms.tasks.Task → Kotlin suspend (`Task.await()`).
            // Used by MlKitDigitRecognizer to await the ML Kit recognize() Task without
            // blocking the Default dispatcher.
            implementation(libs.kotlinx.coroutines.play.services)

            // Lifecycle-aware sensor binding (LifecycleEventObserver / DefaultLifecycleObserver).
            // Wave 2: SensorService binds to a LifecycleOwner so listeners are registered only
            // on RESUMED and unregistered on PAUSED. See REVAMP_PLAN.md §10.
            implementation(libs.androidx.lifecycle.runtime.ktx)
        }

        // Android unit tests (Robolectric + MockK + Turbine + Truth + JUnit4) for
        // StylusInputManager.android.kt + MlKitDigitRecognizer.kt + SensorService.android.kt.
        // The custom source-set name is `androidUnitTest` per KMP's androidTarget DSL.
        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.lifecycle.runtime.testing)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockk)
                implementation(libs.turbine)
                implementation(libs.truth)
            }
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
    // Robolectric in :domain:androidUnitTest needs Android resources packaged into the
    // test classpath (for SensorService SensorEventBuilder etc.) and default-return
    // semantics for un-stubbed Android framework calls (matches androidApp/build.gradle.kts).
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}
