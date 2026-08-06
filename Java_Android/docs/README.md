<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0
-->

# SDK & Library Integration Knowledge Registry (`docs/`)

This directory serves as a shared knowledge registry recording SDK integration patterns, library dependencies, Java imports, and risk mitigations to prevent redundant codebase scanning.

---

## 📋 Standard SDK Integration Template ([template.md](file:///d:/code/telua_skill/Java_Android/docs/template.md))

Whenever integrating a new SDK or third-party library, copy **[template.md](file:///d:/code/telua_skill/Java_Android/docs/template.md)** to create a new record inside `docs/` (e.g., `docs/payment_sdk_integration.md`).

---

## 📚 Existing Integration Records & Benchmark References

* 📄 **[template.md](file:///d:/code/telua_skill/Java_Android/docs/template.md)**: Blank standard template for recording new SDK integrations.
* 📄 **[car_volume_callback_handler_integration.md](file:///d:/code/telua_skill/Java_Android/docs/car_volume_callback_handler_integration.md)**: AOSP `CarVolumeCallbackHandler` architectural integration notes, thread safety, and multi-client dispatch rules.
* 📄 **[example_sdk_integration.md](file:///d:/code/telua_skill/Java_Android/docs/example_sdk_integration.md)**: Payment SDK integration reference showing canonical usage, build dependencies, and defensive guards.
