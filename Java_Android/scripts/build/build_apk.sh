#!/bin/bash
# ==============================================================================
# Copyright (C) 2026 letrthong@gmail.com
# Created & Maintained by: letrthong@gmail.com
# Generated & Refactored by: Gemini 3.8 Pro (Google DeepMind)
# Licensed under the Apache License, Version 2.0
# ==============================================================================

set -e

# --- Constants & Global Defaults ---
CONFIG_FILE_NAME="apk.config"
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
  --no-reboot                 Fast restart (stop && start) instead of full device reboot
  --softlink <path>           Symlink this script + copy $CONFIG_FILE_NAME into <path> (overwrites)
  --repo-sync                 Run 'repo sync' in ROOT_DIR, cd to android/qssi & run post_sync_cli before build
  --repo-sync-only            Run 'repo sync' and post_sync_cli, then exit without building
  --repo-reset                Run 'repo forall -c "git reset --hard && git clean -xdf"' before repo sync
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
            --no-reboot)
                NO_REBOOT="true"
                shift
                ;;
            --softlink)
                if [ -z "$2" ]; then
                    log_error "--softlink requires a target directory path"
                    exit 1
                fi
                SOFTLINK_PATH="$2"
                shift 2
                ;;
            --repo-sync)
                REPO_SYNC="true"
                shift
                ;;
            --repo-sync-only)
                REPO_SYNC="true"
                REPO_SYNC_ONLY="true"
                shift
                ;;
            --repo-reset)
                ENABLE_REPO_RESET="true"
                shift
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

