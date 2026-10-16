#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ -f app/google-services.json ]] || { echo 'Missing app/google-services.json' >&2; exit 1; }
read -r -p 'Path to your permanent release keystore: ' GV_RELEASE_STORE_FILE
[[ -f "$GV_RELEASE_STORE_FILE" ]] || { echo 'Keystore not found' >&2; exit 1; }
GV_RELEASE_KEY_ALIAS=gamevault
read -r -s -p 'Keystore password: ' GV_RELEASE_STORE_PASSWORD; echo
read -r -s -p 'Key password (often same as keystore): ' GV_RELEASE_KEY_PASSWORD; echo
export GV_RELEASE_STORE_FILE GV_RELEASE_STORE_PASSWORD GV_RELEASE_KEY_ALIAS GV_RELEASE_KEY_PASSWORD
if [[ -f gradle/wrapper/gradle-wrapper.jar ]]; then
  ./gradlew :app:testDebugUnitTest :app:assembleRelease
elif command -v gradle >/dev/null 2>&1; then
  gradle :app:testDebugUnitTest :app:assembleRelease
else
  echo 'Gradle wrapper JAR is absent and gradle not installed; use GitHub Actions.' >&2; exit 1
fi
source tools/release_helpers.sh
APK="$(locate_release_apk)"
verify_release_signature "$APK"
echo "Signed APK ready: $APK"
