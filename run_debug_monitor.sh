#!/bin/bash
./gradlew installDebug && \
adb shell monkey -p com.fyp.resilientp2p 1 && \
adb logcat | grep "com.fyp.resilientp2p" > log.log
