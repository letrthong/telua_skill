# Logging Practices & Security Rules (log_rule.md)

This document defines mandatory guidelines for logging, privacy compliance (PII protection), and debug log management in Java/Android development.

---

## 1. Core Rules

### Rule 1.1: Absolute Prohibition of Standard Output & Raw Stack Traces
Never use `System.out.println()`, `System.err.println()`, or `Throwable.printStackTrace()` in production Android/Java code. Always use `android.util.Log` or a structured logging library (e.g., Timber, SLF4J).

### Rule 1.2: Strict Protection of Personally Identifiable Information (PII)
Never log sensitive user data or credentials. This includes passwords, auth tokens, OTP codes, credit card numbers, phone numbers, email addresses, and personal user data.

### Rule 1.3: Debug Log Guarding in Production
Verbose (`Log.v`) and Debug (`Log.d`) statements must be guarded by `BuildConfig.DEBUG` checks or `Log.isLoggable(TAG, Log.DEBUG)` to avoid string concatenation overhead and data leaks in release builds.

### Rule 1.4: Standardized Tag Naming
Define a single, private static constant `TAG` at the top of each class:
```java
private static final String TAG = "MyClassName";
```

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
public void authenticateUser(String username, String password) {
    // Bad 1: Printing raw stack trace to standard output
    try {
        login(username, password);
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Login failed for: " + username);
    }

    // Bad 2: Logging sensitive credentials & tokens!
    Log.d("AUTH", "User token: " + userToken + ", Password: " + password);
}
```

### ✅ REQUIRED BEST PRACTICE:

```java
public class AuthManager {
    private static final String TAG = "AuthManager";

    public void authenticateUser(String username, String password) {
        try {
            login(username, password);
        } catch (AuthException e) {
            // Correct: Structured error logging without stack trace dump
            Log.e(TAG, "Authentication failed for user ID: " + getSanitizedUserId(username), e);
        }

        // Correct: Debug log guarded by BuildConfig.DEBUG, no PII leaked
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Authentication attempt completed.");
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
4. [ ] Is `TAG` defined as `private static final String TAG`? -> **Yes**.
