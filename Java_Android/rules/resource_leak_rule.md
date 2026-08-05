<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# Resource Leak Prevention Rules (resource_leak_rule.md)

This document defines mandatory standards for resource lifecycle management, I/O handling, database cursor cleanup, and BroadcastReceiver unregistering in Java/Android.

---

## 1. Core Rules

### Rule 1.1: Mandatory Try-With-Resources for AutoCloseable I/O
Always use the `try-with-resources` statement when opening any `AutoCloseable` or `Closeable` resource (e.g., `InputStream`, `OutputStream`, `BufferedReader`, `FileReader`, `Cursor`).

### Rule 1.2: Database Cursor Cleanup
Always close database `Cursor` objects in a `finally` block or via `try-with-resources` to prevent memory leaks and SQLite database warnings.

### Rule 1.3: Symmetric Lifecycle Unregistering
Any component, listener, or receiver registered in `onResume()` / `onStart()` / `onCreate()` **MUST** be unregistered symmetrically in `onPause()` / `onStop()` / `onDestroy()` (e.g., `BroadcastReceiver`, `SensorEventListener`, `LocationListener`).

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
public void readFile(File file) throws IOException {
    // Danger 1: InputStream not closed on error! Causes file handle leak.
    FileInputStream fis = new FileInputStream(file);
    byte[] data = fis.readAllBytes();
    fis.close(); 
}

public class MyActivity extends Activity {
    private MyReceiver mReceiver = new MyReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Danger 2: Receiver registered in onCreate but never unregistered!
        registerReceiver(mReceiver, new IntentFilter("ACTION_DATA"));
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

```java
public void readFile(File file) throws IOException {
    // Correct: Automatic resource cleanup via try-with-resources
    try (FileInputStream fis = new FileInputStream(file)) {
        byte[] data = fis.readAllBytes();
        processData(data);
    }
}

public class MyActivity extends Activity {
    private final MyReceiver mReceiver = new MyReceiver();

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mReceiver, new IntentFilter("ACTION_DATA"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Correct: Symmetric unregistering to prevent intent receiver leak
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java/Android resource management code:
1. [ ] Are all `Closeable` resources managed with `try-with-resources`? -> **Must be Yes**.
2. [ ] Are database `Cursor` objects safely closed after use? -> **Must be Yes**.
3. [ ] Are all registered `BroadcastReceivers` and `Listeners` unregistered in symmetrical lifecycle methods? -> **Must be Yes**.
