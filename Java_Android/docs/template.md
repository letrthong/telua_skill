<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0
-->

# SDK Integration Registry Template: [SDK / Library Name]

This document records the exact integration details for `[SDK / Library Name]` to prevent redundant codebase scanning in future development tasks.

---

## 1. Required Java Imports
```java
import com.example.sdk.MainSdkClient;
import com.example.sdk.SdkRequest;
import com.example.sdk.SdkCallback;
import com.example.sdk.SdkException;
```

---

## 2. Build System Dependencies
* **Gradle (`app/build.gradle`):**
  ```groovy
  dependencies {
      implementation 'com.example.sdk:library-name:1.0.0'
  }
  ```
* **AOSP Build (`Android.bp`):**
  ```blueprint
  java_libs: [
      "example-sdk-library",
  ],
  ```

---

## 3. Canonical Usage Pattern
```java
public class SdkIntegrationManager {
    private MainSdkClient mSdkClient;

    public void init(Context context) {
        // Safe ApplicationContext initialization to prevent Activity memory leaks
        this.mSdkClient = new MainSdkClient(context.getApplicationContext());
    }

    public void executeTask(String taskId, SdkCallback callback) {
        SdkRequest request = new SdkRequest(taskId);
        // Execute off Main Thread with explicit 5-second timeout boundary
        mSdkClient.executeAsync(request, 5, TimeUnit.SECONDS, callback);
    }
}
```

---

## 4. Known Risks, Pitfalls & Defensive Guards
* ⚠️ **ANR Risk:** `mSdkClient.executeSync()` is a blocking call. Never invoke on the Main UI Thread.
* ⚠️ **Permissions Required:** Declare necessary permissions in `AndroidManifest.xml`.
* ⚠️ **Timeout Requirement:** Must pass explicit 5-second timeout to avoid infinite loading state.
