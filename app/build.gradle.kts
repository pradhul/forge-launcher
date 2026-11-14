import java.io.File
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.sduduzog.slimlauncher"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.pradhul.forge"
        minSdk = 23
        targetSdk = 36
        versionName = "1.0.0"
        versionCode = 87
        vectorDrawables { useSupportLibrary = true }
        ndk {
            abiFilters += ((project.findProperty("forgeAbi") as? String)?.takeIf { it.isNotBlank() }
                ?: "arm64-v8a")
        }
        signingConfigs {
            if (project.extra.has("RELEASE_STORE_FILE")) {
                register("release") {
                    storeFile = file(project.extra["RELEASE_STORE_FILE"] as String)
                    storePassword = project.extra["RELEASE_STORE_PASSWORD"] as String
                    keyAlias = project.extra["RELEASE_KEY_ALIAS"] as String
                    keyPassword = project.extra["RELEASE_KEY_PASSWORD"] as String
                }
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    buildTypes {
        named("release").configure {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.maybeCreate("release")
        }
        named("debug").configure {
            // Same debug keystore as before (default debug signingConfig).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
            val keepAbi = (project.findProperty("forgeAbi") as? String)?.takeIf { it.isNotBlank() }
                ?: "arm64-v8a"
            val allAbis = listOf("armeabi", "armeabi-v7a", "arm64-v8a", "x86", "x86_64", "mips", "mips64")
            excludes += allAbis.filter { it != keepAbi }.map { "**/$it/**" }
        }
        resources {
            excludes += listOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/androidx/**",
                "META-INF/androidx*.version",
                "kotlin/**"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    lint {
        abortOnError = false
    }
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":feature-habits"))

    // Support Libraries
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Arch Components
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.fragment:fragment-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.7")
    kapt("androidx.room:room-compiler:2.8.4")

    //3rd party libs
    implementation("com.intuit.sdp:sdp-android:1.0.6")
    implementation("com.intuit.ssp:ssp-android:1.0.6")
    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-compiler:2.57.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // LiteRT 1.4+ ships 16 KB-aligned natives and keeps Interpreter + GPU.
    implementation("com.google.ai.edge.litert:litert:1.4.2")
    implementation("com.google.ai.edge.litert:litert-gpu:1.4.2")
    implementation("com.google.ai.edge.litert:litert-support:1.4.2")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.graphics:graphics-path:1.1.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Unit test libs
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    testImplementation("com.google.dagger:hilt-android-testing:2.57.2")
    kaptTest("com.google.dagger:hilt-android-compiler:2.57.2")

    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
}
kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.graphics" && requested.name == "graphics-path") {
            useVersion("1.1.0")
            because("16 KB page-size ELF alignment")
        }
    }
}

val check16kbAlignment = tasks.register("check16kbAlignment") {
    group = "verification"
    description = "Require 16 KB PT_LOAD alignment in arm64-v8a .so files and zipalign -P 16"
    dependsOn("packageDebug")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        require(apk.isFile) { "Missing debug APK at ${apk.absolutePath}" }
        val reports = ArrayList<String>()
        var failures = 0
        ZipFile(apk).use { zip ->
            val sos = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("lib/arm64-v8a/") && it.name.endsWith(".so") }
                .toList()
            require(sos.isNotEmpty()) { "${apk.name} has no lib/arm64-v8a/*.so" }
            for (entry in sos.sortedBy { it.name }) {
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                val line = reportElfLoadAlign(entry.name, bytes)
                reports.add(line)
                logger.lifecycle(line)
                if (line.startsWith("FAIL")) failures += 1
            }
        }
        val zipalign = findZipalign(android.sdkDirectory)
        val zipOut = providers.exec {
            commandLine(
                zipalign.absolutePath,
                "-c",
                "-P",
                "16",
                "-v",
                "4",
                apk.absolutePath
            )
            isIgnoreExitValue = true
        }.standardOutput.asText.get()
        logger.lifecycle(zipOut.trim())
        val zipOk = zipOut.contains("Verification succesful") ||
            zipOut.contains("Verification successful")
        if (!zipOk) {
            failures += 1
            reports.add("FAIL zipalign -c -P 16 -v 4")
        } else {
            reports.add("OK zipalign -c -P 16 -v 4")
        }
        val reportFile = layout.buildDirectory.file("outputs/apk/debug/16kb-alignment.txt").get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(reports.joinToString("\n") + "\n")
        if (failures > 0) {
            throw GradleException(
                "16 KB alignment check failed ($failures). See ${reportFile.absolutePath}"
            )
        }
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(check16kbAlignment)
}

fun findZipalign(sdkDir: File): File {
    val tools = File(sdkDir, "build-tools")
    val latest = tools.listFiles()
        ?.filter { File(it, "zipalign").isFile || File(it, "zipalign.exe").isFile }
        ?.maxByOrNull { it.name }
        ?: throw GradleException("zipalign not found under ${tools.absolutePath}")
    val bin = File(latest, "zipalign")
    return if (bin.isFile) bin else File(latest, "zipalign.exe")
}

fun reportElfLoadAlign(name: String, bytes: ByteArray): String {
    if (bytes.size < 64 ||
        bytes[0] != 0x7f.toByte() ||
        bytes[1] != 'E'.code.toByte() ||
        bytes[2] != 'L'.code.toByte() ||
        bytes[3] != 'F'.code.toByte()
    ) {
        return "FAIL $name not ELF"
    }
    if (bytes[4].toInt() != 2 || bytes[5].toInt() != 1) {
        return "FAIL $name not ELF64 LE"
    }
    fun u16(offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }
    fun u32(offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }
    fun u64(offset: Int): Long {
        return (u32(offset).toLong() and 0xFFFFFFFFL) or
            ((u32(offset + 4).toLong() and 0xFFFFFFFFL) shl 32)
    }
    val phoff = u64(32)
    val phentsize = u16(54)
    val phnum = u16(56)
    if (phentsize < 56) return "FAIL $name phdr too small"
    val aligns = ArrayList<Long>()
    for (index in 0 until phnum) {
        val base = phoff.toInt() + index * phentsize
        if (u32(base) != 1) continue
        aligns.add(u64(base + 48))
    }
    val minAlign = aligns.minOrNull() ?: 0L
    val bad = aligns.filter { it < 0x4000L }
    return if (bad.isEmpty()) {
        "OK $name loads=${aligns.size} min_p_align=0x${minAlign.toString(16)}"
    } else {
        "FAIL $name unaligned=" + bad.joinToString { "0x${it.toString(16)}" }
    }
}