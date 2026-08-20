# Usage Guide — Logcat Toolset

> This guide explains how to install, configure, and use the logcat capture & filter toolset.
> **Note:** The current toolset only runs on **Windows** (because it uses `pywin32` and `.lnk`).

---

## 1. System Requirements

| Component | Requirement |
|-----------|-------------|
| OS | Windows (7/10/11) |
| ADB | Android SDK Platform-Tools (in PATH) |
| Python | Python 3.x |
| Library | `pywin32` |

---

## 2. Installation

### 2.1 Install ADB
1. Download **Android SDK Platform-Tools** from the Android website.
2. Extract it and add the folder containing `adb.exe` to **PATH**:
   - Open *System Properties → Environment Variables*.
   - Add the path to the `Path` variable.

### 2.2 Install Python & pywin32
Open **Command Prompt (CMD)** and run:

```bat
pip install pywin32
```

> If you don't have Python yet, download it from [python.org](https://www.python.org/) and remember to tick **"Add Python to PATH"** during installation.

---

## 3. Configuring the filter (`filter_logcat.config`)

This file determines **which log lines** will be displayed and written to the filter file.

### 3.1 Structure

```json
[
  {
    "Description": "group name",
    "Enable": true,
    "filters": [
      { "keyword": "crash", "Enable": true },
      { "keyword": "failed", "Enable": true }
    ]
  }
]
```

### 3.2 Enabling / disabling

- **Enable a whole group:** set `"Enable": true` at the group level.
- **Enable/disable individual keywords:** change `"Enable"` in each `filters` item.
- **Plain string keywords** (`"crash"`) are enabled by default.

### 3.3 Using the `*` wildcard

| Keyword | Meaning |
|---------|---------|
| `crash` | Matches any line **containing** "crash" |
| `crash*` | Matches lines **starting with** "crash" |
| `*fatal*` | Matches lines **containing** "fatal" anywhere |
| `CAR.AUDIO.*` | Matches any line starting with "CAR.AUDIO." |

### 3.4 Complete configuration example

```json
[
  {
    "Description": "System crash",
    "Enable": true,
    "filters": [
      { "keyword": "crash", "Enable": true },
      { "keyword": "failed", "Enable": true },
      { "keyword": "exception", "Enable": true },
      { "keyword": "fatal", "Enable": true }
    ]
  },
  {
    "Description": "Car Audio",
    "Enable": true,
    "filters": [
      { "keyword": "CAR.AUDIO.*", "Enable": true },
      { "keyword": "AudioService", "Enable": false }
    ]
  }
]
```

> ⚠️ **Tip:** If you enable too many keywords, the log will be very noisy. Enable only a few specific keywords to keep it easy to follow.

---

## 4. How to use

### Step 1: Connect the device
- Enable **USB Debugging** on the Android device.
- Plug the device into the computer.
- Verify the connection:

```bat
adb devices
```

The device should appear with status `device`.

### Step 2: Run `logcat.bat` (capture log)

Open **Command Prompt** in the `logcat/` folder and run:

```bat
logcat.bat
```

Or pass a username to name the file:

```bat
logcat.bat etr1hc
```

Result: log is written to `logs\log_<user>_<timestamp>.log`.

> The script automatically waits for the device, clears the old buffer, and captures log. When the device reboots/disconnects, it creates a new file automatically.

### Step 3: Run `monitor_log.bat` (filter & view log)

Open **another CMD window** and run:

```bat
monitor_log.bat
```

Or directly:

```bat
python monitor_log.py
```

Result:
- Lines matching the keywords are **colorized** and printed to the screen.
- They are also written to `filter_<log name>` in the same folder as the original log.

### Step 3b: Analyze an old log file (optional)

You can also pass a **log file path** to `monitor_log.py` to re-analyze an old log file (no real-time monitoring):

```bat
python monitor_log.py logs\log_etr1hc_20260820_123456.log
```

Result:
- Reads the whole old log file, filters lines matching the keywords.
- Prints matching lines (colorized) to the screen.
- Writes them to `filter_<log name>` (overwrites the file).
- Shows the total number of matching lines when done.

> If no file path is passed, the tool runs in real-time monitoring mode (via `latest_log.lnk`).

### Step 4: Stop

- Press `Ctrl+C` in the `monitor_log` window to stop monitoring.
- Press `Ctrl+C` in the `logcat.bat` window to stop capturing.

---

## 5. Log color table

| Level | Color | Meaning |
|-------|-------|---------|
| `V` | Gray | Verbose |
| `D` | Cyan | Debug |
| `I` | Green | Info |
| `W` | Yellow | Warning |
| `E` | Red | Error |
| `F` | Red bg / white text | Fatal |

---

## 6. Generated files

| File | Content |
|------|---------|
| `logs\log_<user>_<timestamp>.log` | Full original log (all lines) |
| `filter_<log name>.log` | Only lines matching keywords (created by monitor) |
| `latest_log.lnk` | Shortcut pointing to the current log file (auto-updated) |

---

## 7. Troubleshooting

| Problem | Solution |
|---------|----------|
| `'adb' command not found` | Install Platform-Tools and add it to PATH |
| `Missing 'pywin32' library` | Run `pip install pywin32` |
| No log lines are shown | Check `filter_logcat.config` — is any group `Enable: true`? |
| `latest_log.lnk` does not appear | Run `logcat.bat` first, wait a few seconds |
| Device not connecting | Check USB Debugging, `adb devices` |

---

## 8. Quick tips

- **Run 2 CMD windows:** one for `logcat.bat`, one for `monitor_log.bat`.
- **Filter by module:** add keywords like `CAR.AUDIO.*`, `AudioService`, `BluetoothService`...
- **Capture log after reboot:** just leave `logcat.bat` running; it creates a new file automatically when the device comes back up.
