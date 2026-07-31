# Magic Number Elimination & Immutability Rules (magic_number_immutability_rule.md)

This document defines mandatory standards for eliminating magic numbers/strings and enforcing immutability practices (`final`, Java Records, unmodifiable collections).

---

## 1. Core Rules

### Rule 1.1: Complete Prohibition of Magic Numbers & Hardcoded Strings
* **No Unnamed Constants:** Never embed literal numbers (e.g. `Thread.sleep(5000)`, `if (status == 3)`) or raw hardcoded configuration strings directly inside logic methods.
* **Extraction Mandate:** All magic numbers and configurable strings **MUST** be extracted into named `private static final` or `public static final` constants in `UPPER_SNAKE_CASE` at class level.

### Rule 1.2: Mandatory Immutability Practices
* **Immutability First:** Prefer immutable state representation.
  * Use `final` keywords for method parameters and local variables that should not be reassigned.
  * Use Java Records or unmodifiable views (`Collections.unmodifiableList(...)`, `List.copyOf(...)`) for return data objects.
  * Never return direct references to mutable internal collections or arrays.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Magic Numbers & Mutable Data Leaks):

```java
// BAD: Contains magic number 5000, 300, and returns raw mutable list!
public class CarMonitor {
    private List<String> mErrors = new ArrayList<>();

    public void checkEngine() throws Exception {
        Thread.sleep(5000); // BAD: Magic number 5000!
        if (mErrors.size() > 300) { // BAD: Magic number 300!
            Log.e("TAG", "Too many errors");
        }
    }

    public List<String> getErrors() {
        return mErrors; // BAD: Leaks mutable internal list reference!
    }
}
```

### ✅ BEST PRACTICE (Named Constants & Defensive Immutability):

```java
package com.example.app;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Demonstrates total elimination of magic numbers and defensive immutability.
 */
public class CarMonitor {
    private static final String TAG = CarMonitor.class.getSimpleName();
    
    // Extracted named constants eliminating magic numbers
    private static final long ENGINE_CHECK_INTERVAL_MS = 5_000L;
    private static final int MAX_ERROR_THRESHOLD = 300;

    private final List<String> mErrors = new ArrayList<>();

    public void checkEngine() {
        try {
            Thread.sleep(ENGINE_CHECK_INTERVAL_MS);
            
            synchronized (this) {
                if (mErrors.size() > MAX_ERROR_THRESHOLD) {
                    Log.e(TAG, "Error count (" + mErrors.size() + ") exceeded threshold: " + MAX_ERROR_THRESHOLD);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Engine check interrupted", e);
        }
    }

    public synchronized void recordError(String errorMessage) {
        Objects.requireNonNull(errorMessage, "ErrorMessage cannot be null");
        mErrors.add(errorMessage);
    }

    /**
     * Returns an unmodifiable defensive copy of internal error list.
     */
    public synchronized List<String> getErrors() {
        return Collections.unmodifiableList(new ArrayList<>(mErrors));
    }
}
```

---

## 3. AI Self-Correction Checklist

Before emitting Java code:
1. [ ] Are all numeric literals and hardcoded strings extracted into `UPPER_SNAKE_CASE` constants? -> **Must be Yes**.
2. [ ] Are internal mutable collections wrapped in `Collections.unmodifiableList(...)` before returning? -> **Must be Yes**.
