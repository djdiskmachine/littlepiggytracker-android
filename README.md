# littlepiggytracker-android
Android port of piggy tracker
Submodule of LittleGPTracker
Clone LittleGPTRacker repo recursively
Install android-sdk, gradle, etc
sudo apt update && sudo apt install gradle android-sdk -y
Download NDK: Install Android studio and use GUI
Install and run
./gradlew assembleDebug && adb install --user 0 ./app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n org.neocities.djdiskmachine.lgpt_android/.MainActivity

Debug:
adb logcat --pid=$(adb shell pidof org.neocities.djdiskmachine.lgpt_android)