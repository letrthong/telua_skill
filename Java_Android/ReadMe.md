# AI Mandatory Engineering Standards & Rules Manifest for Java/Android

This file serves as the primary system directive and index manifest for AI code generation, refactoring, and review within the `Java_Android` workspace.

## 🚨 System Directive for AI
Whenever generating, reviewing, or refactoring Java/Android code, the AI **MUST** strictly load, enforce, and verify all 14 rule modules defined in the `rules/` directory and the core `coding_style.md` guide.

---

## 📚 Master Index of Rule Modules

### 1. Architectural & Structural Rules
* 📄 **[coding_style.md](file:///d:/code/telua_skill/Java_Android/coding_style.md)**: Core Java engineering standards, modern syntax, immutability, and English documentation.
* 📄 **[design_pattern_architecture_rule.md](file:///d:/code/telua_skill/Java_Android/rules/design_pattern_architecture_rule.md)**: MVC/MVVM separation, Strategy, Observer, Factory, and Dependency Inversion.
* 📄 **[encapsulation_rule.md](file:///d:/code/telua_skill/Java_Android/rules/encapsulation_rule.md)**: Strict private member field access, prohibition of public fields, defensive copying.
* 📄 **[lifecycle_init_rule.md](file:///d:/code/telua_skill/Java_Android/rules/lifecycle_init_rule.md)**: Lightweight constructors, prohibition of overridable methods in constructors, explicit `init()` / `release()` lifecycle pattern.

### 2. Safety, Threading & Resource Management Rules
* 📄 **[ui_thread_rule.md](file:///d:/code/telua_skill/Java_Android/rules/ui_thread_rule.md)**: Main UI thread safety, ANR prevention, background execution, and thread-safe View updates.
* 📄 **[executor_shutdown_rule.md](file:///d:/code/telua_skill/Java_Android/rules/executor_shutdown_rule.md)**: Mandatory shutdown of `Executor` / `ExecutorService` thread pools in lifecycle teardowns.
* 📄 **[resource_leak_rule.md](file:///d:/code/telua_skill/Java_Android/rules/resource_leak_rule.md)**: `try-with-resources` for `AutoCloseable`, SQLite Cursor closing, symmetric `BroadcastReceiver` unregistering.
* 📄 **[singleton_thread_safety_rule.md](file:///d:/code/telua_skill/Java_Android/rules/singleton_thread_safety_rule.md)**: Bill Pugh & volatile double-checked locking for thread-safe singletons, `ApplicationContext` usage.

### 3. Code Hygiene, Null Safety & Quality Rules
* 📄 **[naming_rule.md](file:///d:/code/telua_skill/Java_Android/rules/naming_rule.md)**: AOSP field prefixes (`m`/`s`), `UPPER_SNAKE_CASE` constants, JLS modifier ordering (`public static final`).
* 📄 **[handler_rule.md](file:///d:/code/telua_skill/Java_Android/rules/handler_rule.md)**: Defensive null safety guards on chained getters, mandatory `removeCallbacksAndMessages(null)` cleanup.
* 📄 **[for_loop_rule.md](file:///d:/code/telua_skill/Java_Android/rules/for_loop_rule.md)**: Prohibition of manual `for(int i=0;...)` loops; mandatory use of enhanced `for(Item item : list)` or Java Streams.
* 📄 **[exception_handling_rule.md](file:///d:/code/telua_skill/Java_Android/rules/exception_handling_rule.md)**: Prohibition of empty catch blocks, catching specific exception types, preserving exception cause chaining.
* 📄 **[log_rule.md](file:///d:/code/telua_skill/Java_Android/rules/log_rule.md)**: Prohibition of `System.out.println()` / `e.printStackTrace()`, dynamic `TAG = MyClass.class.getSimpleName()`, PII data protection, `BuildConfig.DEBUG` guarding.
* 📄 **[unit_testability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/unit_testability_rule.md)**: Constructor Dependency Injection, abstracting static/system calls, Arrange-Act-Assert (AAA) JUnit testing pattern.
* 📄 **[checkstyle_lint_rule.md](file:///d:/code/telua_skill/Java_Android/rules/checkstyle_lint_rule.md)**: Checkstyle formatting, PMD quality gates, and Gradle Android Lint verification (`./gradlew lint`).

---

## 🛠️ Static Analysis Tools & Gradle Verification Commands

All generated Java code **MUST** pass static analysis checks before code delivery.

### 1. Run Android Lint via Gradle Terminal
```bash
# macOS / Linux
./gradlew lintDebug

# Windows PowerShell
.\gradlew lintDebug
```

### 2. Run Checkstyle & PMD Static Checks
```bash
# Checkstyle code formatting scan
./gradlew checkstyle

# PMD code quality scan
./gradlew pmd
```
