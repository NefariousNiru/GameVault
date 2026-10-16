plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Signing credentials are supplied only from local environment variables or
// GitHub Actions secrets. They must never be committed to this repository.
val signingStoreFile = providers.environmentVariable("GV_RELEASE_STORE_FILE").orNull
val signingStorePassword = providers.environmentVariable("GV_RELEASE_STORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("GV_RELEASE_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("GV_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    signingStoreFile, signingStorePassword, signingKeyAlias, signingKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.gamevault.app"
    // Android API 37 SDK platforms use minor-versioned names (e.g., android-37.0).
    // The minor must be explicit, or AGP may look for nonexistent android-37.
    compileSdk { version = release(37) { minorApiLevel = 0 } }

    defaultConfig {
        applicationId = "com.gamevault.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("gamevaultRelease") {
                storeFile = file(requireNotNull(signingStoreFile))
                storePassword = requireNotNull(signingStorePassword)
                keyAlias = requireNotNull(signingKeyAlias)
                keyPassword = requireNotNull(signingKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("gamevaultRelease")
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    // Gradle can run on JDK 25; Android device bytecode is a separate target.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

// Never emit an accidentally unsigned release. A Gradle Exec task can be
// serialized by the configuration cache; a doLast closure referencing this
// Gradle Kotlin script cannot (Gradle 9.6 rejects script-object captures).
// The preflight receives credentials only through the process environment.
val checkReleaseSigning = tasks.register<org.gradle.api.tasks.Exec>("checkReleaseSigning") {
    group = "verification"
    description = "Require the permanent signing keystore before a release build"
    workingDir = rootProject.projectDir
    commandLine("bash", "tools/check_release_signing.sh")
}
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach { dependsOn(checkReleaseSigning) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))

    // Compose UI + app state + navigation
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)

    // Cloud identity and per-user persistent library
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Used in stages 3-7: artwork, preferences, API calls, JSON backup
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
