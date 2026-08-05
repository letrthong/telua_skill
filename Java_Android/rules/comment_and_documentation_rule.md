<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# Comment and Documentation Rules (comment_and_documentation_rule.md)

This document defines mandatory standards for self-documenting code, Javadoc comments, and the elimination of redundant/noise comments based on Clean Code Chapter 4 principles.

---

## 1. Core Rules

### Rule 1.1: Self-Documenting Code Over Noise Comments
* **Code Expressiveness:** Express intent through clear, descriptive method and variable names rather than adding redundant comments.
* **Prohibition of Redundant Comments:** Never write comments that merely restate what the code clearly does.
  * **Bad:** `i++; // Increments i`
  * **Bad:** `// Default Constructor` above `public MyClass() {}`
  * **Bad:** `// Set age` above `public void setAge(int age)`

### Rule 1.2: Mandatory "WHY" Comments for Workarounds and Complex Logic
* **Rationale Documentation:** Write comments to explain **WHY** a non-obvious workaround, trade-off, or complex algorithm was chosen (e.g. AOSP VHAL timing workarounds or hardware specific timeouts).

### Rule 1.3: Mandatory Javadoc for Public Interfaces & Methods
* **Public APIs:** All `public` classes, interfaces, and public methods **MUST** have clean Javadoc comments outlining purpose, parameters, return values, and exceptions.
* **Language Requirement:** All code comments and Javadoc **MUST** be written in English.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Noise Comments & Redundant Explanation):

```java
// BAD: Noise comments restating the obvious, useless Javadoc, non-English comments!
public class User {
    // Tên người dùng (Bad: Redundant comment!)
    private String mName;

    // Constructor (Bad: Redundant comment!)
    public User() {}

    // Get name (Bad: Redundant comment!)
    public String getName() {
        return mName; // Return name
    }
}
```

### ✅ BEST PRACTICE (Self-Documenting Code & Explanatory Javadoc):

```java
package com.example.app;

import java.util.Objects;

/**
 * Manages user profile information and validation rules.
 */
public class UserProfileManager {

    private String mUserName;

    /**
     * Updates the user profile name after sanitizing leading and trailing spaces.
     *
     * @param userName Non-null raw user name string.
     * @throws IllegalArgumentException if the provided name is empty.
     */
    public void updateUserName(String userName) {
        Objects.requireNonNull(userName, "UserName cannot be null");
        
        String trimmed = userName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("UserName cannot be empty");
        }

        // NOTE: We preserve casing to match legacy identity provider requirements.
        mUserName = trimmed;
    }

    public String getUserName() {
        return mUserName;
    }
}
```

---

## 3. AI Self-Correction Checklist

Before emitting Java code:
1. [ ] Are redundant noise comments removed? -> **Must be Yes**.
2. [ ] Are public classes and methods documented with Javadoc in English? -> **Must be Yes**.
3. [ ] Are non-obvious decisions explained with "WHY" comments? -> **Must be Yes**.
