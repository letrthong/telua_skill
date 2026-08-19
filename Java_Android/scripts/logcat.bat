@echo off
:: ==============================================================================
:: Copyright (C) 2026 letrthong@gmail.com
:: Created & Maintained by: letrthong@gmail.com
:: Refactored by: Gemini
:: Licensed under the Apache License, Version 2.0
:: ==============================================================================

setlocal enabledelayedexpansion

echo ===================================================
echo  Android ADB Logcat Capture Tool (Auto-Reconnect)
echo ===================================================

:CAPTURE_LOOP
echo.
:: 1. Wait for device connection via ADB
echo [1/3] Waiting for ADB device connection...
adb wait-for-device
echo [INFO] ADB Device connected successfully!

:: 2. Clear previous logcat buffer
echo [2/3] Clearing previous Logcat buffer...
adb logcat -c

:: 3. Generate timestamp
:: Use PowerShell to guarantee a consistent yyyyMMdd_HHmmss format regardless of region
for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format 'yyyyMMdd_HHmmss'"') do set TIMESTAMP=%%I

:: Handle filename to prevent overwriting upon reconnection
if "%~1"=="" (
    set LOG_FILE=logcat_!TIMESTAMP!.log
) else (
    :: If a custom filename argument is provided, append a timestamp to create a new file each time it drops
    set LOG_FILE=%~n1_!TIMESTAMP!%~x1
)

:: 4. Start logcat stream to file
echo [3/3] Capturing Logcat logs to NEW file: !LOG_FILE!
echo Press Ctrl+C to stop recording logs.
echo ---------------------------------------------------

:: The script will block here. When the device disconnects or reboots, this command automatically terminates and execution continues below.
adb logcat -v threadtime > "!LOG_FILE!"

:: 5. Handle Disconnection
echo.
echo [WARNING] Device disconnected! ADB logcat process stopped.
echo [INFO] Preparing to start a new log session...
:: Wait 2 seconds to prevent a rapid loop that could hang the command prompt
timeout /t 2 /nobreak >nul 

:: Loop back to step 1
goto CAPTURE_LOOP
