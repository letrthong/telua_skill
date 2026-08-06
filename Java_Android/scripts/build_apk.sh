#!/bin/bash
# ==============================================================================
# Copyright (C) 2026 letrthong@gmail.com
# Created & Maintained by: letrthong@gmail.com
# Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
# Licensed under the Apache License, Version 2.0
# ==============================================================================

set -e

# 1. Get the directory where this script is currently located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 2. Check and load configuration from the .config file
CONFIG_FILE="$SCRIPT_DIR/.config"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Configuration file '.config' not found in $SCRIPT_DIR!"
    echo "Please create a .config file with required variables."
    exit 1
fi

source "$CONFIG_FILE"

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