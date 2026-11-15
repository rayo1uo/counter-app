#!/usr/bin/env bash
set -euo pipefail
apk_dir="${1:?用法: verify-device.sh APK目录}"
adb_bin="${ADB:-adb}"
adb_cmd=("$adb_bin")
if [[ -n "${ANDROID_SERIAL:-}" ]]; then adb_cmd+=(-s "$ANDROID_SERIAL"); fi
"${adb_cmd[@]}" install -r "$apk_dir/app-validation.apk"
"${adb_cmd[@]}" install -r "$apk_dir/app-validation-androidTest.apk"
result_file="$(mktemp)"
trap 'rm -f "$result_file"' EXIT
"${adb_cmd[@]}" shell am instrument -w com.example.counter.validation.test/androidx.test.runner.AndroidJUnitRunner | tee "$result_file"
if ! grep -Eq '^OK \([0-9]+ tests?\)' "$result_file"; then
    echo '设备验收未通过' >&2
    exit 1
fi
