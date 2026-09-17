<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.8 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# For-Loop & If-Else Control Flow Rules (for_loop_and_if_else_rule.md)

This document defines mandatory coding standards and refactoring rules for loop iteration (enhanced for-each, Streams) and branching control flow (explicit braces, guard clauses, early returns, boolean hygiene) in Java/Android development.

---

## 1. Loop Iteration Rules: Prefer Enhanced For-Each & Streams

Traditional manual index-based loops (`for (int i = 0; i < list.size(); i++)`) are **strictly forbidden** unless low-level index manipulation or performance-critical hardware constraints explicitly require it. 

Always prefer:
1. **Enhanced For-Each Loop (`for (T item : list)`):** Best for clear, readable, and idiomatic non-transformative iteration.
2. **Java Streams / `forEach` (`list.forEach(...)` or `.stream()`):** Best for declarative transformations, filtering, and functional mapping.
3. **`IntStream.range()`:** Use only when index tracking is strictly mandatory.

---

## 2. Loop Code Transformation Examples

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

## 3. If-Else & Control Flow Rules

### Rule 3.1: Always Use Explicit Braces — No Single-Line Statements
**Never** write single-line `if`, `else`, `for`, or `while` statements without braces. **Always** use explicit `{}` braces for all control flow blocks.

**Why This Matters:**
1. **Merge Safety:** Single-line `if` statements are extremely fragile during code merges. A merge conflict or inserted line can break logic silently.
2. **Readability & Scope Delimitation:** Explicit braces clearly define the boundaries of conditional execution.
3. **Bug Prevention:** Prevents accidental addition of statements that appear to belong to the branch but execute unconditionally (Apple `goto fail;` bug).

```java
// ❌ ANTI-PATTERN: Single-line statements without braces
if (event == null) return;
if (isActive) doAction(); else doFallback();
for (Item item : items) process(item);

// ✅ REQUIRED BEST PRACTICE: Explicit braces
if (event == null) {
    return;
}

if (isActive) {
    doAction();
} else {
    doFallback();
}

for (Item item : items) {
    process(item);
}
```

### Rule 3.2: Guard Clauses & Early Return (Flatten Deep Nesting)
* **Arrow Anti-Pattern Prohibition:** Never nest `if-else` blocks more than 2 levels deep (`if { if { if { ... } } }`).
* **Bouncer / Guard Clause Pattern:** Validate preconditions and error boundaries at the top of methods and return/throw immediately. Keep the "happy path" at the root indentation level.

```java
// ❌ ANTI-PATTERN: Deeply nested if-else (Arrow Anti-Pattern)
public void processOrder(Order order) {
    if (order != null) {
        if (order.isValid()) {
            if (order.isPaid()) {
                fulfillOrder(order);
            } else {
                throw new PaymentRequiredException();
            }
        } else {
            throw new InvalidOrderException();
        }
    }
}

// ✅ REQUIRED BEST PRACTICE: Guard Clauses / Early Exit
public void processOrder(Order order) {
    if (order == null) {
        return;
    }
    if (!order.isValid()) {
        throw new InvalidOrderException();
    }
    if (!order.isPaid()) {
        throw new PaymentRequiredException();
    }

    // Happy path remains flat at indentation level 1
    fulfillOrder(order);
}
```

### Rule 3.3: Boolean Hygiene & Simplification
* **Never Compare Against Booleans:** Never write `if (isValid == true)` or `if (isDone == false)`. Write `if (isValid)` or `if (!isDone)`.
* **Prohibition of Yoda Conditions:** Use natural left-to-right reading order: `if (count > 0)` instead of `if (0 < count)`. (For string equality with constants, `CONSTANT.equals(variable)` is permitted to prevent NPE).
* **Command Query Separation (CQS) in Conditions:** Never trigger mutating state commands directly inside an `if` condition expression.

```java
// ❌ ANTI-PATTERNS:
if (isReady == true) { ... }           // Redundant comparison
if (updateStatus("ONLINE")) { ... }     // Command mixed into condition

// ✅ BEST PRACTICES:
if (isReady) { ... }                   // Direct boolean evaluation
boolean updated = updateStatus("ONLINE"); // Separate command from check
if (updated) { ... }
```

### Rule 3.4: Prohibition of Nested Ternary Operators
* Use ternary expressions (`condition ? a : b`) **strictly** for simple, single-line variable assignments or returns.
* **Never** nest ternary operators (`a ? b : c ? d : e`). Use explicit `if-else` blocks instead.

---

## 4. AI Self-Correction & Verification Checklist

Before emitting any Java code containing loops or branching control flow:
1. [ ] Did I avoid writing `for (int i = 0; i < ...)` loops? -> **Must be Yes**.
2. [ ] Is iteration implemented via enhanced for-each or Java Streams? -> **Must be Yes**.
3. [ ] Are **all** `if`, `else`, `for`, `while` statements enclosed in explicit `{}` braces? -> **Must be Yes**.
4. [ ] Are nested `if-else` structures flattened using Guard Clauses / Early Returns (nesting depth $\le 2$)? -> **Must be Yes**.
5. [ ] Are boolean comparisons simplified (no `== true` / `== false`)? -> **Must be Yes**.
6. [ ] Are nested ternary expressions completely avoided? -> **Must be Yes**.
7. [ ] Are all comments written in professional English? -> **Must be Yes**.
