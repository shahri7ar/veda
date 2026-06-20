#!/usr/bin/env bash
# Build the VibeMusic APK locally.
#
# Requirements (all standard tooling — same as Android Studio uses):
#   - JDK 17 (https://adoptium.net/)
#   - Android SDK with platform-android-34 and build-tools 34.0.0
#     (Android Studio installs these by default, or you can use cmdline-tools)
#   - ~4 GB free RAM and ~3 GB free disk for first run (Maven cache)
#
# Usage:
#   ./build-local.sh            # builds debug APK
#   ./build-local.sh release    # builds signed release APK
#
set -euo pipefail

cd "$(dirname "$0")"

VARIANT="${1:-debug}"

if [ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]; then
    echo "ERROR: ANDROID_HOME (or ANDROID_SDK_ROOT) is not set."
    echo "On macOS w/ Android Studio:  export ANDROID_HOME=\$HOME/Library/Android/sdk"
    echo "On Linux w/ Android Studio:  export ANDROID_HOME=\$HOME/Android/Sdk"
    exit 1
fi

if [ ! -f app/vibemusic-release.keystore ] && [ "$VARIANT" = "release" ]; then
    echo "==> Generating self-signed release keystore..."
    keytool -genkeypair \
        -keystore app/vibemusic-release.keystore \
        -alias vibemusic \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass vibemusic -keypass vibemusic \
        -dname "CN=VibeMusic, OU=VibeMusic, O=VibeMusic, L=Anywhere, ST=Anywhere, C=US"
fi

if [ -f gradlew ]; then
    GRADLE_CMD="./gradlew"
    chmod +x gradlew
else
    GRADLE_CMD="gradle"
fi

case "$VARIANT" in
    debug)
        echo "==> Building debug APK..."
        $GRADLE_CMD --no-daemon -x lint assembleDebug
        APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
        ;;
    release)
        echo "==> Building release APK..."
        $GRADLE_CMD --no-daemon -x lint assembleRelease
        APK=$(find app/build/outputs/apk/release -name "*.apk" | head -1)
        ;;
    *)
        echo "Unknown variant: $VARIANT (use 'debug' or 'release')"
        exit 1
        ;;
esac

echo ""
echo "✅ Build successful"
echo "   APK: $(realpath "$APK")"
echo "   Size: $(du -h "$APK" | cut -f1)"
echo ""
echo "Install on a connected device with:"
echo "   adb install -r \"$APK\""
