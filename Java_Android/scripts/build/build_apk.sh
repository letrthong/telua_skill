#!/bin/bash
# ==============================================================================
# Copyright (C) 2026 letrthong@gmail.com
# Created & Maintained by: letrthong@gmail.com
# Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
# Licensed under the Apache License, Version 2.0
# ==============================================================================


set -e

CONFIG_FILE_NAME=".config"

print_help() {
    cat <<EOF
Usage: $0 [--start-deploy true|false] [help|-h|--help]

  --start-deploy true|false   Override ENABLE_DEPLOY from $CONFIG_FILE_NAME
  --test-mode true|false     Enable or disable test mode
  help, -h, --help            Show this help message and exit
EOF
}

for arg in "$@"; do
    case "$arg" in
        help|-h|--help)
            print_help
            exit 0
            ;;
    esac
done

# 1. Get the directory where this script is currently located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 2. Check and load configuration from the .config file
CONFIG_FILE="$SCRIPT_DIR/$CONFIG_FILE_NAME"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file '$CONFIG_FILE_NAME' not found in $SCRIPT_DIR!"
    echo "Please create a $CONFIG_FILE_NAME file with required variables."
    exit 1
fi

source "$CONFIG_FILE"

# 2b. Parse CLI arguments (override ENABLE_DEPLOY from .config when provided)
while [[ $# -gt 0 ]]; do
    case "$1" in
        --start-deploy)
            if [[ "$2" != "true" && "$2" != "false" ]]; then
                echo "ERROR: --start-deploy requires a value of 'true' or 'false', got '$2'"
                exit 1
            fi
            ENABLE_DEPLOY="$2"
            shift 2
            ;;
        --test-mode)
            if [[ "$2" != "true" && "$2" != "false" ]]; then
                echo "ERROR: --test-mode requires a value of 'true' or 'false', got '$2'"
                exit 1
            fi
            TEST_MODE="$2"
            shift 2
            ;;
        *)
            echo "ERROR: Unknown argument '$1'"
            print_help
            exit 1
            ;;
    esac
done

# 2c. Run the test build script (path from .config) and exit when test mode is on
if [ "$TEST_MODE" = "true" ]; then
    if [ -z "$TEST_RELATIVE_PATH" ] || [ -z "$TEST_BUILD_SCRIPT" ]; then
        echo "ERROR: Missing 'TEST_RELATIVE_PATH' or 'TEST_BUILD_SCRIPT' in .config"
        exit 1
    fi
    cd "$SCRIPT_DIR/$TEST_RELATIVE_PATH"
    if ! "./$TEST_BUILD_SCRIPT"; then
        echo "ERROR: Test build script '$TEST_BUILD_SCRIPT' failed"
        exit 1
    fi
    exit 0
fi

# 3. Validate required variables from the .config file
MISSING_VARS=0

if [ -z "$SOURCE_CODE_RELATIVE_PATH" ]; then
    echo "ERROR: Missing required variable 'SOURCE_CODE_RELATIVE_PATH' in .config"
    MISSING_VARS=1
fi

if [ -z "$APK_OUTPUT_RELATIVE_PATH" ]; then
    echo "ERROR: Missing required variable 'APK_OUTPUT_RELATIVE_PATH' in .config"
    MISSING_VARS=1
fi

if [ -z "$APK_FILE_NAME" ]; then
    echo "ERROR: Missing required variable 'APK_FILE_NAME' in .config"
    MISSING_VARS=1
fi

if [ -z "$APK_DEPLOY_PATH" ]; then
    echo "ERROR: Missing required variable 'APK_DEPLOY_PATH' in .config"
    MISSING_VARS=1
fi

if [ "$MISSING_VARS" -eq 1 ]; then
    echo "Please update your .config file to include all required fields."
    exit 1
fi

# 4. Automatically find the Android root directory by searching upward for the "android" folder
CURRENT_DIR="$SCRIPT_DIR"
ANDROID_DIR=""

while [ "$CURRENT_DIR" != "/" ]; do
    if [ "$(basename "$CURRENT_DIR")" = "android" ]; then
        ANDROID_DIR="$CURRENT_DIR"
        break
    fi
    CURRENT_DIR="$(dirname "$CURRENT_DIR")"
done

if [ -z "$ANDROID_DIR" ]; then
    echo "ERROR: Could not find the 'android' directory in the path!"
    exit 1
fi

