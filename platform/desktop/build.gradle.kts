plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":render"))
                implementation(libs.kotlin.coroutines.core)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":ui"))
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengl)
                implementation("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-windows")
                implementation("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:natives-windows")
                implementation("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-windows")
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(compose.desktop.currentOs)
            }
        }

        val linuxX64Main by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengl)
                implementation("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-linux")
                implementation("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:natives-linux")
                implementation("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-linux")
                implementation(compose.desktop.currentOs)
            }
        }

        val mingwX64Main by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.opengl)
                implementation("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-windows")
                implementation("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:natives-windows")
                implementation("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-windows")
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.adoktl.platform.desktop.LauncherKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage
            )
            packageName = "ADOKTL"
            packageVersion = "1.0.0"
            description = "A Dance of Fire and Ice - Kotlin Player"
            vendor = "ADOKTL"

            linux {
                packageName = "adoktl"
                debPackageVersion = "0.1.0-1"
            }

            windows {
                menuGroup = "ADOKTL"
                upgradeUuid = "adoktl-player-0000-0000-0000-000000000000"
            }

            macOS {
                bundleID = "com.adoktl.player"
            }
        }

        buildTypes {
            release {
                proguard {
                    isEnabled = true
                    configurationFiles.from(project.file("proguard-rules.pro"))
                }
            }
        }
    }
}

group = "com.adoktl"
version = "0.1.0"