@echo off
:: ==============================================================================
:: Copyright (C) 2026 letrthong@gmail.com
:: Created & Maintained by: letrthong@gmail.com
:: Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
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
    for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (
        set mm=%%a
        set dd=%%b
        set yyyy=%%c
    )
    for /f "tokens=1-3 delims=:." %%a in ("%time: =0%") do (
        set hh=%%a
        set min=%%b
        set ss=%%c
    )
    set LOG_FILE=logcat_!yyyy!!mm!!dd!_!hh!!min!!ss!.log
) else (
    set LOG_FILE=%~1
)

:: 4. Start logcat stream to file
echo [3/3] Capturing Logcat logs to file: %LOG_FILE%
echo Press Ctrl+C to stop recording logs.
echo ---------------------------------------------------

adb logcat -v threadtime > "%LOG_FILE%"
