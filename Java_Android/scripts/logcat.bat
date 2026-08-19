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

:: --- PREREQUISITE CHECKS ---

:: 0a. Check if running on Windows
if not "%OS%"=="Windows_NT" (
    echo [ERROR] This script is designed to run on Windows operating systems only.
    pause
    exit /b 1
)

:: 0b. Check if ADB is installed and in PATH
where adb >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 'adb' command not found!
    echo [INFO] ADB is either not installed or not added to your system's PATH environment variable.
    echo [INFO] Please download Android SDK Platform-Tools, extract it, and add the folder to your PATH.
    pause
    exit /b 1
)

:: 0c. Create logs directory if it doesn't exist
if not exist "logs" (
    mkdir "logs"
    echo [INFO] Created 'logs' directory for output files.
)

:: ---------------------------

:CAPTURE_LOOP
echo.
:: 1. Wait for device connection via ADB
echo [1/3] Waiting for ADB device connection...
adb wait-for-device
:: Thêm thời gian chờ 2s để các dịch vụ logcat trên điện thoại khởi động hoàn toàn (đặc biệt khi vừa reboot)
timeout /t 2 /nobreak >nul
echo [INFO] ADB Device connected successfully!

:: 2. Clear previous logcat buffer
echo [2/3] Clearing previous Logcat buffer...
adb logcat -c

:: 3. Generate timestamp and handle UserID
:: Use PowerShell to guarantee a consistent yyyyMMdd_HHmmss format regardless of region
for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format 'yyyyMMdd_HHmmss'"') do set TIMESTAMP=%%I

:: Set User ID based on the first argument passed to the script or Windows Username
if "%~1"=="" (
    :: Nếu không truyền userID, tự động lấy Username của hệ thống Windows hiện tại
    set USER_ID=%USERNAME%
) else (
    :: Nếu có truyền tham số (vd: etr1hc), lấy tham số đó làm userID
    set USER_ID=%~1
)

:: Create the log filename in the 'logs' folder: logs\log_userID_timestamp.log
set LOG_FILE=logs\log_!USER_ID!_!TIMESTAMP!.log

:: 3.5 Create a Windows Shortcut (lnk) acting as a soft-link to the current log
set "ABSOLUTE_LOG_PATH=%cd%\!LOG_FILE!"
powershell -NoProfile -Command "$wshell = New-Object -ComObject WScript.Shell; $shortcut = $wshell.CreateShortcut('latest_log.lnk'); $shortcut.TargetPath = '!ABSOLUTE_LOG_PATH!'; $shortcut.Save()"
echo [INFO] Created 'latest_log.lnk' pointing to the current log file.

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
