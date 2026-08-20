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

python monitor_log.py
pause