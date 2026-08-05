<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# External Code & Documentation Reference Rules (external_reference_rule.md)

This document defines mandatory guidelines for AI code search, inspection, and architectural benchmarking when provided with external repository or documentation URLs (e.g., AOSP cs.android.com, googlesource.com, GitHub repositories, or official SDK docs).

---

## 1. Core Rules

### Rule 1.1: Automatic Fetch & Inspection Protocol
Whenever provided with an external URL (directory path or exact file link):
1. **Fetch Content:** Use web reading tools (`read_url_content` or `browser_subagent`) to fetch the source code or documentation.
2. **Target Symbol Localization:** Search for the specific class, interface, method, or field mentioned (e.g., `LocationListener`, `GnssStatus.Callback`, or `mLocationListener`).
3. **Refactoring to Workspace Standards:** Adapt and refactor the external reference code to strictly comply with all 17 `Java_Android` workspace rules (naming conventions, null safety, timeout boundaries, idempotent lifecycle).

### Rule 1.2: Mandatory Integration Registry Log
Whenever external AOSP/SDK/GitHub code patterns are adopted:
* The AI **MUST** log the reference URL, required imports, build dependencies (`build.gradle`/`Android.bp`), and safety notes into a markdown file in the **`docs/`** directory.

---

## 2. Standard Reference Inspection Examples

### Example A: AOSP Source Reference (cs.android.com)
```markdown
# Reference: Android GPS Location Service (LocationListener)

* **Source URL:** `https://cs.android.com/android/platform/superproject/+/main:frameworks/base/location/java/android/location/`
* **Target File:** `frameworks/base/location/java/android/location/LocationListener.java`
* **Adopted Pattern:** GPS Location Listener update handling (`mLocationListener`).
```

### Example B: GitHub Repository Reference (github.com)
```markdown
# Reference: Android Architecture Samples Repository

* **Source URL:** `https://github.com/android/architecture-samples/tree/main/app/src/main/java/com/example/android/architecture/blueprints/todoapp/`
* **Target File:** `data/source/TasksRepository.kt` / `java`
* **Adopted Pattern:** Clean Architecture Repository & Offline-first Data Cache synchronization.
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting code inspired by external references:
1. [ ] Was the external source code (AOSP or GitHub) fetched and verified using URL tools? -> **Must be Yes**.
2. [ ] Was the code refactored to apply `m`/`s` field prefixes and JLS modifier ordering? -> **Must be Yes**.
3. [ ] Are thread safety (`synchronized`) and null safety guards applied? -> **Must be Yes**.
4. [ ] Were reference details logged into `docs/`? -> **Must be Yes**.
