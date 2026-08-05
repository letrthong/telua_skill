<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# Logging Practices & Security Rules (log_rule.md)

This document defines mandatory guidelines for logging, privacy compliance (PII protection), tag standardization, and debug log management in Java/Android development.

---

## 1. Core Rules

### Rule 1.1: Absolute Prohibition of Standard Output & Raw Stack Traces
Never use `System.out.println()`, `System.err.println()`, or `Throwable.printStackTrace()` in production Android/Java code. Always use `android.util.Log` or a structured logging wrapper (e.g., Timber, SLF4J).

### Rule 1.2: Strict Protection of Personally Identifiable Information (PII)
Never log sensitive user data or credentials. This includes passwords, auth tokens, OTP codes, credit card numbers, phone numbers, email addresses, and personal user data.

### Rule 1.3: Debug Log Guarding & Centralized Logger Utility
Verbose (`Log.v`) and Debug (`Log.d`) statements **MUST** be guarded to avoid string concatenation overhead and data leaks in release builds.

To avoid cluttering business methods with ugly boilerplate `if (DEBUG) { ... }` or `if (BuildConfig.DEBUG) { ... }` blocks throughout caller classes, follow these guidelines:

1. **🌟 Recommended Primary Pattern — Centralized Logger Wrapper (`AppLogger`)**:
   Encapsulate `BuildConfig.DEBUG` checks and `String.format()` formatting inside a static `AppLogger` utility class. Callers write clean 1-line log calls without ugly `if` statements.
2. **Error & Warning Logs (`Log.e` / `Log.w` or `AppLogger.e` / `AppLogger.w`)**:
   Critical errors, exceptions, and failure warnings **MUST NOT** be guarded by `if (DEBUG)`. They must always execute in both Debug and Release builds to provide crash/error diagnostics.
3. **Avoid Ugly Inline `if` Statements**:
   Do **NOT** sprinkle manual `if (DEBUG) { Log.d(...); }` or `if (BuildConfig.DEBUG)` checks across caller classes. It degrades readability and clutters code. Use `AppLogger.d(...)` instead.

### Rule 1.4: Dynamic Tag Naming & Tag Length Safety Standard
Define `TAG` using the class literal to prevent copy-paste errors and keep tag names refactoring-safe:
```java
private static final String TAG = MyClassName.class.getSimpleName();
```
> [!NOTE]
> On legacy Android devices (API level < 26), `android.util.Log` throws an `IllegalArgumentException` if `TAG` exceeds 23 characters. `AppLogger` automatically caps/truncates tag strings to 23 characters for safety.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
public class AuthManager {
    // Bad 1: Hardcoded tag string prone to refactoring mistakes
    private static final String TAG = "AuthManager"; 

    public void authenticateUser(String username, String password, String userToken) {
        // Bad 2: Printing raw stack trace to standard output
        try {
            login(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Login failed for: " + username);
        }

        // Bad 3: Logging sensitive credentials & tokens!
        // Bad 4: String concatenation '+' evaluated even if debug logging is disabled!
        Log.d(TAG, "User token: " + userToken + ", Password: " + password);

        // Bad 5: Ugly inline 'if' block cluttering business logic!
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Debugging user auth flow...");
        }
    }
}
```

### ✅ RECOMMENDED BEST PRACTICE (Clean Centralized AppLogger Utility):

```java
/**
 * Production-Grade AppLogger Utility encapsulating BuildConfig.DEBUG checks & tag safety.
 * Eliminates ugly 'if (DEBUG)' boilerplate code across caller classes.
 */
public final class AppLogger {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final int MAX_TAG_LENGTH = 23;

    private AppLogger() {} // Prevent instantiation

    /**
     * Formats and truncates TAG string to 23 chars for legacy Android compatibility.
     */
    private static String sanitizeTag(String tag) {
        if (tag == null) return "AppLogger";
        return (tag.length() > MAX_TAG_LENGTH) ? tag.substring(0, MAX_TAG_LENGTH) : tag;
    }

    /**
     * Verbose log — Guarded internally by DEBUG flag.
     */
    public static void v(String tag, String message, Object... args) {
        if (DEBUG) {
            Log.v(sanitizeTag(tag), (args != null && args.length > 0) ? String.format(message, args) : message);
        }
    }

    /**
     * Debug log — Guarded internally by DEBUG flag. 1-line clean call in caller.
     */
    public static void d(String tag, String message, Object... args) {
        if (DEBUG) {
            Log.d(sanitizeTag(tag), (args != null && args.length > 0) ? String.format(message, args) : message);
        }
    }

    /**
     * Info log — Always executes in Release and Debug.
     */
    public static void i(String tag, String message, Object... args) {
        Log.i(sanitizeTag(tag), (args != null && args.length > 0) ? String.format(message, args) : message);
    }

    /**
     * Warning log — Always executes in Release and Debug.
     */
    public static void w(String tag, String message, Object... args) {
        Log.w(sanitizeTag(tag), (args != null && args.length > 0) ? String.format(message, args) : message);
    }

    /**
     * Error log — Always executes in Release and Debug for critical error tracking.
     */
    public static void e(String tag, String message, Throwable throwable) {
        Log.e(sanitizeTag(tag), message, throwable);
    }
}

/**
 * Caller Class — Clean 1-line logging without ugly 'if (DEBUG)' blocks.
 */
public class AuthManager {
    private static final String TAG = AuthManager.class.getSimpleName();

    public void authenticateUser(String username, String password, String userToken) {
        try {
            login(username, password);
        } catch (AuthException e) {
            // Correct: Error log called directly via AppLogger.e without 'if (DEBUG)'
            AppLogger.e(TAG, "Authentication failed for sanitized user ID: " + getSanitizedId(username), e);
        }

        // Correct & Clean: 1-line Debug log guarded internally by AppLogger (NO ugly 'if (DEBUG)' block)
        AppLogger.d(TAG, "Authentication attempt completed for user: %s", getSanitizedId(username));
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java/Android code with logging:
1. [ ] Are there any `System.out.println()` or `e.printStackTrace()` calls? -> **Must be removed**.
2. [ ] Are passwords, tokens, or PII exposed in log strings? -> **Must be sanitized**.
3. [ ] Are debug logs called via `AppLogger.d(...)` / `AppLogger.v(...)` (avoiding ugly inline `if (DEBUG)` blocks)? -> **Yes**.
4. [ ] Are error/warning logs (`AppLogger.e` / `AppLogger.w`) allowed to log in release builds without `if (DEBUG)`? -> **Yes**.
5. [ ] Is `TAG` defined as `private static final String TAG = MyClass.class.getSimpleName()`? -> **Yes**.
