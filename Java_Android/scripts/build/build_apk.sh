#!/bin/bash
# ==============================================================================
# Copyright (C) 2026 letrthong@gmail.com
# Created & Maintained by: letrthong@gmail.com
# Refactored: Modular functional architecture for AOSP build & deploy
# Licensed under the Apache License, Version 2.0
# ==============================================================================

set -e

# --- Constants & Global Defaults ---
CONFIG_FILE_NAME=".config"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/$CONFIG_FILE_NAME"

# Logging helpers with colors
log_info()    { echo -e "\033[1;34m[INFO]\033[0m $*"; }
log_success() { echo -e "\033[1;32m[SUCCESS]\033[0m $*"; }
log_warn()    { echo -e "\033[1;33m[WARN]\033[0m $*"; }
log_error()   { echo -e "\033[1;31m[ERROR]\033[0m $*"; }

print_help() {
    cat <<EOF
Usage: $0 [OPTIONS]

Options:
  --info                      Display target, build config, and ADB device info
  --start-deploy true|false   Override ENABLE_DEPLOY from $CONFIG_FILE_NAME
  --test-mode true|false     Run unit test suite instead of full build
  -h, --help, help           Display this help message and exit

Configuration:
  Settings are loaded from: $CONFIG_FILE
EOF
}

# --- 1. Parse Command Line Arguments ---
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --info)
                SHOW_INFO="true"
                shift
                ;;
            --start-deploy)
                if [[ "$2" != "true" && "$2" != "false" ]]; then
                    log_error "--start-deploy requires 'true' or 'false', got '$2'"
                    exit 1
                fi
                ENABLE_DEPLOY="$2"
                shift 2
                ;;
            --test-mode)
                if [[ "$2" != "true" && "$2" != "false" ]]; then
                    log_error "--test-mode requires 'true' or 'false', got '$2'"
                    exit 1
                fi
                TEST_MODE="$2"
                shift 2
                ;;
            -h|--help|help)
                print_help
                exit 0
                ;;
            *)
                log_error "Unknown argument '$1'"
                print_help
                exit 1
                ;;
        esac
    done
}

# --- 1b. Diagnostic Info (--info) ---
show_info() {
    resolve_android_paths >/dev/null 2>&1 || true

    echo -e "\n\033[1;36m=== AOSP BUILD & TARGET INFO ===\033[0m"
    echo "  Project:      ${ACTIVE_PROJECT:-default}"
    echo "  Target Lunch: ${CONFIG_TARGET_PRODUCT}-${CONFIG_TARGET_BUILD_VARIANT}"
    echo "  Module:       $SOURCE_CODE_RELATIVE_PATH"
    echo "  APK Output:   $APK_OUTPUT_RELATIVE_PATH/$APK_FILE_NAME"
    echo "  Deploy Path:  $APK_DEPLOY_PATH"
    echo "  AOSP Top:     ${ANDROID_TOP:-Not found}"

    if command -v adb >/dev/null 2>&1; then
        local dev_state build_type internal_id vendor_build_type
        dev_state=$(adb get-state 2>/dev/null || echo "offline")
        build_type=$(adb shell getprop ro.build.type 2>/dev/null | tr -d '\r')
        internal_id=$(adb shell getprop ro.build.internal.id 2>/dev/null | tr -d '\r')
        vendor_build_type=$(adb shell getprop ro.vendor.build.type 2>/dev/null | tr -d '\r')
        echo "  ADB Device:   $dev_state (Build: ${build_type:-unknown})"
        echo "  Internal ID:  ${internal_id:-unknown}"
        echo "  Vendor Build: ${vendor_build_type:-unknown}"
    else
        echo "  ADB Binary:   Not installed"
    fi
    echo -e "\033[1;36m================================\033[0m\n"
    exit 0
}

# --- 2. Load & Validate Configuration ---
load_and_validate_config() {
    if [ ! -f "$CONFIG_FILE" ]; then
        log_error "Configuration file '$CONFIG_FILE_NAME' not found in $SCRIPT_DIR!"
        log_error "Please create $CONFIG_FILE_NAME with required variables."
        exit 1
    fi

    # Load configuration
    source "$CONFIG_FILE"

    local missing_vars=0
    local required_vars=(
        "SOURCE_CODE_RELATIVE_PATH"
        "APK_OUTPUT_RELATIVE_PATH"
        "APK_FILE_NAME"
        "APK_DEPLOY_PATH"
    )

    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            log_error "Missing required variable '$var' in $CONFIG_FILE_NAME"
            missing_vars=1
        fi
    done

    if [ "$missing_vars" -eq 1 ]; then
        log_error "Please update your $CONFIG_FILE_NAME file to include all required fields."
        exit 1
    fi
}

