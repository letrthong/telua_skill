<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# Heap & Stack Object Lifetime Rules (heap_stack_object_lifetime_rule.md)

This document defines mandatory guidelines for understanding and correctly using Java memory model semantics — specifically the **HEAP vs STACK** distinction, object lifetime across threads, safe publication, and the consequences for `ExecutorService` shutdown in Java/Android development.

---

## 1. Core Concepts

### 1.1 HEAP vs STACK — Where Objects Live

| Memory | What lives there | Lifetime | Owner |
|--------|------------------|----------|-------|
| **HEAP** | Actual object data (`new MessageObject(...)`) | Until GC reclaims (no more references) | Shared across ALL threads |
| **STACK** | Local variables, method parameters, references | Until method/thread returns | Per-thread (destroyed when thread dies) |

**Key insight:** A local variable `objA` is only a **reference** (memory address) stored on the thread's STACK. The actual object it points to lives on the HEAP.

### 1.2 Object Survival After Thread Death

When a thread terminates:
1. Its **STACK memory is destroyed** → local references (`objA`) are gone.
2. The **HEAP object is NOT destroyed** — it survives as long as **at least one reference** to it exists anywhere in the process.
3. The **Garbage Collector (GC)** only reclaims objects with **zero reachable references**.

> **Consequence:** An object created inside a child thread can safely outlive that thread **if** its reference was published to another thread (e.g., via `AtomicReference`, `volatile` field, or a shared container) before the thread died.

---

## 2. Core Rules

### Rule 2.1: Publish Before Thread Death
When a background thread creates an object that must be used after the thread terminates, the reference **MUST** be published to a shared, thread-safe container **before** the thread finishes:

```java
// ✅ CORRECT: Publish reference before thread ends
AtomicReference<MessageObject> shared = new AtomicReference<>();
executor.execute(() -> {
    MessageObject obj = new MessageObject("data");
    shared.set(obj);          // publish BEFORE thread ends
});
executor.shutdown();
// ... later, main thread can safely read shared.get()
```

```java
// ❌ ANTI-PATTERN: Reference never escapes the thread → object is lost
executor.execute(() -> {
    MessageObject obj = new MessageObject("data");
    // obj is only on this thread's STACK → gone when thread dies
});
executor.shutdown();
// shared container is empty — object unreachable → GC reclaims it
```

### Rule 2.2: Safe Publication via AtomicReference / Volatile
- Use `AtomicReference<T>` or a `volatile` field for the shared container to guarantee **happens-before** semantics: the write in the child thread is visible to the main thread after `set()`/`get()`.
- **Never** use a plain (non-volatile) field for cross-thread publication — the reference may never become visible (stale read).

### Rule 2.3: Published Objects MUST Be Immutable (Safe Publication)
- The object published across threads **MUST** be effectively immutable (final fields, no setters, or defensive copies).
- **Prohibition of In-Place Mutation:** Never mutate the internal fields of an object already referenced by a published `volatile`/`AtomicReference` field. All state updates must follow the atomic replacement idiom (construct a new snapshot and publish it via a single write).

```java
// ✅ CORRECT: Immutable published object
record MessageObject(String content) { }

// ❌ ANTI-PATTERN: Mutable published object (race conditions)
class MessageObject {
    private String content;   // mutable, no final
    public void setContent(String c) { this.content = c; }
}
```

### Rule 2.4: Executor Shutdown Does NOT Kill Published Objects
- Calling `executor.shutdown()` / `shutdownNow()` only stops the **thread pool** — it does **NOT** destroy objects already created and published to the HEAP.
- This is why the pattern "create in background thread → publish → shutdown executor → use later" is valid.
- **However**, `shutdownNow()` interrupts running tasks — if the task was interrupted **before** publishing, the object is lost. Ensure publication happens before any interruptible operation.

### Rule 2.5: Always Shutdown Executors (Cross-Reference)
- Every `ExecutorService` **MUST** be shut down at the end of its lifecycle (`onDestroy()`, `release()`, `close()`) to avoid **thread leaks**.
- See `executor_shutdown_rule.md` for the mandatory shutdown checklist.
- Prefer `awaitTermination(timeout, unit)` over busy-wait loops (`while (!executor.isTerminated())`) to avoid burning 100% CPU.

---

## 3. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
public class DataManager {
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private MessageObject mShared;   // NOT volatile → stale read risk

    public void loadAsync() {
        mExecutor.execute(() -> {
            mShared = new MessageObject("data");   // Bad 1: plain field, no safe publication
            // Bad 2: if thread is interrupted before this line, object is lost
        });
        mExecutor.shutdown();
        // Bad 3: busy-wait burns CPU
        while (!mExecutor.isTerminated()) { }
        // Bad 4: mShared may be null or stale (no happens-before guarantee)
        mShared.showMessage();
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

```java
public class DataManager {
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final AtomicReference<MessageObject> mShared = new AtomicReference<>();

    public void loadAsync() {
        mExecutor.execute(() -> {
            MessageObject obj = new MessageObject("data");
            mShared.set(obj);   // ✅ publish via AtomicReference (happens-before)
        });
        mExecutor.shutdown();
        try {
            if (!mExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                mExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            mExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        MessageObject result = mShared.get();   // ✅ safe read
        if (result != null) {
            result.showMessage();
        }
    }

    public void release() {
        mExecutor.shutdownNow();
    }
}
```

---

## 4. AI Checklist for Code Generation

1. [ ] Did I create an object inside a background thread that must be used later? -> **Yes**
2. [ ] Did I publish the reference via `AtomicReference` / `volatile` / shared container **before** the thread ends? -> **Must be Yes**
3. [ ] Is the published object effectively **immutable** (final fields / record / defensive copy)? -> **Must be Yes**
4. [ ] Did I avoid busy-wait loops — using `awaitTermination(timeout, unit)` instead? -> **Must be Yes**
5. [ ] Did I shut down the `ExecutorService` in `release()` / `onDestroy()`? -> **Must be Yes**
6. [ ] Did I avoid mutating a published object in place (atomic replacement idiom)? -> **Must be Yes**
