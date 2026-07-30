# Handler Pattern & Safety Coding Rules (handler_rule.md)

This document defines mandatory coding standards and refactoring rules when working with `Handler` and asynchronous execution patterns in Java/Android.

---

## 1. Core Rules

### Rule 1.1: Avoid Unchecked Chained Invocations (Null Safety & Defensive Programming)
Never invoke methods directly on a returned object if that object can potentially be `null` (such as factory/singleton getters like `UIController.getInstance().getPrimaryHandler()`), as this risks throwing a `NullPointerException`. Always assign the result to a local variable, check for `null`, and then execute the operation safely.

### Rule 1.2: Mandatory Lifecycle Cleanup (Prevent Memory & Callback Leaks)
Whenever an `android.os.Handler` is used to post delayed tasks or messages within a lifecycle-aware component (e.g., `Activity`, `Fragment`, `Service`), you **MUST** clear all pending callbacks and messages when the component reaches the end of its lifecycle (e.g., in `onDestroy()` or `onStop()`).

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

#### 1. Chained Calls without Null Verification
```java
// Danger: Chained calls without null safety guards
UIController.getInstance().getPrimaryHandler().post(task);
```

#### 2. Failing to Remove Pending Callbacks on Destroy
```java
public class MyActivity extends Activity {
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public void startTimer() {
        mHandler.postDelayed(this::updateUI, 5000);
    }
    // Danger: Missing onDestroy() cleanup! Pending runnable can crash UI after onDestroy.
}
```

### ✅ REQUIRED BEST PRACTICE:

#### 1. Safe Null Guard & Local Assignment
```java
// Safe execution pattern with chaining/complex getters
Handler primaryHandler = UIController.getInstance().getPrimaryHandler();
if (primaryHandler != null) {
    primaryHandler.post(task);
} else {
    logger.warning("Primary handler is not initialized, fallback execution triggered.");
    fallbackExecution(task);
}
```

#### 2. Proper Handler Lifecycle Cleanup
```java
public class MyActivity extends Activity {
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public void startTimer() {
        mHandler.postDelayed(this::updateUI, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove all pending messages and callbacks to prevent leaks & crashes
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before outputting any Java code involving `Handler` instances or singleton getters, the AI must verify:
1. [ ] Are there any direct chained calls on getters without a `null` check?
2. [ ] Is the retrieved instance safely stored in a local variable before method invocation?
3. [ ] Are all pending callbacks cleared via `removeCallbacksAndMessages(null)` during teardown/`onDestroy()`?
4. [ ] Are all comments and error logs written in professional English?
