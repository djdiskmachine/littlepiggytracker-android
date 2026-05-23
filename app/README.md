# LittleGPTracker Android App

This is the Android application for LittleGPTracker.
A wrapper for the native code with on-screen overlays for configuration and fine-tuning
of the user experience.

## Key Features

- **NDK build**: Uses existing Makefiles via Gradle exec tasks
- **Automatic SDL2**: Downloads and builds SDL2 from official releases
- **Native Build**: Calls `Makefile.ANDROID` for each ABI
- **No Manual Dependencies**: Everything is automated via Gradle

## Building

Simply build the project in Android Studio or via command line:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug  # Install to connected device
```

## How It Works

1. **Download SDL2**: Gradle downloads SDL2 source from GitHub releases
2. **Build SDL2**: Builds `libSDL2.so` for each ABI using CMake
3. **Build LGPT**: Calls `../projects/Makefile.ANDROID` to build `libmain.so`
4. **Package APK**: Gradle packages both libraries into the APK

## File Structure

- `src/main/java/org/libsdl/app/` - SDL2 Activity (no native-lib.cpp needed)
- `src/main/jniLibs/` - Native libraries (auto-generated)
- `build.gradle.kts` - Build configuration with custom tasks
- CMake/cpp/ folder - Not used (can be deleted)

## Requirements

- Android Studio with NDK
- Python 3 (for font generation in Makefile)
