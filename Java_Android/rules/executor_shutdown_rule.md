<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# AI Instructions: Mandatory Executor Shutdown Rule

## CRITICAL RULE: ALWAYS SHUTDOWN EXECUTORS
Whenever you generate, refactor, or review code that initializes or uses an `Executor` or `ExecutorService` (specifically instances like `Executors.newSingleThreadExecutor()` or similar thread pools), you **MUST** ensure that a corresponding `shutdown()` or `shutdownNow()` call is included when the object or component reaches the end of its lifecycle.

## Why This Matters
Failing to shut down an `ExecutorService` keeps background threads alive, leading to severe memory leaks, lingering background tasks, and application hangs (especially in Android/Java environments).

---

## Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):
Initializing an executor without cleanup in the teardown/destroy method.
```java
public class DataManager {
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    
    // Danger: Missing onDestroy() or cleanup method to shutdown mExecutor!
}
```

### ✅ REQUIRED BEST PRACTICE:

#### Option A: Standard ExecutorService Field
```java
public class DataManager {
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public void onDestroy() {
        if (mExecutor != null) {
            mExecutor.shutdownNow(); // Cancels ongoing tasks immediately when lifecycle ends
            mExecutor = null;
        }
    }
}
```

#### Option B: Generic Executor Interface Field
```java
public class DataManager {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    public void release() {
        if (mExecutor instanceof ExecutorService executorService) {
            executorService.shutdownNow();
        }
    }
}
```

---

## AI Checklist for Code Generation
1. [ ] Did I initialize an `Executor` or `ExecutorService`? -> **Yes**
2. [ ] Did I add the matching `shutdown()` / `shutdownNow()` in `onDestroy()`, `close()`, or `release()`? -> **Must be Yes**
3. [ ] If the field is `final`, did I avoid setting it to `null`? -> **Yes**
