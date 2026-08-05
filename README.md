# TeLua AI Skill & Engineering Knowledge Base

This repository serves as a centralized, multi-domain knowledge base containing **AI skill directives**, **mandatory engineering quality rules**, **architectural benchmark templates**, and **compiler warning remediation skills** for high-reliability **Java/Android** system engineering and **C/C++** software development.

---

## 📁 Repository Architecture & Master Map

```
telua_skill/
├── 🤖 Java_Android/            # Java & Android System Service Engineering Suite
│   ├── ReadMe.md              # Master manifest & system directive for Java/Android
│   ├── rules/                 # 23 mandatory quality, safety, threading & IPC rules
│   ├── examples/              # 13 gold-standard benchmark reference templates
│   ├── requirements/          # Business requirements & acceptance criteria
│   ├── design/                # Class diagrams & component architecture
│   ├── docs/                  # Integrated SDK & dependency risk registry
│   └── tools/                 # Build scripts & MCP integration configurations
└── ⚙️ C++/                    # C/C++ Compiler Warning Remediation Skill Suite
    ├── README.md              # Index of C/C++ warning fix skills
    └── *.skill                # 6 GCC/Clang warning remediation directives
```

---

## 🚀 Domain Modules Overview

### 1. 🤖 [Java/Android Engineering Suite](file:///d:/code/telua_skill/Java_Android/ReadMe.md)

Designed for mission-critical Android System Services, Automotive (AAOS) applications, and enterprise Java development:

* 📜 **23 Quality & Safety Rule Modules ([rules/](file:///d:/code/telua_skill/Java_Android/rules/))**:
  * **IPC & Binder Safety:** AIDL `oneway` callbacks, Binder thread pool offloading (<5ms), 1MB buffer limits, `RemoteCallbackList`, `linkToDeath()`, Parcelable field ordering, permission checks, `clearCallingIdentity`.
  * **Threading & Resilience:** UI thread safety, ANR prevention, `ExecutorService` lifecycle shutdown, API latency timeout wrappers.
  * **Architecture & Hygiene:** Clean Code separation (DTO vs Service), Constructor DI, 35-line method limits, Builder pattern for $\le 3$ params, defensive null safety, logger encapsulation (`AppLogger`).
  * **Static Analysis:** Android Lint (`./gradlew lintDebug`), Checkstyle, and PMD quality gates.

* 💻 **13 Benchmark Reference Templates ([examples/](file:///d:/code/telua_skill/Java_Android/examples/))**:
  * Gold-standard code benchmarks for Producer-Consumer bounded queues, `CarServiceConnection`, Observer-Strategy-Factory architecture, CarProperty subscriptions, Repository pattern, and JUnit4/Mockito test suites.

---

### 2. ⚙️ [C/C++ Warning Remediation Suite](file:///d:/code/telua_skill/C++/README.md)

Designed to eliminate strict compiler warnings (`-Werror`) in GCC/Clang builds:

* 📄 **[double-promotion.skill](file:///d:/code/telua_skill/C++/double-promotion.skill)**: Fix implicit `float` to `double` promotion in variadic calls (`printf`/`snprintf`).
* 📄 **[format-truncation.skill](file:///d:/code/telua_skill/C++/format-truncation.skill)**: Prevent string buffer overflow/truncation warnings.
* 📄 **[missing-declarations.skill](file:///d:/code/telua_skill/C++/missing-declarations.skill)**: Fix missing function prototypes and static linkage specifiers.
* 📄 **[packed.skill](file:///d:/code/telua_skill/C++/packed.skill)**: Handle unaligned memory access & struct packing attributes.
* 📄 **[unused-parameter.skill](file:///d:/code/telua_skill/C++/unused-parameter.skill)**: Resolve unused parameter warnings clean of side-effects.
* 📄 **[wrong-format.skill](file:///d:/code/telua_skill/C++/wrong-format.skill)**: Correct format specifier mismatches (`printf`/`scanf`).

---

## 🛠️ Execution Workflow for AI Agents

Whenever performing code generation, refactoring, code review, or debugging within this workspace, AI agents **MUST**:

1. **Identify Target Domain:** Determine whether the task belongs to `Java_Android` or `C++`.
2. **Consult Rule Manifest:** Check matching rules in [Java_Android/rules/](file:///d:/code/telua_skill/Java_Android/rules/) or compiler skills in [C++/](file:///d:/code/telua_skill/C++/README.md).
3. **Align with Benchmark Code:** Cross-reference structural patterns against [Java_Android/examples/](file:///d:/code/telua_skill/Java_Android/examples/).
4. **Enforce Engineering Tenets:**
   * 🧹 **Boy Scout Rule:** Leave modified files cleaner than found.
   * 🔍 **Root Cause Analysis:** Never mask symptoms or swallow exceptions silently.
   * 🧪 **Verification:** Run unit tests and static analysis verification commands before completing tasks.

---

## 📜 License, Copyright & AI Generation Notice

This repository contains code templates generated and refactored by **Gemini 3.6 Pro** in pair-programming collaboration with the maintainer. AOSP reference code patterns are adapted under the **Apache License, Version 2.0**.

For full details, see **[LICENSE.md](file:///d:/code/telua_skill/LICENSE.md)**.
