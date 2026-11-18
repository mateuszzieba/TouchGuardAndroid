#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
APK="$PWD/output/TouchGuard-debug.apk"
[[ -f "$APK" ]] || { echo "Brak APK. Najpierw: ./build_apk_ubuntu.sh" >&2; exit 1; }
SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
if command -v adb >/dev/null 2>&1; then ADB="$(command -v adb)"
elif [[ -x "$SDK_DIR/platform-tools/adb" ]]; then ADB="$SDK_DIR/platform-tools/adb"
else
    echo "Brak adb. Uruchom najpierw ./build_apk_ubuntu.sh lub sudo apt install adb" >&2
    exit 1
fi
"$ADB" start-server
"$ADB" devices
"$ADB" install -r "$APK"
echo 'Zainstalowano TouchGuard. Uruchom aplikację na telefonie.'
