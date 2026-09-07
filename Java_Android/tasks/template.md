# Task Execution & Progress Template (`tasks/template.md`)

* **Task ID:** `TASK-XX`
* **Feature Name:** [Name of Feature / Module]
* **Status:** `PENDING` / `IN_PROGRESS` / `COMPLETED`
* **Requirements Spec:** [requirements/my_feature.md](file:///d:/code/telua_skill/Java_Android/requirements/)
* **Architecture Blueprint:** [design/my_feature.md](file:///d:/code/telua_skill/Java_Android/design/)

---

## 1. Feature Objective & Scope
Brief 1-2 sentence description of what this task accomplishes and why it is being built.

---

## 2. Step-by-Step Execution Checklist

### Phase 1: Requirements & Architecture Alignment
* [ ] **Subtask 1.1:** Review functional requirements & BDD acceptance criteria in `requirements/`.
* [ ] **Subtask 1.2:** Inspect component diagrams, sequence flows, and threading models in `design/`.
* [ ] **Subtask 1.3:** Verify developer sign-off on architectural trade-offs before generating code.

### Phase 2: Domain Data Structures & Contracts
* [ ] **Subtask 2.1:** Implement immutable DTOs / Java Records separating data from behavior ([objects_and_data_structures_rule.md](file:///d:/code/telua_skill/Java_Android/rules/objects_and_data_structures_rule.md)).
* [ ] **Subtask 2.2:** Define public service / repository interfaces with clean abstraction boundaries.

### Phase 3: Core Implementation & Thread Safety
* [ ] **Subtask 3.1:** Implement service logic applying strict thread safety ([thread_safety_concurrency_rule.md](file:///d:/code/telua_skill/Java_Android/rules/thread_safety_concurrency_rule.md)).
* [ ] **Subtask 3.2:** Apply idempotent lifecycle `init()` and `release()` pattern ([lifecycle_init_rule.md](file:///d:/code/telua_skill/Java_Android/rules/lifecycle_init_rule.md)).
* [ ] **Subtask 3.3:** Ensure zero magic numbers/strings ([magic_number_immutability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/magic_number_immutability_rule.md)).
* [ ] **Subtask 3.4:** Keep methods $\le 35$ lines and parameters $\le 3$ ([method_length_and_file_structure_rule.md](file:///d:/code/telua_skill/Java_Android/rules/method_length_and_file_structure_rule.md)).

### Phase 4: Unit Testing & Verification
* [ ] **Subtask 4.1:** Generate matching JUnit4 + Mockito unit test suite following Arrange-Act-Assert (AAA) ([unit_testability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/unit_testability_rule.md)).
* [ ] **Subtask 4.2:** Run unit test suite: `./gradlew testDebugUnitTest`.
* [ ] **Subtask 4.3:** Run static analysis verification: `./gradlew lintDebug`.

### Phase 5: Documentation & Knowledge Registry
* [ ] **Subtask 5.1:** Update `docs/` if new external libraries, SDKs, or permissions were introduced.
* [ ] **Subtask 5.2:** Update [tasks/CHECKLIST.md](file:///d:/code/telua_skill/Java_Android/tasks/CHECKLIST.md) to mark task as `COMPLETED`.

---

## 3. Engineering Rules Enforced
* [ ] [naming_rule.md](file:///d:/code/telua_skill/Java_Android/rules/naming_rule.md) (AOSP `m`/`s` prefixes, JLS modifier ordering)
* [ ] [log_rule.md](file:///d:/code/telua_skill/Java_Android/rules/log_rule.md) (`AppLogger` usage, zero `System.out.println`)
* [ ] [ui_thread_rule.md](file:///d:/code/telua_skill/Java_Android/rules/ui_thread_rule.md) (Zero heavy I/O on UI thread, ANR prevention)
* [ ] [thread_safety_concurrency_rule.md](file:///d:/code/telua_skill/Java_Android/rules/thread_safety_concurrency_rule.md) (Volatile Local Copy, no alien calls under lock)

---

## 4. Deliverables & Verification Summary

| Output Type | File Path | Status |
| :--- | :--- | :---: |
| **Source Code** | `com.example.app.feature.MyClass.java` | PENDING |
| **Unit Test Suite** | `com.example.app.feature.MyClassTest.java` | PENDING |
| **Verification Output** | Unit Test: 0 failed / Lint: 0 errors | PENDING |
