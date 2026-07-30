# AI Coding Style & Engineering Guardrails Guide (v2.0 - Ultimate Edition)

This document serves as the absolute, non-negotiable engineering standard and system-level behavioral directive for AI code generation. Every line of code produced must strictly adhere to these architectural, stylistic, and safety mandates.

---

## 1. Core Engineering Commandments

1. **Modern & Idiomatic First:** Always leverage the latest, highest-level language constructs and idioms. Avoid legacy workarounds or boilerplate code.
2. **Readability & Clean Code:** Write self-documenting code. Functions must follow the Single Responsibility Principle (SRP). Avoid deep nesting and magical numbers.
3. **Immutability & Safety:** Prefer immutable data structures, explicit null-safety handling, and strict type checking.
4. **Universal English Standard:** All code comments, documentation, commit messages, and variable names must be written in professional English.

---

## 2. Java Engineering Standards (Strict Enforcement)

### 2.1. Absolute Prohibition of Manual Index-Based `for` Loops
* **Rule:** Traditional manual loops (`for(int i = 0; i < size; i++)`) are **strictly forbidden** in Java code unless an absolute, benchmarked hardware/low-level constraint necessitates it.
* **Why:** They are prone to off-by-one errors, mutable state leakage, and lack declarative intent.

#### ❌ ANTI-PATTERN (Strictly Banned):
```java
List<User> users = getUsers();
List<String> activeEmails = new ArrayList<>();
for (int i = 0; i < users.size(); i++) {
    User user = users.get(i);
    if (user.isActive()) {
        activeEmails.add(user.getEmail());
    }
}
```

#### ✅ REQUIRED BEST PRACTICE:
```java
// Option A: Declarative transformation using Java Streams
List<String> activeEmails = getUsers().stream()
    .filter(User::isActive)
    .map(User::getEmail)
    .toList();

// Option B: For non-transformative iteration, use enhanced for-loop
for (User user : users) {
    logger.info("Processing user: {}", user.getId());
}

// Option C: If index tracking is strictly mandatory, use IntStream
IntStream.range(0, items.size())
    .forEach(i -> processItem(i, items.get(i)));
```

---

### 2.2. Comprehensive Null Safety (`Optional` & Contracts)
* Never return `null` from methods; return `Optional<T>` or empty collections instead.
* Utilize `Objects.requireNonNull()` or bean validation for defensive programming.

#### ❌ ANTI-PATTERN:
```java
public String getUserRole(User user) {
    if (user != null && user.getProfile() != null) {
        return user.getProfile().getRole();
    }
    return "GUEST";
}
```

#### ✅ REQUIRED BEST PRACTICE:
```java
public String getUserRole(User user) {
    return Optional.ofNullable(user)
        .map(User::getProfile)
        .map(Profile::getRole)
        .orElse("GUEST");
}
```

---

### 2.3. Immutability & Modern Records
* Use `final` keywords for local variables and parameters that do not mutate.
* Use Java `record` for DTOs, value objects, and immutable data holders.

```java
public record TransactionDto(
    @NotNull String transactionId,
    @Positive BigDecimal amount,
    @NotNull Instant timestamp
) {}
```

---

## 3. Python Engineering Standards

* **PEP 8 Compliance:** Follow standard formatting guidelines.
* **Comprehensions over Loops:** Use list, dict, and set comprehensions instead of manual `for` loop appends.
* **Strict Type Hinting:** Every function/method signature must include complete parameter and return type hints.

#### ❌ ANTI-PATTERN:
```python
def extract_active_usernames(users):
    result = []
    for i in range(len(users)):
        if users[i]['is_active']:
            result.append(users[i]['username'])
    return result
```

#### ✅ REQUIRED BEST PRACTICE:
```python
from typing import List, Dict, Any

def extract_active_usernames(users: List[Dict[str, Any]]) -> List[str]:
    """Extract usernames of active users from the input list using comprehension."""
    return [user["username"] for user in users if user.get("is_active", False)]
```

---

## 4. AI Self-Correction & Verification Checklist

Before emitting any response containing code, the AI must internally verify:
1. [ ] Are there any manual `for (int i = 0; ...)` loops in Java? *(If yes, refactor to Stream/for-each immediately)*.
2. [ ] Are all comments and documentation strings written clearly in English?
3. [ ] Are potential `NullPointerException` vectors handled via `Optional` or defensive guards?
4. [ ] Is type safety fully maintained across signatures?
