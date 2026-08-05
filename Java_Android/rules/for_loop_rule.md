<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# For-Loop Best Practice & Iteration Rules (for_loop_rule.md)

This document defines mandatory coding standards and refactoring rules when iterating over collections, arrays, and lists in Java/Android.

---

## 1. Core Rule: Prefer Enhanced For-Each Loops and Declarative Iteration

Traditional manual index-based loops (`for (int i = 0; i < list.size(); i++)`) are **strictly forbidden** unless low-level index manipulation or performance-critical hardware constraints explicitly require it. 

Always prefer:
1. **Enhanced For-Each Loop (`for (T item : list)`):** Best for clear, readable, and idiomatic non-transformative iteration.
2. **Java Streams / `forEach` (`list.forEach(...)` or `.stream()`):** Best for declarative transformations, filtering, and functional mapping.
3. **`IntStream.range()`:** Use only when index tracking is strictly mandatory.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):
Manual index management that introduces unnecessary boilerplate, clutter, and off-by-one risk.
```java
// Anti-pattern: Verbose index-based loop
List<User> users = getUsers();
for (int i = 0; i < users.size(); i++) {
    User user = users.get(i);
    processUser(user);
}
```

### ✅ REQUIRED BEST PRACTICE:

#### Option A: Enhanced For-Each Loop (Clean & Readable)
```java
// Clean and idiomatic iteration
List<User> users = getUsers();
for (User user : users) {
    processUser(user);
}
```

#### Option B: Declarative Java Stream / `forEach`
```java
// Functional / Stream transformation
getUsers().stream()
    .filter(User::isActive)
    .forEach(this::processUser);
```

#### Option C: `IntStream` (When Index is Explicitly Required)
```java
// When index tracking is strictly necessary
IntStream.range(0, users.size())
    .forEach(i -> processUserWithIndex(i, users.get(i)));
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java code containing loops, the AI must verify:
1. [ ] Did I avoid writing `for (int i = 0; i < ...)` loops?
2. [ ] Is the iteration written using an enhanced for-each loop `for (Item item : list)` or Java Streams?
3. [ ] Are all comments written in professional English?
