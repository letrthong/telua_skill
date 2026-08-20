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

:: --- PREREQUISITE CHECKS ---

:: 0a. Check if running on Windows
if not "%OS%"=="Windows_NT" (
    echo [ERROR] This script is designed to run on Windows operating systems only.
    pause
    exit /b 1
)

:: 0b. Check if Python is installed and in PATH
where python >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 'python' command not found!
    echo [INFO] Python is either not installed or not added to your system's PATH environment variable.
    echo [INFO] Please download Python from https://www.python.org/ and tick "Add Python to PATH" during installation.
    pause
    exit /b 1
)

:: 0c. Check if pywin32 is installed (required by monitor_log.py)
python -c "import win32com.client" >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Missing 'pywin32' library!
    echo [INFO] Please install it by running:
    echo.
    echo     pip install pywin32
    echo.
    pause
    exit /b 1
)

:: --- USAGE / HELPER ---
:: If no file path is passed, show usage instructions and exit.
if "%~1"=="" (
    echo.
    echo ===================================================
    echo  Usage: analyze_log.bat ^<path_to_log_file^>
    echo ===================================================
    echo.
    echo  This tool re-analyzes an OLD log file (no real-time).
    echo  It filters lines matching the keywords in
    echo  filter_logcat.config, prints them (colorized) and
    echo  writes them to filter_^<log name^>.
    echo.
    echo  Examples:
    echo    analyze_log.bat logs\log_etr1hc_20260820_123456.log
    echo    analyze_log.bat C:\Users\ETR1HC\Downloads\log_2.txt
    echo.
    echo  Tip: Drag and drop a log file onto this .bat to analyze it.
    echo.
    pause
    exit /b 0
)

:: --- ANALYZE ---
echo [INFO] Analyzing log file: %~1
python monitor_log.py "%~1"
pause