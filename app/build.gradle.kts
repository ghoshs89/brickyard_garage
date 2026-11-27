import java.util.Properties

plugins {
    id("com.android.application")
}

// Release signing: keystore outside the repo, passwords in ~/.gradle/gradle.properties.
val keystore = File(System.getProperty("user.home"), ".android-keys/brickyard-garage.jks")
val storePass = providers.gradleProperty("BRICKYARD_STORE_PASSWORD").orNull
val keyPass = providers.gradleProperty("BRICKYARD_KEY_PASSWORD").orNull

val versionProps = Properties().apply {
    val f = rootProject.file("version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.brickyard.app"
    compileSdk = 35

    defaultConfig {
        // Never change applicationId: updates must install over the saved game.
        applicationId = "com.brickyard.garage"
        minSdk = 22
        targetSdk = 35
        versionCode = (versionProps.getProperty("versionCode") ?: "1").toInt()
        versionName = versionProps.getProperty("versionName") ?: "1.0"
    }

    signingConfigs {
        create("release") {
            if (keystore.exists() && storePass != null) {
                storeFile = keystore
                storePassword = storePass
                keyAlias = "brickyard"
                keyPassword = keyPass ?: storePass
                enableV1Signing = true  // Fire OS 5 / API 22
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = if (keystore.exists() && storePass != null) signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main") {
            // Levels, blueprints, gear puzzles, campaign and garage data ship as assets, read by the same parsers the tools use.
            assets.srcDirs("../content", layout.buildDirectory.dir("generated/brickyardAudio").get().asFile)
        }
    }

    androidResources {
        noCompress += listOf("wav") // SoundPool and MediaPlayer read audio straight from the APK
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":engine"))
}

// Audio is synthesised at build time by :tools (original, royalty-free).
val synthAudio = tasks.register<JavaExec>("synthAudio") {
    group = "build"
    val outDir = layout.buildDirectory.dir("generated/brickyardAudio/audio")
    classpath = project(":tools").extensions.getByType<SourceSetContainer>()["main"].runtimeClasspath
    mainClass.set("com.brickyard.tools.BrickAudioSynth")
    args(outDir.get().asFile.absolutePath)
    inputs.files(project(":tools").fileTree("src/main/java/com/brickyard/tools") { include("BrickAudioSynth.java") })
    outputs.dir(outDir)
}
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach { dependsOn(synthAudio) }
tasks.matching { it.name.startsWith("generate") && it.name.endsWith("Assets") }.configureEach { dependsOn(synthAudio) }
tasks.matching { it.name == "lintVitalAnalyzeRelease" || it.name.startsWith("lint") }.configureEach { dependsOn(synthAudio) }

// The release APK is only built after the full content gate passes.
tasks.matching { it.name == "assembleRelease" }.configureEach { dependsOn(":tools:verifyAllContent") }

// Copy the signed release APK to dist/BrickyardGarage.apk.
tasks.register<Copy>("distApk") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/apk/release")) { include("*.apk") }
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "BrickyardGarage.apk" }
}
