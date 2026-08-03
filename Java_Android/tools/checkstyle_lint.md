# Static Analysis, Checkstyle & Android Lint Rules (checkstyle_lint_rule.md)

This document defines mandatory guidelines and CLI commands for running static analysis tools (Checkstyle, PMD, and Android Lint) to ensure zero-defect code quality.

---

## 1. Core Static Analysis Directives

### Rule 1.1: Mandatory Android Lint Verification
All generated Java/Android code **MUST** pass Android Lint checks without errors or high-severity warnings. Before finalizing code, run the Gradle Lint command:
* **Windows Command:** `.\gradlew lintDebug`
* **macOS / Linux Command:** `./gradlew lintDebug`
* **HTML Report Output:** `app/build/reports/lint-results-debug.html`

### Rule 1.2: Checkstyle Compliance
All Java source files must comply with Checkstyle rules:
* **Indentation & Formatting:** 4 spaces for Java, no tab characters.
* **Modifier Order:** JLS standard (`public static final`).
* **Naming Conventions:** Class (`PascalCase`), Method/Field (`lowerCamelCase` with `m`/`s` prefixes), Constant (`UPPER_SNAKE_CASE`).
* **Imports:** No wildcard `*` imports.

### Rule 1.3: PMD & SpotBugs Quality Gates
* **Unused Code:** Zero unused imports, local variables, or private methods.
* **Empty Blocks:** Zero empty `catch`, `if`, or `for` blocks.

---

## 2. Command Execution Guide

### Running Android Lint via Terminal
```bash
# Run Android Lint analysis on debug build
./gradlew lintDebug

# Run full project lint check
./gradlew lint
```

### Running Checkstyle via Terminal
```bash
# Run Checkstyle task via Gradle
./gradlew checkstyle
```

---

## 3. Recommended Gradle Configuration (`app/build.gradle`)

```groovy
android {
    lint {
        // Stop build on critical errors
        abortOnError true
        // Generate HTML & XML reports
        htmlReport true
        htmlOutput file("build/reports/lint-report.html")
        xmlReport true
        // Enable API compatibility check
        checkDependencies true
    }
}
```

---

## 4. AI Self-Correction & Verification Checklist

Before finalizing code changes:
1. [ ] Does the code pass `./gradlew lintDebug` without critical errors? -> **Must be Yes**.
2. [ ] Are wildcard imports avoided (`import java.util.*`)? -> **Must be Yes**.
3. [ ] Are all Checkstyle modifier ordering rules respected (`public static final`)? -> **Must be Yes**.
