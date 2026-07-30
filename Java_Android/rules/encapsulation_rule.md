# Encapsulation & Member Field Access Rules (encapsulation_rule.md)

This document defines mandatory object-oriented encapsulation standards, prohibiting direct access or public exposure of class member fields in Java/Android.

---

## 1. Core Rules

### Rule 1.1: Strict Field Encapsulation (Private Fields Only)
All non-constant class fields (instance fields and static fields) **MUST** be declared with `private` access visibility. Never declare member fields as `public` or package-private unless required by framework annotations (e.g., ButterKnife/Dagger injections).

### Rule 1.2: Prohibition of Direct Member Access
External classes **MUST NOT** access or mutate another object's fields directly (e.g., `user.mAge = 30` or `int age = user.age`). Access must always be mediated through controlled getter and setter methods or domain-specific operations.

### Rule 1.3: Immutable/Defensive Accessors
* **Getters:** Return immutable copies or unmodifiable collections for mutable objects (e.g., `Collections.unmodifiableList(mItems)`).
* **Setters:** Validate inputs defensively before mutating internal state.

### Rule 1.4: Exception for `static final` Constants Only
The only fields permitted to be declared `public` are immutable constants defined as `public static final`.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
// Bad: Exposed public fields, direct field access breaking encapsulation
public class UserProfile {
    public String name;
    public int age;
    public List<String> roles;
}

public class Main {
    public void process(UserProfile user) {
        // Direct field mutation without validation
        user.age = -5; // Invalid state!
        user.name = "Alice";
        user.roles.add("ADMIN"); // Directly mutating internal list
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

```java
// Correct: Private fields with encapsulation & validation
public class UserProfile {
    private String mName;
    private int mAge;
    private final List<String> mRoles = new ArrayList<>();

    public UserProfile(String name, int age) {
        setName(name);
        setAge(age);
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = Objects.requireNonNull(name, "Name cannot be null");
    }

    public int getAge() {
        return mAge;
    }

    public void setAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        this.mAge = age;
    }

    // Defensive copy for mutable collections
    public List<String> getRoles() {
        return Collections.unmodifiableList(mRoles);
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java class definition:
1. [ ] Are all non-constant fields marked as `private`? -> **Must be Yes**.
2. [ ] Are public fields completely avoided (except `public static final` constants)? -> **Must be Yes**.
3. [ ] Is direct field access (`obj.field = value`) avoided in external callers? -> **Must be Yes**.
4. [ ] Do getters return defensive copies for mutable collections? -> **Must be Yes**.
