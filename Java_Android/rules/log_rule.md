# Logging Practices & Security Rules (log_rule.md)

This document defines mandatory guidelines for logging, privacy compliance (PII protection), tag standardization, and debug log management in Java/Android development.

---

## 1. Core Rules

### Rule 1.1: Absolute Prohibition of Standard Output & Raw Stack Traces
Never use `System.out.println()`, `System.err.println()`, or `Throwable.printStackTrace()` in production Android/Java code. Always use `android.util.Log` or a structured logging wrapper (e.g., Timber, SLF4J).

### Rule 1.2: Strict Protection of Personally Identifiable Information (PII)
Never log sensitive user data or credentials. This includes passwords, auth tokens, OTP codes, credit card numbers, phone numbers, email addresses, and personal user data.

### Rule 1.3: Debug Log Guarding & String Concatenation Avoidance
Verbose (`Log.v`) and Debug (`Log.d`) statements **MUST** be guarded by `BuildConfig.DEBUG` checks or `Log.isLoggable(TAG, Log.DEBUG)` to avoid string concatenation overhead and data leaks in release builds.

### Rule 1.4: Dynamic Tag Naming Standard
Define `TAG` using the class literal to prevent copy-paste errors and keep tag names refactoring-safe:
```java
private static final String TAG = MyClassName.class.getSimpleName();
```

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
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

```java
public class AuthManager {
    // Correct: Dynamic refactoring-safe TAG
    private static final String TAG = AuthManager.class.getSimpleName();

    public void authenticateUser(String username, String password, String userToken) {
        try {
            login(username, password);
        } catch (AuthException e) {
            // Correct: Structured error logging without stack trace dump
            Log.e(TAG, "Authentication failed for sanitized user ID: " + getSanitizedId(username), e);
        }

        // Correct: Debug log guarded by BuildConfig.DEBUG, no PII leaked, formatted string
        if (BuildConfig.DEBUG) {
            Log.d(TAG, String.format("Authentication attempt completed for user: %s", getSanitizedId(username)));
        }
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java/Android code with logging:
1. [ ] Are there any `System.out.println()` or `e.printStackTrace()` calls? -> **Must be removed**.
2. [ ] Are passwords, tokens, or PII exposed in log strings? -> **Must be removed/sanitized**.
3. [ ] Are debug logs guarded by `BuildConfig.DEBUG`? -> **Yes**.
4. [ ] Is `TAG` defined as `private static final String TAG = MyClass.class.getSimpleName()`? -> **Yes**.
