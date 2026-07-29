# AI Instructions: Mandatory Executor Shutdown Rule

## CRITICAL RULE: ALWAYS SHUTDOWN EXECUTORS
Whenever you generate, refactor, or review code that initializes or uses an `Executor` or `ExecutorService` (specifically instances like `mExecutor = Executors.newSingleThreadExecutor()` or similar thread pools), you **MUST** ensure that a corresponding `shutdown()` call is included when the object or component reaches the end of its lifecycle.

## Why This Matters
Failing to shut down an `ExecutorService` keeps background threads alive, leading to severe memory leaks, lingering background tasks, and application hangs (especially in Android/Java environments).

## Implementation Pattern
Whenever you write code containing an executor definition like:
```java
private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
```
You **MUST** include the cleanup/shutdown lifecycle block in the appropriate disposal method (e.g., `onDestroy()`, `close()`, `release()`, or `shutdown()`):
```java
if (mExecutor instanceof java.util.concurrent.ExecutorService) {
    ((java.util.concurrent.ExecutorService) mExecutor).shutdown();
}

OR 
if (mExecutor != null) {
   mExecutor.shutdown();
}

mExecutor = null;
```

## AI Checklist for Code Generation
1. Did I initialize `mExecutor` (or any `ExecutorService`)? -> **Yes**
2. Have I added the matching `shutdown()` check in the teardown/destroy method? -> **Must be Yes**
