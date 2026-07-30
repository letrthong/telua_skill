# Naming Conventions & Modifier Ordering Rules (naming_rule.md)

This document defines mandatory standards for naming variables, constants, classes, and organizing Java access modifiers and member ordering in Android development.

---

## 1. Naming Conventions (Android/AOSP Standard)

### 1.1 Field Prefixes
* **Non-Public, Non-Static Member Fields:** Must start with `m` prefix in lowerCamelCase (e.g., `mContext`, `mUserName`, `mExecutor`).
* **Static Member Fields:** Must start with `s` prefix in lowerCamelCase (e.g., `sInstance`, `sCount`).
* **Other Fields & Parameters:** Use standard lowerCamelCase without prefixes (e.g., `userId`, `context`).

### 1.2 Constants
* **`static final` Constants:** MUST use `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT_MS`, `KEY_USER_TOKEN`).

### 1.3 Classes, Interfaces, and Methods
* **Classes / Interfaces / Enums:** Use `UpperCamelCase` / `PascalCase` (e.g., `UserProfileActivity`, `NetworkRepository`).
* **Methods & Local Variables:** Use `lowerCamelCase` (e.g., `getUserProfile()`, `activeUserCount`).

---

## 2. Java Modifier Ordering (JLS Standard)

Always arrange field and method modifiers in the standard Java Language Specification order:
`public` / `protected` / `private` -> `static` -> `final` -> `transient` -> `volatile` -> `synchronized`

* ✅ `public static final String KEY_DATA = "data";` (Correct)
* ❌ `final static public String KEY_DATA = "data";` (Strictly Banned)

---

## 3. Class Member Layout Order

Organize class members logically from top to bottom:
1. **Static Constants:** `public static final` / `private static final`
2. **Static Fields:** `private static UserActivity sInstance;`
3. **Instance Fields:** `private final Context mContext;`, `private Handler mHandler;`
4. **Constructors / Factory Methods**
5. **Public Methods (Lifecycle & API Methods)**
6. **Private & Helper Methods**

---

## 4. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):
Unprefixed non-public fields, incorrect modifier order, confusing constant naming.
```java
public class UserActivity extends Activity {
    // Bad: Modifier order out of standard, constant not UPPER_SNAKE_CASE
    static final public int default_timeout = 5000;
    
    // Bad: Missing 'm' prefix for private instance field
    private Context context;
    
    // Bad: Missing 's' prefix for private static field
    private static UserActivity instance;
}
```

### ✅ REQUIRED BEST PRACTICE:
```java
public class UserActivity extends Activity {
    // Correct: JLS modifier order + UPPER_SNAKE_CASE for constant
    public static final int DEFAULT_TIMEOUT_MS = 5000;

    // Correct: 's' prefix for private static field
    private static UserActivity sInstance;

    // Correct: 'm' prefix for private instance fields
    private final Context mContext;
    private Handler mHandler;

    public UserActivity(Context context) {
        this.mContext = context;
    }
}
```

---

## 5. AI Self-Correction & Verification Checklist

Before emitting any Java/Android code, the AI must verify:
1. [ ] Do private/protected instance fields start with `m` prefix? (e.g., `mExecutor`)
2. [ ] Do private/protected static fields start with `s` prefix? (e.g., `sInstance`)
3. [ ] Are `static final` constants named in `UPPER_SNAKE_CASE`?
4. [ ] Are access modifiers ordered correctly (`public/private` -> `static` -> `final`)?