# 5. ROOT_DIR is the parent directory of the "android" folder, and ANDROID_TOP points to qssi
ROOT_DIR="$(dirname "$ANDROID_DIR")"
ANDROID_TOP="$ROOT_DIR/android/qssi"

SOURCE_DIR="$ANDROID_TOP/$SOURCE_CODE_RELATIVE_PATH"
DIR_OUT="$ANDROID_TOP/$APK_OUTPUT_RELATIVE_PATH"
APK_OUT="$DIR_OUT/$APK_FILE_NAME"


echo "ROOT_DIR: $ROOT_DIR"
echo "ANDROID_TOP: $ANDROID_TOP"

# 6. Clean up old build outputs and start building
rm -rfv "$DIR_OUT"

cd "$ANDROID_TOP"
echo "ANDROID_TOP= $ANDROID_TOP"
if [[ -n "$TARGET_PRODUCT" && -n "$TARGET_BUILD_VARIANT" ]]; then
   
    echo "Environment is ready!"
    echo "TARGET_PRODUCT = $TARGET_PRODUCT"
    echo "TARGET_BUILD_VARIANT = $TARGET_BUILD_VARIANT"
else
   
    source build/envsetup.sh
    echo "Log: Running lunch with target: ${CONFIG_TARGET_PRODUCT}-${CONFIG_TARGET_BUILD_VARIANT}"
    lunch "${CONFIG_TARGET_PRODUCT}-${CONFIG_TARGET_BUILD_VARIANT}"
    if [[ -n "$TARGET_PRODUCT" && -n "$TARGET_BUILD_VARIANT" ]]; then
        # If both variables exist, print a success log and their values
        echo "Environment is ready!"
        echo "TARGET_PRODUCT = $TARGET_PRODUCT"
        echo "TARGET_BUILD_VARIANT = $TARGET_BUILD_VARIANT"
    else
        echo "Error: Environment variables do not exist."
        return 1 2>/dev/null || exit 1
    fi
fi


# 7. Navigate to the source code directory and print the current path before building
echo "Navigating to source directory: $SOURCE_DIR"
cd "$SOURCE_DIR"
mm -j15

# 8. Verify that the APK was successfully built
if [ ! -f "$APK_OUT" ]; then
    echo "ERROR: APK not found at $APK_OUT"
    exit 1
fi

# 9. Copy the generated APK back to the script's directory
cp -fv "$APK_OUT" "$SCRIPT_DIR/"

echo ""
echo "============================================"
echo "Build successful!"
echo "APK: $SCRIPT_DIR/$APK_FILE_NAME"
echo "============================================"


# ==============================================================================
# Wait for device with a timeout (default 180 seconds).
# Returns 0 if the device becomes online, 1 on timeout.
# ==============================================================================
wait_for_device() {
    local timeout_sec="${1:-180}"
    local elapsed=0
    echo "Waiting for device (timeout: ${timeout_sec}s)..."
    while [ "$elapsed" -lt "$timeout_sec" ]; do
        if adb get-state 2>/dev/null | grep -q "device"; then
            echo "Device is online."
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    echo "ERROR: Timed out waiting for device after ${timeout_sec}s"
    return 1
}

echo ""
echo "============================================"
echo " Start deployment"
echo "============================================"
if [ "$ENABLE_DEPLOY" != "true" ]; then
    echo "ENABLE_DEPLOY is not 'true', skipping deployment and logcat."
    exit 0
fi

if ! command -v adb >/dev/null 2>&1; then
    echo "ERROR: 'adb' not found. Please install Android platform-tools and ensure adb is in your PATH before deploying."
    exit 1
fi

if ! wait_for_device 180; then
    echo "ERROR: Device did not become available before deployment. Aborting."
    exit 1
fi
adb root
adb remount

adb push "$SCRIPT_DIR/$APK_FILE_NAME"  "$APK_DEPLOY_PATH/$APK_FILE_NAME"
adb shell sync
adb reboot
sleep 1
echo "Deployment completed."

echo "Please wait about 2-3 minutes."
if ! wait_for_device 180; then
    echo "ERROR: Device did not come back online after reboot. Aborting."
    exit 1
fi

adb devices
if [ "$ENABLE_LOGCAT" != "true" ]; then
    echo "ENABLE_LOGCAT is not 'true', skipping logcat."
    exit 0
