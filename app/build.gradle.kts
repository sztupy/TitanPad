plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.rust)
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    kotlin("plugin.serialization") version "1.9.0"
}

android {
    namespace = "scot.raven.titanpad"
    compileSdk = 36
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "scot.raven.titanpad"
        minSdk = 29
        targetSdk = 36
        versionCode = 5
        versionName = "0.3.0-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
        resValues = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            resValue("string", "app_name", "TitanPad Debug")
            resValue("string", "accessibility_service_label", "TitanPad Debug")
            resValue("color", "ic_launcher_background", value="#FFAAAA")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    //noinspection WrongGradleMethod
    kotlin {
        jvmToolchain(21)
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

androidRust {
    module("titanpad_rust") {
        path = file("src/main/rust")
        targets = listOf("arm", "arm64", "x86", "x86_64")

        buildType("debug") {
            profile = "dev"
            runTests = true
        }

        buildType("release") {
            profile = "release"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.savedstate.ktx)
    implementation(libs.compose.material.icons)
    //noinspection UseTomlInstead
    implementation("dev.rikka.shizuku:api:13.1.5")
    //noinspection UseTomlInstead
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.kotlinx.serialization.json)
}