plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.core)
            }
        }
        val jvmMain by getting
        val linuxX64Main by getting
        val mingwX64Main by getting
    }
}

group = "com.adoktl"
version = "0.1.0"