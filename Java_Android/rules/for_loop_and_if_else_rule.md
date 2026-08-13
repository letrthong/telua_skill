<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# For-Loop Best Practice & Iteration Rules (for_loop_and_if_else_rule.md)

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

## 3. Mandatory Braces for Control Flow Statements (if / else / for / while)

### Rule 3.1: Always Use Explicit Braces — No Single-Line `if` Statements

**Never** write single-line `if` statements without braces. **Always** use explicit `{}` braces for all control flow statements (`if`, `else if`, `else`, `for`, `while`, `do`).

**Why This Matters:**
1. **Merge Safety:** Single-line `if` statements are extremely fragile during code merges. A merge can easily insert a line between the `if` condition and the intended body, silently breaking the logic — the new line executes unconditionally while the original statement remains gated.
2. **Readability:** Explicit braces clearly delimit the scope of the condition.
3. **Bug Prevention:** Prevents accidental addition of statements that appear to be inside the `if` block but are actually outside it.

### ❌ ANTI-PATTERN (Strictly Banned):

```java
// Bad 1: Single-line if without braces — fragile during merge
if (event == null) return;

// Bad 2: Single-line if-else without braces
if (isActive) doSomething();
else doOtherThing();

// Bad 3: Single-line for without braces
for (Item item : items) process(item);

// Bad 4: Merge can silently break logic
// Before merge:
if (config.enabled) startService();
// After merge (another branch adds a line):
if (config.enabled) log("starting");  // ← only this line is gated!
startService();                        // ← now runs unconditionally!
```

### ✅ REQUIRED BEST PRACTICE:

```java
// Correct: Explicit braces — merge-safe and readable
if (event == null) {
    return;
}

// Correct: Braces for if-else
if (isActive) {
    doSomething();
} else {
    doOtherThing();
}

// Correct: Braces for for-each
for (Item item : items) {
    process(item);
}

// Correct: Merge-safe — added lines stay inside the block
if (config.enabled) {
    log("starting");
    startService();
}
```

---

## 4. AI Self-Correction & Verification Checklist

Before emitting any Java code containing loops or control flow, the AI must verify:
1. [ ] Did I avoid writing `for (int i = 0; i < ...)` loops?
2. [ ] Is the iteration written using an enhanced for-each loop `for (Item item : list)` or Java Streams?
3. [ ] Are **all** `if`, `else`, `for`, `while` statements using explicit `{}` braces? (No single-line bodies!)
4. [ ] Are all comments written in professional English?
