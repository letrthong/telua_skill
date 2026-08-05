<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# Exception Handling & Error Safety Rules (exception_handling_rule.md)

This document defines mandatory guidelines for exception catching, error propagation, exception chaining, and preventing swallowed errors in Java/Android development.

---

## 1. Core Rules

### Rule 1.1: Absolute Prohibition of Empty Catch Blocks
Never leave a `catch` block empty (swallowing exceptions). If an exception is intentionally ignored, document the reason explicitly with a comment. Otherwise, log the error or trigger fallback logic.

### Rule 1.2: Catch Specific Exceptions
Avoid catching generic `Exception` or `Throwable` unless acting as a top-level global boundary handler. Always catch specific exception types (`IOException`, `ParseException`, `SQLException`) to avoid catching unintended runtime exceptions like `NullPointerException`.

### Rule 1.3: Preserve Exception Cause (Exception Chaining)
When wrapping and rethrowing exceptions, always pass the original exception as the `cause` parameter to preserve the full stack trace.

### Rule 1.4: Prohibition of Returning & Passing Null
* **Clean Code Chapter 7 Standard:** Avoid returning `null` from methods. Returning `null` forces callers to write defensive `if (obj != null)` checks everywhere.
* **Return Alternatives:** Return `Optional<T>` for single nullable items, `Collections.emptyList()` / `Collections.emptySet()` for collections, or Null Object Pattern instances.
* **Avoid Passing Null:** Do not pass `null` as method arguments unless explicitly required by legacy APIs.

### Rule 1.5: Command Query Separation (CQS)
* **Separation of Side Effects:** A method must either perform an action (Command — mutating state) **OR** return data (Query — pure calculation without side effects), but never both.
* **❌ Anti-Pattern:** `if (setAttr("key", "val")) { ... }` (Mutates attribute AND returns boolean).
* **✅ Best Practice:** `if (attributeExists("key")) { setAttribute("key", "val"); }`

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
public void loadConfig(File configFile) {
    // Bad 1: Catching generic Exception and ignoring it completely!
    try {
        parseFile(configFile);
    } catch (Exception e) {
        // Empty catch block - errors are silently swallowed!
    }

    // Bad 2: Rethrowing without preserving original exception cause
    try {
        readNetworkData();
    } catch (IOException e) {
        throw new RuntimeException("Network failed"); // Original 'e' stack trace lost!
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

```java
public class ConfigLoader {
    private static final String TAG = "ConfigLoader";

    public void loadConfig(File configFile) throws ConfigException {
        // Correct: Catch specific exception type
        try (InputStream is = new FileInputStream(configFile)) {
            parseFile(is);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Config file not found, using default configuration.", e);
            useDefaultConfig();
        } catch (IOException e) {
            // Correct: Exception chaining - passing 'e' as the cause
            throw new ConfigException("Failed to read config file: " + configFile.getName(), e);
        }
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java code containing try-catch blocks:
1. [ ] Are all catch blocks non-empty with proper logging or fallback handling? -> **Must be Yes**.
2. [ ] Are specific exception types caught instead of raw `Exception`? -> **Must be Yes**.
3. [ ] When rethrowing, is the original exception passed as the `cause`? -> **Must be Yes**.
