# Handler Pattern & Safety Coding Rules (handler_rule.md)

This document defines mandatory coding standards and refactoring rules when working with `Handler` and asynchronous execution patterns in Java/Android.

---

## 1. Core Rule: Avoid Unchecked Chained Invocations (Null Safety & Defensive Programming)

Never invoke methods directly on a returned object if that object can potentially be `null` (such as `Handler.getMain()` or similar factory/singleton getters), as this risks throwing a `NullPointerException`. Always assign the result to a local variable, check for `null`, and then execute the operation safely.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):
Directly calling methods on a potential null-returning getter without verification.
```java
import android.os.Handler;
// Danger: If Handler.getMain() returns null, this throws a NullPointerException
Handler.getMain().executeOrSendMessage(xyz);
```

```java
// Danger: Chained calls without null safety guards
UIController.getInstance().getPrimaryHandler().post(task);
```

### ✅ REQUIRED BEST PRACTICE:
Assign the instance to a local variable, perform an explicit `null` check, and then execute safely.
```java
// Safe execution pattern
Handler handler = Handler.getMain();
if (handler != null) {
    handler.executeOrSendMessage(xyz);
}
```

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

---

## 3. AI Self-Correction & Verification Checklist

Before outputting any Java code involving singleton getters, utility factories, or `Handler` instances, the AI must verify:
1. [ ] Are there any direct chained calls on static getters (e.g., `Class.getSomething().method()`) without a null check?
2. [ ] Is the retrieved instance safely stored in a local variable and checked against `null` before method invocation?
3. [ ] Are all comments and error logs written in professional English?
