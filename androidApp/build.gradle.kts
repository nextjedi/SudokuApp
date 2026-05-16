plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nextjedi.sudoku"
    // M3E review §1: compileSdk = 35 is required for some M3 1.4 predictive-back
    // animation specs and works better with RenderEffect / dynamic blur.
    // NOTE: requires Android SDK 35 platform installed locally + on CI.
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nextjedi.sudoku"
        minSdk = 24
        // Bump to 35 to match compileSdk and unlock M3 1.4 predictive-back specs.
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val props = project.properties
            storeFile = props["KEYSTORE_PATH"]?.toString()?.let { file(it) }
            storePassword = props["KEYSTORE_PASSWORD"]?.toString()
            keyAlias = props["KEY_ALIAS"]?.toString()
            keyPassword = props["KEY_PASSWORD"]?.toString()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // AGP 8.4+ requires JDK 17 toolchain.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // M3E review §9: enforce "no Color(0x...) outside ui/theme/" via Lint + a JUnit
    // grep test (see HardcodedColorAuditTest.kt). A future custom Lint module
    // (:lint-checks) will replace the JUnit grep test — see hardcoded-color-violations.md.
    lint {
        abortOnError = false
        warningsAsErrors = false
        baseline = file("lint-baseline.xml")
        disable += setOf("InvalidPackage")
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    // M3E adaptive surface (NavigationSuiteScaffold, currentWindowAdaptiveInfo).
    implementation(libs.material3.adaptive)
    implementation(libs.material3.adaptive.nav.suite)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Theme layer unit tests (Robolectric-backed; BlurEffect is a no-op under
    // Robolectric so glassyTint blur is verified via SDK_INT shadowing only).
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