# --- 3. Run Test Mode ---
run_tests() {
    if [ "$TEST_MODE" != "true" ]; then
        return 0
    fi

    log_info "Running in Test Mode..."
    if [ -z "$TEST_RELATIVE_PATH" ] || [ -z "$TEST_BUILD_SCRIPT" ]; then
        log_error "Missing 'TEST_RELATIVE_PATH' or 'TEST_BUILD_SCRIPT' in $CONFIG_FILE_NAME"
        exit 1
    fi

    local test_dir="$SCRIPT_DIR/$TEST_RELATIVE_PATH"
    if [ ! -d "$test_dir" ]; then
        log_error "Test directory '$test_dir' does not exist"
        exit 1
    fi

    cd "$test_dir"
    if ! "./$TEST_BUILD_SCRIPT"; then
        log_error "Test build script '$TEST_BUILD_SCRIPT' failed"
        exit 1
    fi

    log_success "Tests completed successfully."
    exit 0
}

# --- 4. Resolve Android AOSP Top Directory ---
resolve_android_paths() {
    local current_dir="$SCRIPT_DIR"
    local android_dir=""

    while [ "$current_dir" != "/" ] && [ "$current_dir" != "." ]; do
        if [ "$(basename "$current_dir")" = "android" ]; then
            android_dir="$current_dir"
            break
        fi
        current_dir="$(dirname "$current_dir")"
    done

    if [ -z "$android_dir" ]; then
        log_error "Could not find the 'android' root directory in ancestor paths!"
        exit 1
    fi

    ROOT_DIR="$(dirname "$android_dir")"
    ANDROID_TOP="$ROOT_DIR/android/qssi"

    SOURCE_DIR="$ANDROID_TOP/$SOURCE_CODE_RELATIVE_PATH"
    DIR_OUT="$ANDROID_TOP/$APK_OUTPUT_RELATIVE_PATH"
    APK_OUT="$DIR_OUT/$APK_FILE_NAME"

    log_info "ROOT_DIR:    $ROOT_DIR"
    log_info "ANDROID_TOP: $ANDROID_TOP"
    log_info "SOURCE_DIR:  $SOURCE_DIR"
}

# --- 5. Setup AOSP Build Environment ---
setup_build_environment() {
    cd "$ANDROID_TOP"

    if [[ -n "$TARGET_PRODUCT" && -n "$TARGET_BUILD_VARIANT" ]]; then
        log_info "Build environment is already active:"
        log_info "TARGET_PRODUCT = $TARGET_PRODUCT, TARGET_BUILD_VARIANT = $TARGET_BUILD_VARIANT"
        return 0
    fi

    if [ ! -f "build/envsetup.sh" ]; then
        log_error "Cannot find 'build/envsetup.sh' in $ANDROID_TOP"
        exit 1
    fi

    log_info "Initializing environment: source build/envsetup.sh"
    source build/envsetup.sh

    local target="${CONFIG_TARGET_PRODUCT}-${CONFIG_TARGET_BUILD_VARIANT}"
    log_info "Running lunch target: $target"
    lunch "$target"

    if [[ -z "$TARGET_PRODUCT" || -z "$TARGET_BUILD_VARIANT" ]]; then
        log_error "Failed to initialize target environment variables after lunch."
        exit 1
    fi

    log_success "Environment ready: $TARGET_PRODUCT-$TARGET_BUILD_VARIANT"
}

# --- 6. Compile APK Module ---
build_apk() {
    log_info "Cleaning previous build output: $DIR_OUT"
    rm -rf "$DIR_OUT"

    log_info "Navigating to module source directory: $SOURCE_DIR"
    if [ ! -d "$SOURCE_DIR" ]; then
        log_error "Source directory does not exist: $SOURCE_DIR"
        exit 1
    fi

    cd "$SOURCE_DIR"
    log_info "Compiling module with mm -j15..."
    mm -j15

    if [ ! -f "$APK_OUT" ]; then
        log_error "APK not found at $APK_OUT after build completed."
        exit 1
    fi

    cp -fv "$APK_OUT" "$SCRIPT_DIR/"
    log_success "Build successful!"
    log_success "APK copied to: $SCRIPT_DIR/$APK_FILE_NAME"
}

# --- 7. Device Wait Utility ---
wait_for_device() {
    local timeout_sec="${1:-180}"
    local elapsed=0

    log_info "Waiting for device (timeout: ${timeout_sec}s)..."
    while [ "$elapsed" -lt "$timeout_sec" ]; do
        if adb get-state 2>/dev/null | grep -q "device"; then
            log_info "Device is online."
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done

    log_error "Timed out waiting for device after ${timeout_sec}s"
    return 1
}

