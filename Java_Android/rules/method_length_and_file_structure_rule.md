# Method Decomposition & File-Class Matching Rules (method_length_and_file_structure_rule.md)

This document defines mandatory standards for Java method length limits, Single Responsibility Principle (SRP) method decomposition, and Java Language Specification (JLS) file-to-class naming rules.

---

## 1. Core Rules

### Rule 1.1: Maximum Method Length Boundary (Max 35 Lines)
* **Line Limit:** No single Java method/function should exceed **35 lines of code** (excluding standard Javadoc comments).
* **Decomposition Strategy:** If a method performs multiple steps (e.g., initialization, validation, payload parsing, callback dispatching), decompose it into focused, single-responsibility `private` helper methods.
* **Benefits:** Enhances code readability, simplifies unit testing (AAA pattern), and improves IDE cognitive navigation.

### Rule 1.2: Strict 1-to-1 Class and File Name Alignment
* **JLS Compliance:** Every top-level `public` class/interface/enum **MUST** reside in its own dedicated `.java` file.
* **Exact Name Match:** The `.java` filename **MUST** match the top-level class name exactly (case-sensitive).
  * Example: `public class MyCarServiceManager` **MUST** be saved in `MyCarServiceManager.java`.
* **Prohibition of Multi-Class Files:** Never define multiple top-level `public` classes within a single `.java` file. Inner classes/interfaces should be `static inner` or moved to standalone files.

### Rule 1.3: Prohibition of Flag Arguments (Boolean Control Parameters)
* **Clean Code Chapter 3 Standard:** Never pass `boolean` flags to control method branching logic (e.g., `render(boolean isSuite)`). A boolean flag indicates that the method is performing more than one task (violating SRP).
* **Decomposition Solution:** Split the method into separate, descriptive methods called directly by the client (e.g., `renderSuite()` and `renderSingle()`).

### Rule 1.4: Avoid Negative Conditionals
* **Cognitive Readability:** Always prefer positive conditional expressions over double negatives, as negative conditionals increase cognitive load.
  * **❌ Anti-Pattern:** `if (!isNotReady) { ... }` or `if (!shouldNotProcess) { ... }`
  * **✅ Best Practice:** `if (isReady) { ... }` or `if (shouldProcess) { ... }`

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Monolithic Method & Mismatched Names):

```java
// File: Utils.java (Mismatched filename with public class CarProcessor!)

public class CarProcessor { // BAD: Filename Utils.java does not match CarProcessor!

    // BAD: Monolithic method exceeding 60 lines doing connection, parsing, logging, and callbacks!
    public void processCarData(Context context, Bundle data) {
        // Step 1: Validate (10 lines)
        if (context == null || data == null) {
            return;
        }
        String vin = data.getString("vin");
        if (vin == null || vin.isEmpty()) {
            Log.e("TAG", "Invalid VIN");
            return;
        }

        // Step 2: Parse Payload (20 lines)
        int speed = data.getInt("speed", 0);
        int rpm = data.getInt("rpm", 0);
        double temp = data.getDouble("temp", 0.0);
        // ... (more extraction)

        // Step 3: Business Logic Calculation (20 lines)
        // ... (complex math computations)

        // Step 4: UI Thread Dispatching & Database save (15 lines)
        // ... (total 65 lines monolithic block)
    }
}
```

### ✅ BEST PRACTICE (Clean Decomposition & Exact Name Alignment):

```java
// File: CarProcessor.java (Exact 1-to-1 match with public class CarProcessor)

package com.example.app;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import java.util.Objects;

/**
 * Demonstrates clean method decomposition (all methods < 20 lines)
 * and exact 1-to-1 file-to-class naming alignment.
 */
public class CarProcessor {
    private static final String TAG = CarProcessor.class.getSimpleName();

    public void processCarData(Context context, Bundle data) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(data, "Bundle data cannot be null");

        if (!validatePayload(data)) {
            Log.e(TAG, "Payload validation failed. Aborting processing.");
            return;
        }

        CarTelemetry telemetry = parseTelemetry(data);
        dispatchTelemetry(telemetry);
    }

    private boolean validatePayload(Bundle data) {
        String vin = data.getString("vin");
        return vin != null && !vin.trim().isEmpty();
    }

    private CarTelemetry parseTelemetry(Bundle data) {
        int speed = data.getInt("speed", 0);
        int rpm = data.getInt("rpm", 0);
        double temp = data.getDouble("temp", 0.0);
        return new CarTelemetry(speed, rpm, temp);
    }

    private void dispatchTelemetry(CarTelemetry telemetry) {
        Log.d(TAG, "Dispatching telemetry: " + telemetry);
        // Clean, focused single-responsibility operation
    }

    // Immutable data holder record
    public record CarTelemetry(int speed, int rpm, double temp) {}
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting Java code:
1. [ ] Does the `.java` filename match the `public class` name 100% exactly? -> **Must be Yes**.
2. [ ] Are all methods/functions kept under 35 lines of code? -> **Must be Yes**.
3. [ ] Are complex operations split into descriptive `private` helper methods? -> **Must be Yes**.
