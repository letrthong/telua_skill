<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# Singleton Pattern & Thread Safety Rules (singleton_thread_safety_rule.md)

This document defines mandatory standards for thread-safe Singleton pattern implementation and preventing Context memory leaks in Android Java development.

---

## 1. Core Rules

### Rule 1.1: Mandatory Thread-Safe Initialization
When creating a Singleton class, it **MUST** be thread-safe. Use either the **Bill Pugh Holder Pattern** (lazy, thread-safe, no synchronization overhead) or **Double-Checked Locking with `volatile`**.

### Rule 1.2: Absolute Prohibition of Non-Volatile Double-Checked Locking
If using Double-Checked Locking, the singleton instance variable **MUST** be declared with the `volatile` keyword to prevent instruction reordering bugs.

### Rule 1.3: Application Context Usage (Prevent Memory Leaks)
Singletons **MUST NOT** hold direct references to `Activity`, `Fragment`, or `View` Contexts. If a `Context` parameter is passed, store only `context.getApplicationContext()`.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
// Bad: Non-thread-safe lazy initialization + Activity Context leak!
public class NetworkManager {
    private static NetworkManager sInstance;
    private Context mContext;

    private NetworkManager(Context context) {
        this.mContext = context; // Danger: Retaining Activity Context causes Memory Leak!
    }

    public static NetworkManager getInstance(Context context) {
        if (sInstance == null) { // Danger: Race condition in multi-threaded environment!
            sInstance = new NetworkManager(context);
        }
        return sInstance;
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

#### Option A: Bill Pugh Holder Pattern (Recommended - Lazy & Thread-Safe)
```java
public class NetworkManager {
    private final Context mApplicationContext;

    private NetworkManager(Context context) {
        // Safe: Store only ApplicationContext
        this.mApplicationContext = context.getApplicationContext();
    }

    private static class InstanceHolder {
        // Loaded lazily on first access, guaranteed thread-safe by JVM classloader
        private static NetworkManager sInstance;
    }

    public static synchronized void init(Context context) {
        if (InstanceHolder.sInstance == null) {
            InstanceHolder.sInstance = new NetworkManager(context);
        }
    }

    public static NetworkManager getInstance() {
        if (InstanceHolder.sInstance == null) {
            throw new IllegalStateException("NetworkManager must be initialized first!");
        }
        return InstanceHolder.sInstance;
    }
}
```

#### Option B: Volatile Double-Checked Locking
```java
public class CacheRepository {
    // Correct: 'volatile' prevents instruction reordering
    private static volatile CacheRepository sInstance;

    private CacheRepository() {}

    public static CacheRepository getInstance() {
        if (sInstance == null) {
            synchronized (CacheRepository.class) {
                if (sInstance == null) {
                    sInstance = new CacheRepository();
                }
            }
        }
        return sInstance;
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Singleton implementation:
1. [ ] Is the singleton instance thread-safe (Bill Pugh Holder or `volatile` Double-Checked Locking)? -> **Must be Yes**.
2. [ ] Is `context.getApplicationContext()` stored instead of raw `Activity` Context? -> **Must be Yes**.
3. [ ] Is the constructor `private`? -> **Must be Yes**.
