#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/mobile-android"

WITH_CONNECTED=false
NO_DAEMON="${ANDROID_QG_NO_DAEMON:-true}"
MIN_FREE_GB="${ANDROID_QG_MIN_FREE_GB:-4}"

print_usage() {
  cat <<'EOF'
Usage:
  ./scripts/android-quality-gate.sh
  ./scripts/android-quality-gate.sh --with-connected
  ./scripts/android-quality-gate.sh --help

Runs Android validation tasks in a deterministic sequential order:
  1) :app:assembleDebug
  2) :app:testDebugUnitTest
  3) :app:lintDebug
  4) :app:compileDebugAndroidTestKotlin
  5) :app:connectedDebugAndroidTest (only with --with-connected)

Environment knobs:
  ANDROID_QG_NO_DAEMON=true|false      (default: true)
  ANDROID_QG_MIN_FREE_GB=<int>         (default: 4)
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-connected)
      WITH_CONNECTED=true
      shift
      ;;
    --help|-h)
      print_usage
      exit 0
      ;;
    *)
      echo "ERROR: unknown argument: $1" >&2
      print_usage
      exit 1
      ;;
  esac
done

if [[ ! -d "$ANDROID_DIR" ]]; then
  echo "ERROR: Android directory not found: $ANDROID_DIR" >&2
  exit 1
fi

if [[ ! -f "$ANDROID_DIR/local.properties" ]]; then
  echo "ERROR: $ANDROID_DIR/local.properties is missing." >&2
  echo "Copy mobile-android/local.properties.example and set sdk.dir." >&2
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
  echo "ERROR: Android SDK path not found." >&2
  echo "Set sdk.dir in mobile-android/local.properties or export ANDROID_SDK_ROOT." >&2
  exit 1
fi

if [[ ! -d "$SDK_ROOT" ]]; then
  echo "ERROR: Android SDK directory does not exist: $SDK_ROOT" >&2
  exit 1
fi

export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

available_kb="$(df -Pk "$ANDROID_DIR" | awk 'NR==2 {print $4}')"
required_kb=$((MIN_FREE_GB * 1024 * 1024))
if [[ "$available_kb" -lt "$required_kb" ]]; then
  echo "ERROR: low disk space near project filesystem." >&2
  echo "Available: $((available_kb / 1024 / 1024)) GiB, required: ${MIN_FREE_GB} GiB." >&2
  echo "Free disk space before running quality gate." >&2
  exit 1
fi

GRADLE_ARGS=(--console=plain)
if [[ "$NO_DAEMON" == "true" ]]; then
  GRADLE_ARGS+=(--no-daemon)
fi

run_task() {
  local task="$1"
  echo
  echo "==> Running $task"
  ./gradlew "$task" "${GRADLE_ARGS[@]}"
}

cd "$ANDROID_DIR"

run_task ":app:assembleDebug"
run_task ":app:testDebugUnitTest"
run_task ":app:lintDebug"
run_task ":app:compileDebugAndroidTestKotlin"

if [[ "$WITH_CONNECTED" == "true" ]]; then
  ADB_BIN="$(command -v adb || true)"
  if [[ -z "$ADB_BIN" ]]; then
    echo "ERROR: adb is not available in PATH." >&2
    exit 1
  fi

  "$ADB_BIN" start-server >/dev/null 2>&1 || true
  connected_devices="$("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}')"
  if [[ -z "$connected_devices" ]]; then
    echo "ERROR: --with-connected requested but no online device/emulator detected." >&2
    echo "Run ./scripts/android-connected-tests.sh --preflight for exact provisioning steps." >&2
    exit 2
  fi

  echo
  echo "Connected target(s):"
  printf '%s\n' "$connected_devices"
  run_task ":app:connectedDebugAndroidTest"
fi

echo
echo "Android quality gate completed successfully."
