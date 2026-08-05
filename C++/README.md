# C/C++ AI Skill & Warning Remediation Repository

This directory contains specialized AI skill modules designed to diagnose, explain, and resolve common GCC/Clang compiler warnings and build errors in C/C++ development.

---

## 📚 Master Index of C/C++ Skills

* 📄 **[double-promotion.skill](file:///d:/code/telua_skill/C++/double-promotion.skill)**: Remediation for `-Werror=double-promotion` when passing `float` variables to variadic functions (`printf`, `snprintf`, etc.) without explicit `double` casting.
* 📄 **[format-truncation.skill](file:///d:/code/telua_skill/C++/format-truncation.skill)**: Remediation for `-Werror=format-truncation` in string formatting functions (`snprintf`/`sprintf`) where destination buffers may be truncated.
* 📄 **[missing-declarations.skill](file:///d:/code/telua_skill/C++/missing-declarations.skill)**: Remediation for `-Werror=missing-declarations` by adding proper function prototypes, header inclusions, or `static` internal linkage specifiers.
* 📄 **[packed.skill](file:///d:/code/telua_skill/C++/packed.skill)**: Remediation for structure packing, unaligned memory accesses, and compiler alignment warnings (`__attribute__((packed))`).
* 📄 **[unused-parameter.skill](file:///d:/code/telua_skill/C++/unused-parameter.skill)**: Remediation for `-Werror=unused-parameter` using `(void)param` casts or `[[maybe_unused]]` / `__attribute__((unused))` annotations.
* 📄 **[wrong-format.skill](file:///d:/code/telua_skill/C++/wrong-format.skill)**: Remediation for format specifier mismatches in `printf`/`scanf` style calls (e.g. `%d` vs `%size_t` / `%uint32_t`).

---

## 🛠️ Usage Guidelines for AI Agents

1. **Detect Warning/Error:** Match compiler output against target error flags (e.g., `-Wdouble-promotion`, `-Wformat-truncation`).
2. **Apply Skill Directive:** Load the corresponding `.skill` file to extract exact code transformation patterns.
3. **Verify Fix:** Re-run project compilation to ensure zero compiler warnings or errors remain.
