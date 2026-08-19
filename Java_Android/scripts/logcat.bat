@echo off
:: ==============================================================================
:: Copyright (C) 2026 letrthong@gmail.com
:: Created & Maintained by: letrthong@gmail.com
:: Refactored by: Gemini
:: Licensed under the Apache License, Version 2.0
:: ==============================================================================

setlocal enabledelayedexpansion

echo ===================================================
echo  Android ADB Logcat Capture Tool
echo ===================================================

:: 1. Wait for device connection via ADB
echo [1/3] Waiting for ADB device connection...
adb wait-for-device
echo [INFO] ADB Device connected successfully!

:: 2. Clear previous logcat buffer
echo [2/3] Clearing previous Logcat buffer...
adb logcat -c

:: 3. Generate timestamp or use custom filename argument
if "%~1"=="" (
    :: Use PowerShell to guarantee a consistent yyyyMMdd_HHmmss format regardless of region
    for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format 'yyyyMMdd_HHmmss'"') do set TIMESTAMP=%%I
    set LOG_FILE=logcat_!TIMESTAMP!.log
) else (
    set LOG_FILE=%~1
)

:: 4. Start logcat stream to file
echo [3/3] Capturing Logcat logs to file: %LOG_FILE%
echo Press Ctrl+C to stop recording logs.
echo ---------------------------------------------------

adb logcat -v threadtime > "%LOG_FILE%"
