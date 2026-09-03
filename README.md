# littlepiggytracker-android
## Android port of piggy tracker, submodule of LittleGPTracker

Clone LittleGPTRacker repo recursively

Install android-sdk, gradle, etc
```
sudo apt update && sudo apt install gradle android-sdk -y
```
Download NDK: Install Android Studio and use GUI
Install version 27.0.12077973
Install and run:
```
./gradlew assembleDebug && adb install --user 0 ./app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n net.djdiskmachi.lgpt_android/.MainActivity
```

TOOLCHAIN in Makefile.ANDROID variable might need changing depending on your build host system 
```
TOOLCHAIN := $(NDK_PATH)/toolchains/llvm/prebuilt/darwin-x86_64
or
TOOLCHAIN := $(NDK_PATH)/toolchains/llvm/prebuilt/linux-x86_64
etc.
```
Debug:
adb logcat --pid=$(adb shell pidof net.djdiskmachi.lgpt_android)