fi
echo "Start logcat and save to $SCRIPT_DIR/deployment_log.txt"
adb logcat -d | grep -E "AUDIO" > "$SCRIPT_DIR/deployment_log.txt"
echo "Deployment log saved to $SCRIPT_DIR/deployment_log.txt"#!/bin/bash
# ==============================================================================
# Copyright (C) 2026 letrthong@gmail.com
# Created & Maintained by: letrthong@gmail.com
# Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
# Licensed under the Apache License, Version 2.0
# ==============================================================================


set -e

CONFIG_FILE_NAME="oemcarservice_apk.config"

print_help() {
    cat <<EOF
Usage: $0 [--start-deploy true|false] [help|-h|--help]

  --start-deploy true|false   Override ENABLE_DEPLOY from $CONFIG_FILE_NAME
  --test-mode true|false     Enable or disable test mode
  help, -h, --help            Show this help message and exit
EOF
}

for arg in "$@"; do
    case "$arg" in
        help|-h|--help)
            print_help
            exit 0
            ;;
    esac
done

# 1. Get the directory where this script is currently located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 2. Check and load configuration from the .config file
CONFIG_FILE="$SCRIPT_DIR/$CONFIG_FILE_NAME"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file '$CONFIG_FILE_NAME' not found in $SCRIPT_DIR!"
    echo "Please create a $CONFIG_FILE_NAME file with required variables."
    exit 1
fi

source "$CONFIG_FILE"

# 2b. Parse CLI arguments (override ENABLE_DEPLOY from .config when provided)
while [[ $# -gt 0 ]]; do
    case "$1" in
        --start-deploy)
            if [[ "$2" != "true" && "$2" != "false" ]]; then
                echo "ERROR: --start-deploy requires a value of 'true' or 'false', got '$2'"
                exit 1
            fi
            ENABLE_DEPLOY="$2"
            shift 2
            ;;
        --test-mode)
            if [[ "$2" != "true" && "$2" != "false" ]]; then
                echo "ERROR: --test-mode requires a value of 'true' or 'false', got '$2'"
                exit 1
            fi
            TEST_MODE="$2"
            shift 2
            ;;
        *)
            echo "ERROR: Unknown argument '$1'"
            print_help
            exit 1
            ;;
    esac
done

# 2c. Run the test build script (path from .config) and exit when test mode is on
if [ "$TEST_MODE" = "true" ]; then
    if [ -z "$TEST_RELATIVE_PATH" ] || [ -z "$TEST_BUILD_SCRIPT" ]; then
        echo "ERROR: Missing 'TEST_RELATIVE_PATH' or 'TEST_BUILD_SCRIPT' in .config"
        exit 1
    fi
    cd "$SCRIPT_DIR/$TEST_RELATIVE_PATH"
    if ! "./$TEST_BUILD_SCRIPT"; then
        echo "ERROR: Test build script '$TEST_BUILD_SCRIPT' failed"
        exit 1
    fi
    exit 0
fi

# 3. Validate required variables from the .config file
MISSING_VARS=0

if [ -z "$SOURCE_CODE_RELATIVE_PATH" ]; then
    echo "ERROR: Missing required variable 'SOURCE_CODE_RELATIVE_PATH' in .config"
    MISSING_VARS=1
fi

if [ -z "$APK_OUTPUT_RELATIVE_PATH" ]; then
    echo "ERROR: Missing required variable 'APK_OUTPUT_RELATIVE_PATH' in .config"
    MISSING_VARS=1
fi

if [ -z "$APK_FILE_NAME" ]; then
    echo "ERROR: Missing required variable 'APK_FILE_NAME' in .config"
    MISSING_VARS=1
fi

if [ -z "$APK_DEPLOY_PATH" ]; then
    echo "ERROR: Missing required variable 'APK_DEPLOY_PATH' in .config"
    MISSING_VARS=1
fi

if [ "$MISSING_VARS" -eq 1 ]; then
    echo "Please update your .config file to include all required fields."
    exit 1
fi

# 4. Automatically find the Android root directory by searching upward for the "android" folder
CURRENT_DIR="$SCRIPT_DIR"
ANDROID_DIR=""

while [ "$CURRENT_DIR" != "/" ]; do
    if [ "$(basename "$CURRENT_DIR")" = "android" ]; then
        ANDROID_DIR="$CURRENT_DIR"
        break
    fi
    CURRENT_DIR="$(dirname "$CURRENT_DIR")"
done

if [ -z "$ANDROID_DIR" ]; then
    echo "ERROR: Could not find the 'android' directory in the path!"
    exit 1
fi

# 5. ROOT_DIR is the parent directory of the "android" folder, and ANDROID_TOP points to qssi
ROOT_DIR="$(dirname "$ANDROID_DIR")"
ANDROID_TOP="$ROOT_DIR/android/qssi"

