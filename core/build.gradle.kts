plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    jvm()
    androidTarget()
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
}

group = "com.adoktl"
version = "0.1.0"
