<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# AI Code Preservation & Non-Destructive Editing Rule (ai_preservation_rule.md)

This document defines mandatory directives for AI code generation, modification, and feature additions to ensure AI agents preserve existing codebase stability and refrain from performing unsolicited modifications or destructive refactoring.

---

## 1. Core Directives

### Rule 1.1: Strict Non-Destructive Editing Policy (Additive-First)
Whenever an AI agent receives a request to implement a new feature, add a helper method, or fix a specific bug:
* **MANDATORY:** The AI **MUST NOT** delete, rewrite, or alter existing working code unless explicitly instructed by the user.
* **MANDATORY:** Prefer additive code changes (adding new methods, extending classes, or appending new branches) over mutating working legacy logic.

### Rule 1.2: Explicit Trigger Requirement for Code Inspection & Refactoring
* **STRICT MANDATE:** The AI **MUST ONLY** inspect, audit, or refactor existing code when the user provides an **EXPLICIT COMMAND** to do so (e.g., *"Refactor this class"*, *"Optimize method X"*, *"Clean up code smells in file Y"*, or *"Review against rule Z"*).
* **FORBIDDEN:** Without an explicit user command, the AI **MUST NOT** refactor, reformat, clean up, or alter any existing code blocks, even if the code contains known smells, deprecated methods, or non-standard formatting.
* **GOLDEN RULE:** *"If the user did not explicitly ask you to refactor or change it, leave it 100% untouched."*

### Rule 1.3: Preservation of Existing API Contracts & Method Signatures
* **FORBIDDEN:** Altering existing public/protected method signatures, changing return types, or modifying parameter orders of existing methods without an explicit user instruction.
* Modifying established signatures risks breaking external callers, dependent modules, and unit tests across the workspace.

### Rule 1.4: Strict Scope Bounding
* Confine code edits strictly to the target file, class, or method specified by the user.
* **FORBIDDEN:** Modifying adjacent files, global configuration files, or unrelated utility classes unless required to resolve compile errors directly caused by the requested addition.

### Rule 1.5: Mandatory Workspace Rule Compliance for Newly Added Code
While existing legacy code is preserved untouched, all **newly added code** (new methods, extended classes, new fields, or logic branches) **MUST STILL STRICTLY COMPLY** with all 24 workspace engineering rules:
* **Naming Conventions:** Must use `m`/`s` field prefixes and JLS modifier ordering ([naming_rule.md](file:///d:/code/telua_skill/Java_Android/rules/naming_rule.md)).
* **Clean Code Limits:** Must limit new methods to $\le 35$ lines and method parameters to $\le 3$ ([parameter_count_and_builder_rule.md](file:///d:/code/telua_skill/Java_Android/rules/parameter_count_and_builder_rule.md) & [method_length_and_file_structure_rule.md](file:///d:/code/telua_skill/Java_Android/rules/method_length_and_file_structure_rule.md)).
* **Safety & Threading:** Must enforce thread safety, non-blocking execution, and defensive null safety ([ui_thread_rule.md](file:///d:/code/telua_skill/Java_Android/rules/ui_thread_rule.md) & [handler_rule.md](file:///d:/code/telua_skill/Java_Android/rules/handler_rule.md)).
* **Logging & Testing:** Must use `AppLogger` ([log_rule.md](file:///d:/code/telua_skill/Java_Android/rules/log_rule.md)) and generate corresponding JUnit4/Mockito unit tests ([unit_testability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/unit_testability_rule.md)).

### Rule 1.6: Harmonization with Controlled Boy Scout Rule
* The traditional "Boy Scout Rule" (leaving the codebase cleaner than found) is strictly bounded to **newly added code** and **explicit user refactoring commands**.
* When adding a new feature to an existing file, apply clean code standards strictly to your newly written code. Do NOT refactor surrounding legacy code blocks unless the user explicitly commands a refactoring task.

---

## 2. Examples: Correct vs. Incorrect AI Modification Behavior

### Scenario: User asks "Add a method to calculate battery level percentage in `VehicleBatteryManager.java`."

```java
// ❌ INCORRECT AI BEHAVIOR (Unsolicited rewrite of existing getStatus method):
public class VehicleBatteryManager {
    // AI deleted existing legacy code and refactored getStatus() without permission!
    public String getStatus() {
        return "OPTIMAL"; // AI rewrote legacy logic!
    }

    public int getBatteryPercentage() {
        return 85;
    }
}

// ✅ REQUIRED AI BEHAVIOR (Additive-first preservation of existing code):
public class VehicleBatteryManager {
    // Existing legacy code preserved 100% untouched:
    public String getStatus() {
        // Original legacy logic remains intact even if suboptimal
        return mLegacyStatusSensor != null ? mLegacyStatusSensor.readRaw() : "UNKNOWN";
    }

    // Additive change: New requested method appended cleanly
    public int getBatteryPercentage() {
        return mCachedBatteryPercentage;
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before finalizing any code modification:
1. [ ] Did I preserve all existing method signatures and working logic 100% intact? -> **Must be Yes**.
2. [ ] Did I refrain from deleting or refactoring untouched code blocks? -> **Must be Yes**.
3. [ ] Are my changes purely additive or confined strictly to the requested edit scope? -> **Must be Yes**.
4. [ ] Did I avoid modifying adjacent files that the user did not ask to touch? -> **Must be Yes**.
