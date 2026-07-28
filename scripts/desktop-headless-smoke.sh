#!/usr/bin/env bash
# Quick headless desktop launch smoke (substitute for manual keyboard/dialog QA when no display).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
./gradlew -p desktop-standalone desktopJar --quiet
JAR="$ROOT/desktop-standalone/engine/build/libs/CFHC-desktop-${desktopVersion:-1.4e}.jar"
if [[ ! -f "$JAR" ]]; then
  # Resolve from gradle.properties when present
  if [[ -f "$ROOT/gradle.properties" ]]; then
    ver="$(sed -n 's/^desktopVersion=//p' "$ROOT/gradle.properties" | head -1 | tr -d '\r')"
    JAR="$ROOT/desktop-standalone/engine/build/libs/CFHC-desktop-${ver}.jar"
  fi
fi
if [[ ! -f "$JAR" ]]; then
  # Fallback for older artifact names during transition
  JAR="$ROOT/desktop-standalone/engine/build/libs/CFHC-desktop-prototype.jar"
fi
if ! command -v xvfb-run >/dev/null 2>&1; then
  echo "xvfb-run not found; skipping desktop GUI smoke"
  exit 0
fi
echo "Launching desktop.Main new under xvfb (8s timeout)..."
timeout 8 xvfb-run -a java -jar "$JAR" new </dev/null >/tmp/cfhc-desktop-smoke.log 2>&1 || {
  code=$?
  if [[ "$code" -eq 124 ]]; then
    echo "Desktop process stayed responsive through timeout (expected)."
    exit 0
  fi
  cat /tmp/cfhc-desktop-smoke.log
  exit "$code"
}
