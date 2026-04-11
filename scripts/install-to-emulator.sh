#!/usr/bin/env bash
# Build and install the debug APK. Uses project-local Gradle user home to avoid cache
# lock fights with other Gradle runs.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

# Optional: export GRADLE_USER_HOME="$ROOT/.gradle-user" if Gradle cache locks with other IDEs.
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"

if ! command -v adb >/dev/null; then
  echo "adb not found. Add to PATH: \$ANDROID_HOME/platform-tools"
  exit 1
fi

echo "Waiting for adb device (timeout 120s)..."
timeout 120 bash -c 'until [[ "$(adb get-state 2>/dev/null || true)" == "device" ]]; do sleep 1; done'
adb devices

echo "Building and installing (no daemon, less likely to fight other Gradle)..."
./gradlew installDebug --no-daemon --max-workers=2

echo "Launching app..."
adb shell am start -n com.timebox.app/.MainActivity

echo "Done."
