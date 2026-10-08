#!/usr/bin/env bash
set -euo pipefail

echo "Runtime APK artifact contents:"
find ci-apks -type f -maxdepth 4 -print | sort

APP_APK="$(find ci-apks/debug -maxdepth 1 -type f -name '*-debug.apk' | head -n 1)"
TEST_APK="$(find ci-apks/androidTest/debug -maxdepth 1 -type f -name '*-debug-androidTest.apk' | head -n 1)"

test -n "$APP_APK"
test -n "$TEST_APK"

echo "Android /data capacity before install:"
adb shell df -h /data

echo "Installing application APK: $APP_APK"
adb install -r "$APP_APK"

echo "Installing instrumentation APK: $TEST_APK"
adb install -r "$TEST_APK"

AAPT="$(find "$ANDROID_HOME/build-tools" -type f -name aapt | sort -V | tail -n 1)"
test -n "$AAPT"
TEST_PACKAGE="$("$AAPT" dump badging "$TEST_APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
APP_PACKAGE="$("$AAPT" dump badging "$APP_APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
test -n "$TEST_PACKAGE"
test -n "$APP_PACKAGE"

adb logcat -c

set +e
OUTPUT="$(adb shell am instrument -w "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" 2>&1)"
STATUS=$?
set -e

printf '%s\n' "$OUTPUT"

if [ "$STATUS" -ne 0 ] || ! printf '%s\n' "$OUTPUT" | grep -Eq 'OK \([1-9][0-9]* tests?\)'; then
    echo "Instrumentation did not complete successfully. Recent Android logcat:"
    adb logcat -d -v threadtime | tail -n 1500
    exit 1
fi


echo "Collecting rendered GoreeCloud Since evidence."
mkdir -p ci-screenshots
for screenshot in \
  dashboard-empty \
  dashboard-empty-dark \
  main-activity-light \
  main-activity-dark \
  tracker-type-chooser \
  tracker-type-chooser-dark \
  create-streak \
  create-streak-dark \
  time-zone-picker \
  time-zone-picker-dark \
  tracker-details \
  tracker-details-dark \
  reset-streak \
  reset-streak-dark \
  history \
  history-dark \
  dashboard-populated \
  dashboard-populated-dark \
  achievements \
  achievements-dark \
  settings \
  settings-dark \
  settings-recovery \
  settings-recovery-dark \
  settings-about \
  settings-about-dark
do
    destination="ci-screenshots/${screenshot}.png"
    adb exec-out run-as "$APP_PACKAGE" cat "files/visual-evidence/${screenshot}.png" > "$destination"
    test -s "$destination"
done

echo "Rendered evidence files:"
find ci-screenshots -maxdepth 1 -type f -name '*.png' -print | sort