# Block until the device drops off adb after 'adb reboot', so wait_for_device
# does not return early on the still-running old session.
wait_for_disconnect() {
    # Older platform-tools lack wait-for-disconnect: fall back to a short sleep.
    if ! timeout 30 adb wait-for-disconnect 2>/dev/null; then
        sleep 3
    fi
}

# --- 8. Ensure ADB Remount (Handles first-time reboot requirement) ---
# NOTE:
# 1. Device MUST run a 'userdebug' or 'eng' build image. Production 'user' builds
#    do not support 'adb root' or 'adb remount'.
# 2. First-time remount setup (dynamic partition / overlayfs) requires an automatic
#    reboot and may take ~3-5 mins before the system partition becomes writable.
# 3. Subsequent remounts will complete instantly in 1-2 seconds.
ensure_adb_remount() {
    log_info "Acquiring root access (adb root)..."
    adb root
    wait_for_device 30

    # Verify userdebug / eng build variant
    local build_type
    build_type=$(adb shell getprop ro.build.type 2>/dev/null | tr -d '\r')
    log_info "Detected device build variant: '${build_type:-unknown}'"
    if [ "$build_type" = "user" ]; then
        log_error "Device is running a 'user' build! 'adb remount' is forbidden on user builds."
        log_error "Please flash a 'userdebug' or 'eng' build image to enable deployment."
        exit 1
    fi

    log_info "Checking filesystem write permissions (adb remount)..."
    local remount_output
    remount_output=$(adb remount 2>&1 || true)
    echo "$remount_output"

    # Check if first-time remount setup requires a reboot
    if echo "$remount_output" | grep -qiE "reboot|scratch"; then
        log_warn "First-time remount detected (overlayfs/scratch setup required)."
        log_info "Rebooting device now (this first-time initialization takes ~3-5 minutes)..."
        adb reboot
        wait_for_disconnect
        if ! wait_for_device 300; then
            log_error "Device did not come back online after remount reboot. Aborting."
            exit 1
        fi

        log_info "Device back online. Re-acquiring root and completing remount..."
        adb root
        wait_for_device 30
        local second_remount
        second_remount=$(adb remount 2>&1 || true)
        echo "$second_remount"
        if echo "$second_remount" | grep -qi "remount failed"; then
            log_error "adb remount failed after reboot. Aborting."
            exit 1
        fi
        log_success "First-time remount setup complete."
    else
        log_success "Filesystem remounted quickly (1-2s)."
    fi
}


# --- 9. Deploy APK to Device ---
deploy_apk() {
    if [ "$ENABLE_DEPLOY" != "true" ]; then
        log_info "ENABLE_DEPLOY is not 'true', skipping device deployment."
        return 0
    fi

    echo ""
    log_info "============================================"
    log_info " Starting Deployment"
    log_info "============================================"

    if ! command -v adb >/dev/null 2>&1; then
        log_error "'adb' not found. Ensure Android platform-tools is in PATH."
        exit 1
    fi

    if ! wait_for_device 180; then
        log_error "Device did not become available before deployment. Aborting."
        exit 1
    fi

    ensure_adb_remount

    log_info "Pushing $APK_FILE_NAME to $APK_DEPLOY_PATH/$APK_FILE_NAME"
    adb push "$SCRIPT_DIR/$APK_FILE_NAME" "$APK_DEPLOY_PATH/$APK_FILE_NAME"
    adb shell chmod 644 "$APK_DEPLOY_PATH/$APK_FILE_NAME"
    adb shell sync

    log_info "Rebooting device to apply changes..."
    adb reboot
    wait_for_disconnect

    log_info "Waiting for device to come back online after reboot (approx 2-3 mins)..."
    if ! wait_for_device 300; then
        log_error "Device did not come back online after reboot. Aborting."
        exit 1
    fi

    adb devices
    log_success "Deployment completed successfully."
}

# --- 10. Logcat Capture ---
capture_logs() {
    if [ "$ENABLE_DEPLOY" != "true" ] || [ "$ENABLE_LOGCAT" != "true" ]; then
        return 0
    fi

    local log_file="$SCRIPT_DIR/deployment_log.txt"
    log_info "Capturing logcat filtered by 'AUDIO' to $log_file"
    adb logcat -d | grep -E "AUDIO" > "$log_file" || true
    log_success "Deployment log saved to $log_file"
}

# --- Main Entry Point ---
main() {
    load_and_validate_config
    parse_arguments "$@"

    if [ "$SHOW_INFO" = "true" ]; then
        show_info
    fi

    # Test scripts need ANDROID_BUILD_TOP, so set up the build env before them.
    resolve_android_paths
    setup_build_environment

    run_tests

    build_apk
    deploy_apk
    capture_logs

    log_success "All tasks completed successfully."
    exit 0
}

main "$@"