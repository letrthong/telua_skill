
# AI Coding Style Guide

This document defines the rules, standards, and practical examples to guide AI in generating clean, modern, optimized, and best-practice-compliant source code across popular programming languages (especially Java, Python, JavaScript/TypeScript...).

---

## 1. Core Principles

1. **Modern & Idiomatic:** Always prioritize the latest language syntaxes and features (e.g., Java Streams instead of traditional loops when appropriate).
2. **Clean & Readable:** Use meaningful variable/function names, keep functions concise (Single Responsibility Principle), and avoid code duplication (DRY).
3. **Safe & Secure:** Handle exceptions clearly, prevent null pointers, and validate input data.
4. **Performance:** Optimize algorithms, minimize redundant memory allocation, and avoid unnecessary costly operations.

---

## 2. Java Coding Standards

### 2.1. Prohibition of Traditional Manual `for` Loops (`for(int i = 0; ... )`)
Unless there is an extreme, low-level hardware performance reason, **strictly avoid** traditional `for` loops with manual index counters `i`. Instead, use **Java Streams**, **`forEach`**, or **Enhanced For-loops (`for-each`)**.

#### ❌ ANTI-PATTERN (What NOT to do):
```java
// Using traditional for loop with a manual index counter
List<String> names = Arrays.asList("An", "Bình", "Cường");
List<String> upperNames = new ArrayList<>();
for (int i = 0; i < names.size(); i++) {
    if (names.get(i).startsWith("A")) {
        upperNames.add(names.get(i).toUpperCase());
    }
}# AI Coding Style Guide

This document defines the rules, standards, and practical examples to guide AI in generating clean, modern, optimized, and best-practice-compliant source code across popular programming languages (especially Java, Python, JavaScript/TypeScript...).

---

## 1. Core Principles

1. **Modern & Idiomatic:** Always prioritize the latest language syntaxes and features (e.g., Java Streams instead of traditional loops when appropriate).
2. **Clean & Readable:** Use meaningful variable/function names, keep functions concise (Single Responsibility Principle), and avoid code duplication (DRY).
3. **Safe & Secure:** Handle exceptions clearly, prevent null pointers, and validate input data.
4. **Performance:** Optimize algorithms, minimize redundant memory allocation, and avoid unnecessary costly operations.

---

## 2. Java Coding Standards

### 2.1. Prohibition of Traditional Manual `for` Loops (`for(int i = 0; ... )`)
Unless there is an extreme, low-level hardware performance reason, **strictly avoid** traditional `for` loops with manual index counters `i`. Instead, use **Java Streams**, **`forEach`**, or **Enhanced For-loops (`for-each`)**.

#### ❌ ANTI-PATTERN (What NOT to do):
```java
// Using traditional for loop with a manual index counter
List<String> names = Arrays.asList("An", "Bình", "Cường");
List<String> upperNames = new ArrayList<>();
for (int i = 0; i < names.size(); i++) {
    if (names.get(i).startsWith("A")) {
        upperNames.add(names.get(i).toUpperCase());
    }
}
