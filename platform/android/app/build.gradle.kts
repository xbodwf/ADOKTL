import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget()

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":render"))
                implementation(project(":ui"))
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.kotlin.coroutines.android)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
            }
        }
    }
}

android {
    namespace = "com.adoktl.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adoktl.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.register<Copy>("packageReleaseXapk") {
    group = "build"
    description = "Packages release APK as xapk (sideload format)"

    dependsOn("assembleRelease")

    val appVersionName = "1.0.0"
    val appVersionCode = 1
    val buildDir = layout.buildDirectory.get().asFile
    val apkFile = file("${buildDir}/outputs/apk/release/release/${project.name}-release.apk")
    val outDir = file("${buildDir}/outputs/xapk")

    inputs.file(apkFile)
    outputs.dir(outDir)

    from(outDir) { exclude("*.apk", "manifest.json") }

    from(apkFile) { rename { "app.apk" } }

    doFirst {
        val manifest = """
            {
              "app_name": "ADOKTL",
              "package_name": "com.adoktl.android",
              "version_name": "${appVersionName}",
              "version_code": ${appVersionCode},
              "min_sdk_version": 26,
              "target_sdk_version": 35,
              "split_config": [],
              "expansion": []
            }
        """.trimIndent()
        outDir.mkdirs()
        file("$outDir/manifest.json").writeText(manifest)
    }

    doLast {
        val xapkFile = file("${outDir}/${project.name}-${appVersionName}.xapk")
        val manifestBytes = file("${outDir}/manifest.json").readBytes()
        val apkBytes = apkFile.readBytes()

        ZipOutputStream(FileOutputStream(xapkFile)).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestBytes)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("app.apk"))
            zos.write(apkBytes)
            zos.closeEntry()
        }

        file("${outDir}/manifest.json").delete()
        println("XAPK written to: $xapkFile")
    }
}

tasks.register<Zip>("packageDebugApks") {
    group = "build"
    description = "Packages all debug APKs into apks archive (multi-ABI)"

    dependsOn("assembleDebug")

    val appVersionName = "1.0.0"

    archiveFileName.set("${project.name}-${appVersionName}-debug.apks")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/apks"))

    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include("**/*.apk")
    }
}