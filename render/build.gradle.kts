plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
                implementation(project(":core"))
                implementation(libs.kotlin.coroutines.core)
            }
        }
        val androidMain by getting
        val jvmMain by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengl)
            }
        }
        val linuxX64Main by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengl)
            }
        }
        val mingwX64Main by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengl)
            }
        }
    }
}

android {
    namespace = "com.adoktl.render"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
}

group = "com.adoktl"
version = "0.1.0"
