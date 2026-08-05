<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# UI Thread Safety & ANR Prevention Rules (ui_thread_rule.md)

This document defines mandatory guidelines to guarantee UI responsiveness and prevent Application Not Responding (ANR) crashes in Android Java development.

---

## 1. Core Rules

### Rule 1.1: Absolute Prohibition of Heavy Operations on Main/UI Thread
Never perform disk I/O, network requests, database queries, bitmap processing, or heavy mathematical computations on the Main (UI) Thread.

### Rule 1.2: Prohibition of Blocking Calls on Main Loop
Never invoke blocking calls (e.g., `Thread.sleep()`, `Future.get()`, `CountDownLatch.await()`, `Object.wait()`) on the Main Thread.

### Rule 1.3: Thread-Safe UI Updates
All UI element modifications (Views, Adapters, Dialogs) **MUST** occur on the Main Thread. If executing from a background thread, dispatch the update back to the UI thread using `runOnUiThread()`, `Handler(Looper.getMainLooper())`, or LiveData/State.

### Rule 1.4: Mandatory AndroidX Thread & Null Safety Annotations
* **Static Analysis Annotations:** Always annotate methods and parameters with AndroidX annotations to enable IDE and Android Lint (`./gradlew lintDebug`) compile-time verification:
  * `@WorkerThread`: Mark all background execution, database, or network methods.
  * `@MainThread` / `@UiThread`: Mark methods that interact with UI components.
  * `@NonNull` / `@Nullable`: Explicitly declare nullability contracts for parameters and return types.
  * `@VisibleForTesting`: Annotate methods/fields whose visibility is relaxed solely for testing purposes.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
public class NetworkActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Danger 1: Network on Main Thread! Throws NetworkOnMainThreadException
        String result = httpGet("https://api.example.com/data");

        // Danger 2: Blocking Main Thread with sleep! Triggers ANR dialog
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // Ignored
        }
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

```java
public class NetworkActivity extends Activity {
    private ExecutorService mExecutor;
    private Handler mMainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExecutor = Executors.newSingleThreadExecutor();
        mMainHandler = new Handler(Looper.getMainLooper());

        // Correct: Asynchronous execution on background thread
        mExecutor.execute(() -> {
            String result = httpGet("https://api.example.com/data");

            // Correct: Dispatch UI update safely back to Main Thread
            mMainHandler.post(() -> updateUi(result));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
        }
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java/Android code handling execution:
1. [ ] Is network or file I/O executed on a background thread (`Executor`, `Worker`, `Coroutine`)? -> **Must be Yes**.
2. [ ] Are there any `Thread.sleep()` or blocking calls on the UI thread? -> **Must be removed**.
3. [ ] Are View updates dispatched on `Looper.getMainLooper()`? -> **Must be Yes**.
