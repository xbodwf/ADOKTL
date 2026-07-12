plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    jvm { compilations.all { kotlinOptions.jvmTarget = "17" } }
    androidTarget { compilations.all { kotlinOptions.jvmTarget = "17" } }
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.core)
            }
        }
        val androidMain by getting
        val jvmMain by getting
        val linuxX64Main by getting
        val mingwX64Main by getting
    }
}

android {
    namespace = "com.adoktl.core"
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
