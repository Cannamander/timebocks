#!/usr/bin/env bash
# Run this in a normal terminal (outside Cursor), not the integrated terminal, so the
# emulator keeps running if the IDE restarts.
set -euo pipefail

AVD_NAME="${AVD_NAME:-Timebox_API34}"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

if [[ ! -x "$ANDROID_HOME/emulator/emulator" ]]; then
  echo "Install the emulator first: sdkmanager \"emulator\" \"system-images;android-34;google_apis;x86_64\""
  exit 1
fi

if adb devices 2>/dev/null | grep -q "emulator-.*[[:space:]]device$"; then
  echo "An emulator is already running:"
  adb devices
  exit 0
fi

LOG="${TMPDIR:-/tmp}/timebox-emulator.log"
echo "Starting AVD=$AVD_NAME (log: $LOG)"
nohup "$ANDROID_HOME/emulator/emulator" -avd "$AVD_NAME" -gpu auto >>"$LOG" 2>&1 &
EMU_PID=$!
echo "emulator PID=$EMU_PID"
echo "Wait for the window; then run: scripts/install-to-emulator.sh"
