<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0
-->

# Architectural Design & System Modeling Guide (`design/`)

This directory contains system design documents, architectural blueprints, Mermaid class diagrams, sequence flows, and API specifications for `Java_Android` features.

---

## 📐 Usage & Workflow

1. **Copy Template:** Whenever designing a new feature or refactoring architecture, copy **[template.md](file:///d:/code/telua_skill/Java_Android/design/template.md)** to create a new spec file inside `design/` (e.g., `design/user_authentication_design.md`).
2. **Include Diagrams:** Use Mermaid syntax for Class Diagrams and Asynchronous Sequence Flows.
3. **Define API Contracts:** Specify defensive method signatures, parameter limits ($\le 3$), and exception propagation.
4. **Threading & Resilience:** Detail background thread offloading, timeout boundaries (3-5s), and lifecycle cleanup.

---

## 📚 Standard Files & Benchmark References

* 📄 **[template.md](file:///d:/code/telua_skill/Java_Android/design/template.md)**: Blank standard template for system architecture design documents.
