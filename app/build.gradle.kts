plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Configuration for SDL2 and native build
val sdlVersion = "2.30.9"
val sdlUrl = "https://github.com/libsdl-org/SDL/releases/download/release-$sdlVersion/SDL2-$sdlVersion.tar.gz"
val sdlDownloadDir = layout.buildDirectory.dir("sdl-download").get().asFile
val sdlExtractDir = layout.buildDirectory.dir("sdl-extracted").get().asFile

android {
    namespace = "org.neocities.djdiskmachine.lgpt_android"
    compileSdk = 36
    defaultConfig {
        applicationId = "org.neocities.djdiskmachine.lgpt_android"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }


    signingConfigs {
        create("release") {
            storeFile = file(System.getProperty("user.home") + "/lgpt.jks")
            storePassword = "lgptpass"
            keyAlias = "lgpt"
            keyPassword = "lgptpass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    
    buildFeatures {
        viewBinding = true
    }
}

// Task to download SDL2
val downloadSDL2 by tasks.registering {
    description = "Download SDL2 source from official release"
    group = "SDL2"
    
    val outputFile = file("$sdlDownloadDir/SDL2-${sdlVersion}.tar.gz")
    
    outputs.file(outputFile)
    
    doLast {
        sdlDownloadDir.mkdirs()
        if (!outputFile.exists()) {
            println("Downloading SDL2 $sdlVersion from $sdlUrl")
            ant.invokeMethod("get", mapOf(
                "src" to sdlUrl,
                "dest" to outputFile,
                "verbose" to true
            ))
        } else {
            println("SDL2 already downloaded: $outputFile")
        }
    }
}

// Task to extract SDL2
val extractSDL2 by tasks.registering(Copy::class) {
    description = "Extract SDL2 source archive"
    group = "SDL2"
    
    dependsOn(downloadSDL2)
    
    from(tarTree(resources.gzip("$sdlDownloadDir/SDL2-${sdlVersion}.tar.gz")))
    into(sdlExtractDir)
    
    doFirst {
        sdlExtractDir.mkdirs()
    }
    
    doLast {
        // Create SDL2 subdirectory in include for <SDL2/SDL.h> style includes
        val includeDir = file("$sdlExtractDir/SDL2-$sdlVersion")
        val sdl2Dir = file("$includeDir/SDL2")
        if (!sdl2Dir.exists()) {
            sdl2Dir.mkdirs()
            // Create symlinks or copy headers to SDL2 subdirectory
            copy {
                from("$includeDir/include")
                into("$includeDir/SDL2")
            }
        }
    }
}

// Task to copy SDL2 Android makefiles and build
val copySDL2Libs by tasks.registering {
    description = "Build SDL2 for Android using ndk-build"
    group = "SDL2"
    
    dependsOn(extractSDL2)
    
    val sdl2Armv7 = file("src/main/jniLibs/armeabi-v7a/libSDL2.so")
    val sdl2Arm64 = file("src/main/jniLibs/arm64-v8a/libSDL2.so")
    
    outputs.files(sdl2Armv7, sdl2Arm64)
    
    // Only run if SDL2 libraries don't exist
    onlyIf {
        !sdl2Armv7.exists() || !sdl2Arm64.exists()
    }
    
    doLast {
        val sdlSrcDir = file("$sdlExtractDir/SDL2-$sdlVersion")
        val jniDir = file("src/main/jni")
        
        // Create JNI directory structure for SDL2 build
        jniDir.mkdirs()
        
        // Create Application.mk
        file("$jniDir/Application.mk").writeText("""
APP_PLATFORM := android-21
APP_ABI := armeabi-v7a arm64-v8a
APP_STL := c++_shared
        """.trimIndent())
        
        // Create Android.mk that builds SDL2
        file("$jniDir/Android.mk").writeText("""
LOCAL_PATH := ${'$'}(call my-dir)
include ${'$'}(CLEAR_VARS)
include $sdlSrcDir/Android.mk
        """.trimIndent())
        
        // Build SDL2 for both ABIs
        file("src/main/jniLibs/armeabi-v7a").mkdirs()
        file("src/main/jniLibs/arm64-v8a").mkdirs()
        
        exec {
            workingDir = file("src/main")
            commandLine(
                "${android.ndkDirectory}/ndk-build",
                "NDK_PROJECT_PATH=.",
                "NDK_OUT=$jniDir/obj",
                "NDK_LIBS_OUT=jniLibs"
            )
        }
        
        // Clean up temporary build files
        delete(jniDir)
    }
}

// Task to build LittleGPTracker for armeabi-v7a
val buildLGPTArmv7 by tasks.registering(Exec::class) {
    description = "Build LittleGPTracker native library for armeabi-v7a using Makefile"
    group = "Native Build"
    
    dependsOn(copySDL2Libs)
    
    val sdlRoot = file("$sdlExtractDir/SDL2-$sdlVersion")
    val sdlLib = file("src/main/jniLibs/armeabi-v7a")
    val outputLib = file("src/main/jniLibs/armeabi-v7a/libmain.so")
    
    // Track source files as inputs so Gradle rebuilds when they change
    inputs.dir(file("$projectDir/../../sources"))
    inputs.file(file("$projectDir/../../projects/Makefile.ANDROID"))
    
    outputs.file(outputLib)
    outputs.file(file("$projectDir/../../projects/libmain_armeabi-v7a.so"))
    
    workingDir = file("$projectDir/../../projects")
    
    environment("PWD", file("$projectDir/../../projects").absolutePath)
    environment("PLATFORM", "ANDROID")
    environment("ABI", "armeabi-v7a")
    environment("ANDROID_NDK_HOME", android.ndkDirectory.absolutePath)
    environment("SDL_INCLUDE", sdlRoot.absolutePath)
    environment("SDL_LIB", sdlLib.absolutePath)
    
    commandLine("make", "PLATFORM=ANDROID")
    
    doLast {
        // Copy the built library
        copy {
            from("$projectDir/../../projects/libmain_armeabi-v7a.so")
            into("src/main/jniLibs/armeabi-v7a")
            rename { "libmain.so" }
        }
    }
}

// Task to build LittleGPTracker for arm64-v8a
val buildLGPTArm64 by tasks.registering(Exec::class) {
    description = "Build LittleGPTracker native library for arm64-v8a using Makefile"
    group = "Native Build"
    
    dependsOn(copySDL2Libs)
    
    val sdlRoot = file("$sdlExtractDir/SDL2-$sdlVersion")
    val sdlLib = file("src/main/jniLibs/arm64-v8a")
    val outputLib = file("src/main/jniLibs/arm64-v8a/libmain.so")
    
    // Track source files as inputs so Gradle rebuilds when they change
    inputs.dir(file("$projectDir/../../sources"))
    inputs.file(file("$projectDir/../../projects/Makefile.ANDROID"))
    
    outputs.file(outputLib)
    outputs.file(file("$projectDir/../../projects/libmain_arm64-v8a.so"))
    
    workingDir = file("$projectDir/../../projects")
    
    environment("PWD", file("$projectDir/../../projects").absolutePath)
    environment("PLATFORM", "ANDROID")
    environment("ABI", "arm64-v8a")
    environment("ANDROID_NDK_HOME", android.ndkDirectory.absolutePath)
    environment("SDL_INCLUDE", sdlRoot.absolutePath)
    environment("SDL_LIB", sdlLib.absolutePath)
    
    commandLine("make", "PLATFORM=ANDROID")
    
    doLast {
        // Copy the built library
        copy {
            from("$projectDir/../../projects/libmain_arm64-v8a.so")
            into("src/main/jniLibs/arm64-v8a")
            rename { "libmain.so" }
        }
    }
}

// Make the preBuild task depend on our native builds
tasks.named("preBuild") {
    dependsOn(buildLGPTArmv7, buildLGPTArm64)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}