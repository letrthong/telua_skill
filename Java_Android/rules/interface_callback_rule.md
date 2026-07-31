# Interface and Non-Blocking Callback Rules (interface_callback_rule.md)

This document defines mandatory engineering standards for designing Java interfaces, callback listeners, non-blocking callback dispatching, and error-handling paradigms.

---

## 1. Core Rules

### Rule 1.1: Interface Naming Conventions & Functional Annotation
* **Naming Standards:** 
  * Event Listeners **MUST** use the `On[Event]Listener` prefix (e.g., `OnCarServiceListener`, `OnDataProcessedListener`).
  * Asynchronous Callbacks **MUST** use the `[Feature]Callback` suffix (e.g., `ResultCallback`, `TaskCallback`).
* **Functional Annotation:** Single-method interfaces **MUST** be annotated with `@FunctionalInterface` to support Lambda expressions.

### Rule 1.2: Mandatory Non-Blocking Asynchronous Callback Dispatching
* **No Inline Direct Execution:** Never invoke listener callbacks directly on heavy worker threads or lock-sensitive threads without offloading.
* **Dispatching Strategies:**
  * **Strategy A (AOSP Executor Pattern):** Accept an `@CallbackExecutor Executor executor` parameter and dispatch callbacks via `executor.execute(() -> listener.onSuccess(data))`.
  * **Strategy B (Main Looper Handler Pattern):** Post callbacks back to the Main UI Thread using `mMainHandler.post(() -> listener.onSuccess(data))`.
* **Benefit:** Prevents slow or blocking subscriber implementations from freezing the event publisher thread.

### Rule 1.3: Robust Error Handling & Exception Isolation
* **No Checked Exceptions in Callback Signatures:** Do not declare checked exceptions on callback methods. Include a dedicated `onError(Throwable throwable)` callback method instead.
* **Try-Catch Isolation:** Wrap all callback invocations inside a `try-catch (Throwable t)` block to isolate publisher loops from unexpected runtime exceptions thrown by subscriber implementations.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Direct Blocking Callback Invocation & Unprotected Exception Risk):

```java
// BAD 1: Direct inline callback call on worker thread. If subscriber blocks, worker thread hangs!
// BAD 2: No try-catch isolation. If subscriber throws RuntimeException, publisher loop crashes!
public class HeavyDataPublisher {
    public interface DataListener {
        void onData(String data) throws Exception; // BAD: Checked exception!
    }

    public void publishData(DataListener listener, String data) {
        // BAD: Direct blocking call!
        try {
            listener.onData(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### ✅ BEST PRACTICE (Non-Blocking Executor/Handler Dispatch & Isolated Error Callback):

```java
package com.example.app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Demonstrates non-blocking callback dispatching, executor offloading,
 * and robust exception isolation.
 */
public class NonBlockingPublisherExample {
    private static final String TAG = NonBlockingPublisherExample.class.getSimpleName();

    /**
     * Functional callback interface for receiving asynchronous data events.
     */
    @FunctionalInterface
    public interface OnDataProcessedListener {
        void onDataProcessed(String result);
    }

    /**
     * Callback interface with dedicated error handler.
     */
    public interface TaskCallback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    /**
     * Strategy A: Dispatches callback via user-provided @CallbackExecutor Executor (AOSP Standard).
     */
    public void dispatchViaExecutor(
            Executor executor,
            OnDataProcessedListener listener,
            String data
    ) {
        Objects.requireNonNull(executor, "Executor cannot be null");
        Objects.requireNonNull(listener, "OnDataProcessedListener cannot be null");

        // Offload callback invocation to caller's Executor so publisher thread never blocks
        executor.execute(() -> {
            try {
                listener.onDataProcessed(data);
            } catch (Throwable t) {
                Log.e(TAG, "Subscriber threw unhandled exception during callback execution", t);
            }
        });
    }

    /**
     * Strategy B: Dispatches callback back to Main UI Thread safely using Handler.
     */
    public void dispatchToMainThread(
            Handler mainHandler,
            TaskCallback<String> callback,
            String result
    ) {
        Objects.requireNonNull(mainHandler, "MainHandler cannot be null");
        Objects.requireNonNull(callback, "TaskCallback cannot be null");

        mainHandler.post(() -> {
            try {
                callback.onSuccess(result);
            } catch (Throwable t) {
                Log.e(TAG, "Error in TaskCallback.onSuccess execution", t);
                callback.onError(t);
            }
        });
    }
}
```

---

## 3. AI Self-Correction Checklist

Before emitting Java code with interfaces or callbacks:
1. [ ] Are callbacks dispatched asynchronously via `@CallbackExecutor Executor` or `Handler`? -> **Must be Yes**.
2. [ ] Is callback invocation wrapped inside a `try-catch (Throwable t)` guard? -> **Must be Yes**.
3. [ ] Are single-method interfaces annotated with `@FunctionalInterface`? -> **Must be Yes**.
