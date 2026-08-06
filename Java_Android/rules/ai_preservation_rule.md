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

### Rule 1.7: Safe Refactoring Directives When Unit Tests Exist
When the user explicitly commands a code refactoring task on a module that has existing Unit Tests:
1. **Baseline Test Execution:** The AI **MUST** run existing unit tests first (`./gradlew testDebugUnitTest`) to establish a 100% green passing baseline before making any code modifications.
2. **Behavioral Contract Preservation:** True refactoring modifies internal structure without altering external behavior. Therefore, **ALL existing unit tests MUST continue to pass unmodified** after refactoring is completed.
3. **Prohibition of Test Assertion Tampering:**
   * **FORBIDDEN:** The AI **MUST NEVER** comment out failing assertions, delete failing unit test cases, or alter expected test outputs to force tests to pass after refactoring.
   * **CORRECT ACTION:** If an existing unit test fails post-refactoring, it proves the refactored code broke a behavioral contract. The AI **MUST** fix the refactored source code to satisfy the existing test, NOT alter the test.
4. **New Unit Tests for Extracted Helper Classes:** If refactoring extracts new helper classes or interfaces, the AI **MUST** generate new matching unit test suites ([unit_testability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/unit_testability_rule.md)) for the extracted components while keeping existing test suites intact.

### Rule 1.8: Mandatory End-to-End Execution & Completion Reporting Workflow
Whenever completing a coding task, feature addition, or bug fix, the AI **MUST** execute and document the following 6-step cycle:
1. **Read Requirements:** Parse feature requirements and acceptance criteria in `requirements/`.
2. **Check Rules & Templates:** Cross-reference active rules in `rules/` and benchmark code in `examples/`.
3. **Additive Code Generation:** Apply non-destructive edits, preserving legacy code 100% untouched unless explicit refactoring is requested.
4. **Unit Test Generation & Execution:** Generate matching JUnit4/Mockito test cases (`MyClassTest.java`) and run test verification (`./gradlew testDebugUnitTest`).
5. **Update Knowledge Registry:** Document new SDK or library dependencies inside `docs/`.
6. **Produce Completion Report:** Provide a concise, structured completion report detailing:
   * 📝 **Changes Made:** Summary of created or updated files.
   * 🛡️ **Rules Enforced:** Key rules applied (e.g., non-destructive editing, thread safety, null safety).
   * 🧪 **Verification Results:** Results of unit test and static analysis execution (`./gradlew lintDebug`).

### Rule 1.9: Configurable Verification Checklist & Flag Gating
The AI **MUST** inspect [verification_config.json](file:///d:/code/telua_skill/Java_Android/scripts/verification_config.json) before executing verification tasks:
* **Flag == `true`:** The AI **MUST** execute the corresponding script/command and include its pass/fail log in the completion report.
  * `run_unit_tests: true` -> Execute `./gradlew testDebugUnitTest`
  * `run_lint_check: true` -> Execute `./gradlew lintDebug`
  * `run_clean_whitespace: true` -> Execute `python Java_Android/scripts/clean_java.py`
  * `run_build_apk: true` -> Execute `./Java_Android/scripts/build_apk.sh`
* **Flag == `false`:** The AI **SKIPS** that specific verification step cleanly and marks it as `[SKIPPED (Disabled in Config)]` in the completion report, saving execution time for quick drafts or offline environments.

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

### Scenario B: User asks "Add support for a new custom event trigger while preserving the legacy `onTrigger()` event handler in `VehicleEventManager.java`."

```java
// ❌ INCORRECT AI BEHAVIOR (Mutating existing onTrigger execution path & refactoring legacy logic):
public class VehicleEventManager {
    // DANGER: AI modified existing method signature and rewrote legacy onTrigger execution path!
    public void onTrigger(int eventType, Bundle payload) {
        // AI refactored legacy switch-case into new logic, risking regressions for legacy events!
        if (eventType == NEW_CUSTOM_EVENT) {
            handleCustomEvent(payload);
        } else {
            // AI modified legacy handling code!
            processLegacyEventNewWay(eventType);
        }
    }
}

// ✅ REQUIRED AI BEHAVIOR (Preserving legacy onTrigger 100% untouched & adding dedicated new handler):
public class VehicleEventManager {
    // 1. Existing legacy onTrigger method preserved 100% untouched:
    public void onTrigger(int legacyEventType, Bundle legacyPayload) {
        // Original legacy event dispatching logic remains 100% intact
        switch (legacyEventType) {
            case EVENT_SPEED_WARNING:
                handleSpeedWarning(legacyPayload);
                break;
            default:
                Log.d(TAG, "Legacy event processed: " + legacyEventType);
                break;
        }
    }

    // 2. Additive change: Dedicated new handler added cleanly without touching legacy onTrigger logic
    public void onCustomEventTrigger(CustomEventData customEvent) {
        if (customEvent == null) {
            return;
        }
        AppLogger.d(TAG, "New custom event triggered: " + customEvent.getEventId());
        processCustomEventInternal(customEvent);
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
