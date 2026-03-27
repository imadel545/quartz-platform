#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/mobile-android"
DEFAULT_TASK=":app:connectedDebugAndroidTest"

print_usage() {
  cat <<'EOF'
Usage:
  ./scripts/android-connected-tests.sh --preflight
  ./scripts/android-connected-tests.sh [gradle args...]

Examples:
  ./scripts/android-connected-tests.sh
  ./scripts/android-connected-tests.sh --stacktrace --info
  ./scripts/android-connected-tests.sh -Pandroid.testInstrumentationRunnerArguments.class=com.quartz.platform.ExampleInstrumentedTest
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  print_usage
  exit 0
fi

PREFLIGHT_ONLY=false
if [[ "${1:-}" == "--preflight" ]]; then
  PREFLIGHT_ONLY=true
  shift
fi

if [[ ! -f "$ANDROID_DIR/local.properties" ]]; then
  echo "ERROR: mobile-android/local.properties is missing."
  echo "Copy mobile-android/local.properties.example to mobile-android/local.properties and set sdk.dir."
  exit 1
fi

sdk_dir_from_local_properties="$(
  sed -n 's/^sdk.dir=//p' "$ANDROID_DIR/local.properties" | tail -n1
)"

if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
  SDK_ROOT="$ANDROID_SDK_ROOT"
elif [[ -n "${ANDROID_HOME:-}" ]]; then
  SDK_ROOT="$ANDROID_HOME"
elif [[ -n "$sdk_dir_from_local_properties" ]]; then
  SDK_ROOT="$sdk_dir_from_local_properties"
else
  echo "ERROR: Android SDK path not found."
  echo "Set sdk.dir in mobile-android/local.properties or export ANDROID_SDK_ROOT."
  exit 1
fi

if [[ ! -d "$SDK_ROOT" ]]; then
  echo "ERROR: Android SDK directory does not exist: $SDK_ROOT"
  exit 1
fi

export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

ADB_BIN="$(command -v adb || true)"
if [[ -z "$ADB_BIN" ]]; then
  echo "ERROR: adb is not available."
  echo "Install platform-tools for SDK root $ANDROID_SDK_ROOT:"
  echo "  sdkmanager --sdk_root=\"$ANDROID_SDK_ROOT\" \"platform-tools\""
  exit 1
fi

echo "SDK root: $ANDROID_SDK_ROOT"
echo "adb: $ADB_BIN"

"$ADB_BIN" start-server >/dev/null 2>&1 || true

connected_devices="$("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -z "$connected_devices" ]]; then
  echo
  echo "No online Android device/emulator detected."
  echo "A connected target is mandatory for $DEFAULT_TASK."

  emulator_bin="$ANDROID_SDK_ROOT/emulator/emulator"
  avdmanager_bin="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager"

  if [[ ! -x "$emulator_bin" ]]; then
    echo
    echo "Emulator binary not found in SDK."
    echo "Install emulator + ARM64 system image (Android 35):"
    echo "  sdkmanager --sdk_root=\"$ANDROID_SDK_ROOT\" \"emulator\" \"system-images;android-35;default;arm64-v8a\""
  else
    avds="$("$emulator_bin" -list-avds 2>/dev/null || true)"
    if [[ -z "$avds" ]]; then
      echo
      echo "No AVD found."
      if [[ -x "$avdmanager_bin" ]]; then
        echo "Create one AVD:"
        echo "  echo \"no\" | avdmanager create avd -n quartzApi35 -k \"system-images;android-35;default;arm64-v8a\" --device \"medium_phone\""
      else
        echo "avdmanager not found under cmdline-tools/latest/bin."
      fi
    else
      first_avd="$(printf '%s\n' "$avds" | head -n1)"
      echo
      echo "Available AVD(s):"
      printf '%s\n' "$avds"
      echo
      echo "Start one AVD (headless):"
      echo "  \"$emulator_bin\" -avd \"$first_avd\" -no-window -no-audio"
    fi
  fi

  exit 2
fi

echo "Connected target(s):"
printf '%s\n' "$connected_devices"

if [[ "$PREFLIGHT_ONLY" == true ]]; then
  echo "Preflight OK."
  exit 0
fi

cd "$ANDROID_DIR"
echo "Running $DEFAULT_TASK ..."
./gradlew "$DEFAULT_TASK" --console=plain "$@"
