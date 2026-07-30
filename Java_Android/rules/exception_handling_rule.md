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