SOURCE_DIR="$ANDROID_TOP/$SOURCE_CODE_RELATIVE_PATH"
DIR_OUT="$ANDROID_TOP/$APK_OUTPUT_RELATIVE_PATH"
APK_OUT="$DIR_OUT/$APK_FILE_NAME"


echo "ROOT_DIR: $ROOT_DIR"
echo "ANDROID_TOP: $ANDROID_TOP"

# 6. Clean up old build outputs and start building
rm -rfv "$DIR_OUT"

cd "$ANDROID_TOP"
echo "ANDROID_TOP= $ANDROID_TOP"
if [[ -n "$TARGET_PRODUCT" && -n "$TARGET_BUILD_VARIANT" ]]; then
   
    echo "Environment is ready!"
    echo "TARGET_PRODUCT = $TARGET_PRODUCT"
    echo "TARGET_BUILD_VARIANT = $TARGET_BUILD_VARIANT"
else
   
    source build/envsetup.sh
    echo "Log: Running lunch with target: ${CONFIG_TARGET_PRODUCT}-${CONFIG_TARGET_BUILD_VARIANT}"
    lunch "${CONFIG_TARGET_PRODUCT}-${CONFIG_TARGET_BUILD_VARIANT}"
    if [[ -n "$TARGET_PRODUCT" && -n "$TARGET_BUILD_VARIANT" ]]; then
        # If both variables exist, print a success log and their values
        echo "Environment is ready!"
        echo "TARGET_PRODUCT = $TARGET_PRODUCT"
        echo "TARGET_BUILD_VARIANT = $TARGET_BUILD_VARIANT"
    else
        echo "Error: Environment variables do not exist."
        return 1 2>/dev/null || exit 1
    fi
fi


# 7. Navigate to the source code directory and print the current path before building
echo "Navigating to source directory: $SOURCE_DIR"
cd "$SOURCE_DIR"
mm -j15

# 8. Verify that the APK was successfully built
if [ ! -f "$APK_OUT" ]; then
    echo "ERROR: APK not found at $APK_OUT"
    exit 1
fi

# 9. Copy the generated APK back to the script's directory
cp -fv "$APK_OUT" "$SCRIPT_DIR/"

echo ""
echo "============================================"
echo "Build successful!"
echo "APK: $SCRIPT_DIR/$APK_FILE_NAME"
echo "============================================"


# ==============================================================================
# Wait for device with a timeout (default 180 seconds).
# Returns 0 if the device becomes online, 1 on timeout.
# ==============================================================================
wait_for_device() {
    local timeout_sec="${1:-180}"
    local elapsed=0
    echo "Waiting for device (timeout: ${timeout_sec}s)..."
    while [ "$elapsed" -lt "$timeout_sec" ]; do
        if adb get-state 2>/dev/null | grep -q "device"; then
            echo "Device is online."
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    echo "ERROR: Timed out waiting for device after ${timeout_sec}s"
    return 1
}

echo ""
echo "============================================"
echo " Start deployment"
echo "============================================"
if [ "$ENABLE_DEPLOY" != "true" ]; then
    echo "ENABLE_DEPLOY is not 'true', skipping deployment and logcat."
    exit 0
fi

if ! command -v adb >/dev/null 2>&1; then
    echo "ERROR: 'adb' not found. Please install Android platform-tools and ensure adb is in your PATH before deploying."
    exit 1
fi

if ! wait_for_device 180; then
    echo "ERROR: Device did not become available before deployment. Aborting."
    exit 1
fi
adb root
adb remount

adb push "$SCRIPT_DIR/$APK_FILE_NAME"  "$APK_DEPLOY_PATH/$APK_FILE_NAME"
adb shell sync
adb reboot
sleep 1
echo "Deployment completed."

echo "Please wait about 2-3 minutes."
if ! wait_for_device 180; then
    echo "ERROR: Device did not come back online after reboot. Aborting."
    exit 1
fi

adb devices
if [ "$ENABLE_LOGCAT" != "true" ]; then
    echo "ENABLE_LOGCAT is not 'true', skipping logcat."
    exit 0
fi
echo "Start logcat and save to $SCRIPT_DIR/deployment_log.txt"
adb logcat -d | grep -E "AUDIO" > "$SCRIPT_DIR/deployment_log.txt"
echo "Deployment log saved to $SCRIPT_DIR/deployment_log.txt"
