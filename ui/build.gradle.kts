plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
}

kotlin {
    jvm { compilations.all { kotlinOptions.jvmTarget = "17" } }
    androidTarget { compilations.all { kotlinOptions.jvmTarget = "17" } }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":render"))
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
            }
        }
        val androidMain by getting
        val jvmMain by getting
    }
}

android {
    namespace = "com.adoktl.ui"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

group = "com.adoktl"
version = "0.1.0"