# --- 1a. Install into a project directory (--softlink <path>) ---
# Symlinks this script (fixes to the original reach every project) and copies
# apk.config (each project keeps its own settings). Existing files are overwritten.
# NOTE: the script finds apk.config and the 'android' root relative to the
# link's location, so <path> must be inside the target AOSP tree.
install_to_project() {
    local target_dir="$1"
    local real_script real_target

    if [ ! -d "$target_dir" ]; then
        log_error "Target directory does not exist: $target_dir"
        exit 1
    fi

    # Resolve the ORIGINAL script, even if this run was started via a symlink.
    real_script="$(readlink -f "${BASH_SOURCE[0]}")"
    real_target="$(cd "$target_dir" && pwd -P)"

    if [ "$real_target" = "$(dirname "$real_script")" ] \
        || [ "$real_target" = "$(cd "$SCRIPT_DIR" && pwd -P)" ]; then
        log_error "Target is the script's own directory: $real_target"
        exit 1
    fi

    case "$real_target/" in
        */android/*) ;;
        *) log_warn "'$real_target' is not under an 'android' directory; the build will not find the AOSP tree from there." ;;
    esac

    ln -sfn "$real_script" "$real_target/build_apk.sh"
    # rm first: if the old config is a symlink, 'cp' would write through it.
    rm -f "$real_target/$CONFIG_FILE_NAME"
    cp "$CONFIG_FILE" "$real_target/$CONFIG_FILE_NAME"

    log_success "Linked:  $real_target/build_apk.sh -> $real_script"
    log_success "Copied:  $CONFIG_FILE -> $real_target/$CONFIG_FILE_NAME"
    log_info "Next: edit $real_target/$CONFIG_FILE_NAME, then run: cd \"$real_target\" && ./build_apk.sh --info"
}

# --- 1b. Diagnostic Info (--info) ---
show_info() {
    resolve_android_paths >/dev/null 2>&1 || true

    local module_name="${MODULE_NAME:-${APK_FILE_NAME%.apk}}"
    local jobs="${BUILD_JOBS:-$(nproc 2>/dev/null || echo 8)}"
    echo -e "\n\033[1;36m=== AOSP BUILD & TARGET INFO ===\033[0m"
    echo "  Project:          ${ACTIVE_PROJECT:-default}"
    echo "  Target Lunch:     ${CONFIG_TARGET_PRODUCT}-${CONFIG_TARGET_BUILD_VARIANT}"
    echo "  Module Build Jobs:-j$jobs (m $module_name)"
    echo "  Module Name:      $module_name"
    echo "  Source Path:      $SOURCE_CODE_RELATIVE_PATH"
    echo "  APK Output:       $APK_OUTPUT_RELATIVE_PATH/$APK_FILE_NAME"
    echo "  Deploy Path:      $APK_DEPLOY_PATH"
    echo "  Logcat Filter:    ${LOGCAT_FILTER:-None (full)}"
    echo "  Debug Tags:       ${DEBUG_LOG_TAGS:-None} (Enabled: ${ENABLE_DEBUG_LOG:-false})"
    echo "  Root Dir:         ${ROOT_DIR:-Not found}"
    echo "  AOSP Top:         ${ANDROID_TOP:-Not found (no 'android' dir above $SCRIPT_DIR)}"
    echo "  Repo Sync Jobs:   ${REPO_SYNC_JOBS:-4}"
    echo "  Repo Reset:       ${REPO_ENABLE_RESET:-${ENABLE_REPO_RESET:-false}}"
    echo "  Repo Post Root:   ${REPO_POST_SYNC_ROOT_COMMAND:-${POST_SYNC_ROOT_COMMAND:-None}}"
    echo "  Repo Post Top:    ${REPO_POST_SYNC_BUILD_COMMAND:-${POST_SYNC_BUILD_COMMAND:-None}}"

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
        # 'return' (not 'exit') so callers like show_info can recover.
        return 1
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

# --- 4b. Post-Sync CLI Hook 1: ROOT_DIR (BEFORE entering android/qssi) ---
# Customize this function to run CLI commands at ROOT_DIR right after repo sync finishes.
# (e.g. BSP scripts, vendor setup, git status across repos)
post_sync_root_cli() {
    log_info "============================================================"
    log_info " Running Post-Sync Hook 1: ROOT_DIR (Before android/qssi)"
    log_info " Current Directory: $(pwd)"
    log_info "============================================================"

    local root_cmd="${REPO_POST_SYNC_ROOT_COMMAND:-$POST_SYNC_ROOT_COMMAND}"
    if [ -n "$root_cmd" ]; then
        log_info "Executing root command from config: $root_cmd"
        local r_start
        r_start=$(date +%s)
        eval "$root_cmd"
        local r_end
        r_end=$(date +%s)
        local r_dur=$((r_end - r_start))
        log_success "Root command completed in $((r_dur / 60))m $((r_dur % 60))s."
    else
        log_info "No REPO_POST_SYNC_ROOT_COMMAND set in $CONFIG_FILE_NAME."
        log_info "Add custom root commands directly in post_sync_root_cli() or config."
        # Example commands in ROOT_DIR:
        #   ./scripts/setup_bsp.sh
        #   git status -s
    fi

    log_success "Post-sync ROOT_DIR hook completed."
}

# --- 4c. Post-Sync CLI Hook 2: ANDROID_TOP (AFTER entering android/qssi) ---
# Customize this function to run CLI commands after entering $ROOT_DIR/android/qssi ($ANDROID_TOP).
# (e.g. full project rebuild, lunch, target compile)
post_sync_cli() {
    log_info "============================================"
    log_info " Running Post-Sync Custom CLI Hook"
    log_info " Current Directory: $(pwd)"
    log_info "============================================"

    # --------------------------------------------------------------------------
    # 1. FULL PROJECT REBUILD (CẬP NHẬT CLI TẠI ĐÂY)
    # Nếu đã cấu hình REPO_POST_SYNC_BUILD_COMMAND trong apk.config thì tự động chạy:
    # --------------------------------------------------------------------------
    local build_cmd="${REPO_POST_SYNC_BUILD_COMMAND:-$POST_SYNC_BUILD_COMMAND}"
    if [ -n "$build_cmd" ]; then
        log_info "Executing full project rebuild command from config:"
        log_info ">> $build_cmd"
        local b_start
        b_start=$(date +%s)
        eval "$build_cmd"
        local b_end
        b_end=$(date +%s)
        local b_dur=$((b_end - b_start))
        log_success "Full project rebuild completed in $((b_dur / 60))m $((b_dur % 60))s."
    else
        log_info "No REPO_POST_SYNC_BUILD_COMMAND set in $CONFIG_FILE_NAME."
        log_info "You can configure REPO_POST_SYNC_BUILD_COMMAND in $CONFIG_FILE_NAME"
        log_info "or insert your custom build commands directly below in this function."
    fi

    # --------------------------------------------------------------------------
    # 2. CUSTOM POST-SYNC CLI COMMANDS (THÊM LỆNH TÙY CHỌN TẠI ĐÂY):
    # Ví dụ khi cần cập nhật sau này:
    #   source build/envsetup.sh
    #   lunch ${CONFIG_TARGET_PRODUCT}-${CONFIG_TARGET_BUILD_VARIANT}
    #   m -j$(nproc)
    #   git status -s
    # --------------------------------------------------------------------------

    log_success "Post-sync CLI hook completed."
}

# --- 4c. Repo Sync Utility ---
run_repo_sync() {
    if [ "$REPO_SYNC" != "true" ]; then
        return 0
    fi

    log_info "============================================"
    log_info " Starting Repo Sync in ROOT_DIR"
    log_info "============================================"

    if ! command -v repo >/dev/null 2>&1; then
        log_error "'repo' binary not found in PATH! Please ensure Google repo is installed and accessible."
        exit 1
    fi

    if [ ! -d "$ROOT_DIR" ]; then
        log_error "ROOT_DIR '$ROOT_DIR' does not exist!"
        exit 1
    fi

    log_info "Navigating to ROOT_DIR: $ROOT_DIR"
    cd "$ROOT_DIR"

    if [ ! -d ".repo" ]; then
        log_warn "No '.repo' directory found in ROOT_DIR ($ROOT_DIR). Attempting repo sync anyway..."
    fi

    # Optional hard reset across all git repositories
    local enable_reset="${REPO_ENABLE_RESET:-${ENABLE_REPO_RESET:-false}}"
    if [ "$enable_reset" = "true" ]; then
        log_warn "============================================================"
        log_warn " CAUTION: Hard resetting and cleaning all repos (repo forall)!"
        log_warn " Running: repo forall -c \"git reset --hard && git clean -xdf\""
        log_warn " All uncommitted changes and untracked files will be lost."
        log_warn "============================================================"
        if ! repo forall -c "git reset --hard && git clean -xdf"; then
            log_error "'repo forall' git reset failed in $ROOT_DIR!"
            exit 1
        fi
        log_success "All repositories reset and cleaned successfully."
    fi

    local jobs="${REPO_SYNC_JOBS:-4}"
    local sync_flags="${REPO_SYNC_FLAGS:--d -c --force-sync --no-clone-bundle --tags}"
    local start_ts
    start_ts=$(date +%s)
    local start_str
    start_str=$(date '+%Y-%m-%d %H:%M:%S')

    log_info "Sync started at: $start_str"
    log_info "Executing: repo sync -j$jobs $sync_flags"
    log_warn "NOTE: 'repo sync' can take a substantial amount of time. Please wait..."

    if ! repo sync -j"$jobs" $sync_flags; then
        log_error "repo sync failed in $ROOT_DIR!"
        exit 1
    fi

    local end_ts
    end_ts=$(date +%s)
    local duration=$((end_ts - start_ts))
    local minutes=$((duration / 60))
    local seconds=$((duration % 60))
    log_success "repo sync completed in ${minutes}m ${seconds}s ($duration seconds)."

    # 1. Run Hook 1 in ROOT_DIR (BEFORE cd to android/qssi)
    post_sync_root_cli

    # 2. Navigate to ANDROID_TOP ($ROOT_DIR/android/qssi)
    log_info "Navigating to: $ANDROID_TOP"
    if [ ! -d "$ANDROID_TOP" ]; then
        log_error "Target directory '$ANDROID_TOP' does not exist after repo sync!"
        exit 1
    fi
    cd "$ANDROID_TOP"
    log_info "Current working directory: $(pwd)"

    # 3. Run Hook 2 in ANDROID_TOP (AFTER cd to android/qssi)
    post_sync_cli

    if [ "$REPO_SYNC_ONLY" = "true" ]; then
        log_success "Repo sync only requested (--repo-sync-only). Exiting without build."
        exit 0
    fi
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
    # 'mm' builds every module under the directory, including tests/androidTest,
    # so build only the APK module (name = APK file name without .apk).
    local module_name="${MODULE_NAME:-${APK_FILE_NAME%.apk}}"
    local jobs="${BUILD_JOBS:-$(nproc 2>/dev/null || echo 8)}"
    log_info "Compiling module '$module_name' with m -j$jobs..."
    m "$module_name" -j"$jobs"

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

# After a full reboot, wait until Android reports boot completed.
# (Only valid after a real reboot: the property is reset at boot, but NOT by
# 'stop'/'start' - see restart_framework.)
wait_for_boot_completed() {
    local timeout_sec="${1:-300}"
    local elapsed=0

    log_info "Waiting for boot to complete (sys.boot_completed=1, timeout: ${timeout_sec}s)..."
    while [ "$elapsed" -lt "$timeout_sec" ]; do
        if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
            log_info "Boot completed after ${elapsed}s."
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done

    log_error "Boot did not complete within ${timeout_sec}s."
    return 1
}

# Restart the Android framework without a full reboot (--no-reboot).
# NOTE: sys.boot_completed usually stays "1" across 'stop'/'start', so it
# cannot detect readiness. Instead, wait for a NEW system_server PID and for
# PackageManager to answer.
restart_framework() {
    local timeout_sec="${1:-90}"
    local elapsed=0
    local old_pid new_pid

    old_pid=$(adb shell pidof system_server 2>/dev/null | tr -d '\r')
    log_info "Fast restart (--no-reboot): restarting Android framework (old system_server PID: ${old_pid:-none})..."
    adb shell stop
    sleep 1
    adb shell start

    log_info "Waiting for Android framework to be ready (timeout: ${timeout_sec}s)..."
    while [ "$elapsed" -lt "$timeout_sec" ]; do
        new_pid=$(adb shell pidof system_server 2>/dev/null | tr -d '\r')
        if [ -n "$new_pid" ] && [ "$new_pid" != "$old_pid" ] \
            && adb shell pm path android 2>/dev/null | grep -q "package:"; then
            log_info "Android framework is up (new system_server PID: $new_pid) after ${elapsed}s."
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done

    log_error "Android framework did not come back within ${timeout_sec}s."
    return 1
}

# --- 8. Ensure ADB Remount (Handles first-time reboot requirement) ---
# NOTE:
# 1. Device MUST run a 'userdebug' or 'eng' build image. Production 'user' builds
#    do not support 'adb root' or 'adb remount'.
# 2. First-time remount setup (dynamic partition / overlayfs) requires an automatic
#    reboot and may take ~3-5 mins before the system partition becomes writable.
# 3. Subsequent remounts will complete instantly in 1-2 seconds.
ensure_adb_remount() {
    # Verify userdebug / eng build variant first (getprop works without root),
    # so 'user' builds get a clear error instead of a failed 'adb root'.
    local build_type
    build_type=$(adb shell getprop ro.build.type 2>/dev/null | tr -d '\r')
    log_info "Detected device build variant: '${build_type:-unknown}'"
    if [ "$build_type" = "user" ]; then
        log_error "Device is running a 'user' build! 'adb remount' is forbidden on user builds."
        log_error "Please flash a 'userdebug' or 'eng' build image to enable deployment."
        exit 1
    fi

    log_info "Acquiring root access (adb root)..."
    acquire_root || exit 1

    log_info "Remounting partitions read-write (adb remount)..."
    local remount_output
    remount_output=$(adb remount 2>&1 || true)
    echo "$remount_output"

    # Trust an actual write test, not adb's message text (it varies by version).
    if is_deploy_path_writable; then
        log_success "Filesystem is writable."
        return 0
    fi

    # Not writable yet: first-time overlayfs/scratch setup needs a reboot.
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
        acquire_root || exit 1
        remount_output=$(adb remount 2>&1 || true)
        echo "$remount_output"
        if is_deploy_path_writable; then
            log_success "First-time remount setup complete. Filesystem is writable."
            return 0
        fi
    fi

    log_error "'$(dirname "$APK_DEPLOY_PATH")' is still read-only after 'adb remount'."
    log_error "See the remount output above (if verity is enabled: 'adb disable-verity' then reboot)."
    exit 1
}

# Wait until adbd actually runs as root (uid 0). 'adb root' restarts adbd, so a
# plain wait_for_device can return on the old, non-root session.
acquire_root() {
    local elapsed=0
    while [ "$elapsed" -lt 30 ]; do
        if [ "$(adb shell id -u 2>/dev/null | tr -d '\r')" = "0" ]; then
            log_info "adbd is running as root."
            return 0
        fi
        adb root >/dev/null 2>&1 || true
        sleep 2
        elapsed=$((elapsed + 2))
    done
    log_error "adbd is not running as root after 30s ('adb root' failed)."
    return 1
}

# Return 0 if the parent directory of APK_DEPLOY_PATH can be written to.
is_deploy_path_writable() {
    local probe
    probe="$(dirname "$APK_DEPLOY_PATH")/.build_apk_rw_test"
    adb shell "touch '$probe' && rm -f '$probe'" >/dev/null 2>&1
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
    # Remove old oat/dex cache if present to ensure updated code runs
    adb shell rm -rf "$APK_DEPLOY_PATH/oat" 2>/dev/null || true
    adb shell sync

    if [ "$ENABLE_DEBUG_LOG" = "true" ] && [ -n "$DEBUG_LOG_TAGS" ]; then
        local log_level="${DEBUG_LOG_LEVEL:-DEBUG}"
        log_info "Setting $log_level log properties for tags: $DEBUG_LOG_TAGS"
        for tag in $DEBUG_LOG_TAGS; do
            adb shell setprop persist.log.tag."$tag" "$log_level"
            adb shell setprop log.tag."$tag" "$log_level"
        done
        log_success "Debug log properties configured."
    fi

    # Clear old logcat buffer before restart so captured logs are 100% fresh
    if [ "$ENABLE_LOGCAT" = "true" ]; then
        log_info "Clearing previous device logcat buffer (adb logcat -c)..."
        adb logcat -c 2>/dev/null || true
    fi

    if [ "$NO_REBOOT" = "true" ]; then
        if ! restart_framework 90; then
            log_error "Fast restart failed. Re-run without --no-reboot for a full reboot."
            exit 1
        fi
    else
        log_info "Rebooting device to apply changes..."
        adb reboot
        wait_for_disconnect

        log_info "Waiting for device to come back online after reboot (approx 2-3 mins)..."
        if ! wait_for_device 300; then
            log_error "Device did not come back online after reboot. Aborting."
            exit 1
        fi

        # adb comes online early in boot; wait until Android reports boot done.
        if ! wait_for_boot_completed 300; then
            log_error "Device did not finish booting after reboot. Aborting."
            exit 1
        fi
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

    # Give the app/service time to start and emit logs after boot/restart.
    local settle="${LOGCAT_SETTLE_SECONDS:-10}"
    log_info "Waiting ${settle}s for the app to start before capturing logcat..."
    sleep "$settle"

    if [ -n "$LOGCAT_FILTER" ]; then
        log_info "Capturing logcat filtered by '$LOGCAT_FILTER' to $log_file"
        adb logcat -d | grep -E "$LOGCAT_FILTER" > "$log_file" || true
    else
        log_info "Capturing full logcat (no filter) to $log_file"
        adb logcat -d > "$log_file" || true
    fi

    local line_count
    line_count=$(wc -l < "$log_file" | tr -d ' ')
    if [ "$line_count" -eq 0 ]; then
        log_warn "Captured log is empty. The app may not have started yet or LOGCAT_FILTER matches nothing."
        log_warn "Try a larger LOGCAT_SETTLE_SECONDS in $CONFIG_FILE_NAME."
    fi
    log_success "Deployment log saved to $log_file ($line_count lines)"
}

# --- Main Entry Point ---
main() {
    load_and_validate_config
    parse_arguments "$@"

    if [ -n "$SOFTLINK_PATH" ]; then
        install_to_project "$SOFTLINK_PATH"
        exit 0
    fi

    if [ "$SHOW_INFO" = "true" ]; then
        show_info
    fi

    # Test scripts need ANDROID_BUILD_TOP, so set up the build env before them.
    resolve_android_paths || exit 1

    # Run repo sync in ROOT_DIR, then cd to android/qssi and run post_sync_cli
    run_repo_sync

    setup_build_environment

    run_tests

    build_apk
    deploy_apk
    capture_logs

    log_success "All tasks completed successfully."
    exit 0
}

main "$@"