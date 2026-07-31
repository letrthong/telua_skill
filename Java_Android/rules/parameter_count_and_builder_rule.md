# Parameter Limit & Builder Pattern Rules (parameter_count_and_builder_rule.md)

This document defines mandatory engineering standards for limiting method parameter counts (Max 3 parameters) and enforcing the Parameter Object DTO / Builder Pattern for complex signatures.

---

## 1. Core Rules

### Rule 1.1: Maximum Parameter Limit Boundary (Max 3 Parameters)
* **Parameter Boundary:** No single Java method/function should take more than **3 parameters** (`public void doSomething(ParamA a, ParamB b, ParamC c)`).
* **Rationale:** Methods with 4+ parameters are difficult to read, error-prone due to argument swapping, and hard to test.

### Rule 1.2: Mandatory Parameter Object / DTO Refactoring
* If a operation requires 4 or more arguments, encapsulate related parameters into an immutable Parameter Object, DTO, or Java Record.
  * **Bad:** `public void createUser(String name, String email, int age, boolean isAdmin, String department)`
  * **Good:** `public void createUser(UserCreationRequest request)`

### Rule 1.3: Mandatory Builder Pattern for Complex Configurations
* For configuration objects or data models with optional and mandatory parameters, provide a static nested `Builder` class (matching Android SDK standards like `UserCreationRequest.Builder`).

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Excessive Parameters - 5 arguments):

```java
// BAD: Method takes 5 parameters, easy to swap String arguments!
public void registerVehicle(String vin, String model, int year, double mileage, boolean isElectric) {
    // Monolithic parameter list
}
```

### ✅ BEST PRACTICE (Clean Builder & Parameter DTO):

```java
package com.example.app;

import java.util.Objects;

/**
 * Demonstrates clean parameter encapsulation using Builder Pattern and Java Records.
 */
public class VehicleRegistrationManager {

    // Clean API with exactly 1 Parameter Object
    public void registerVehicle(VehicleRegistrationRequest request) {
        Objects.requireNonNull(request, "VehicleRegistrationRequest cannot be null");
        
        String vin = request.vin();
        String model = request.model();
        int year = request.year();
        double mileage = request.mileage();
        boolean isElectric = request.isElectric();

        // Process registration with validated parameters...
    }

    // Immutable Parameter Object Record with Builder
    public record VehicleRegistrationRequest(
            String vin,
            String model,
            int year,
            double mileage,
            boolean isElectric
    ) {
        public VehicleRegistrationRequest {
            Objects.requireNonNull(vin, "VIN cannot be null");
            Objects.requireNonNull(model, "Model cannot be null");
        }

        public static class Builder {
            private String mVin;
            private String mModel;
            private int mYear;
            private double mMileage;
            private boolean mIsElectric;

            public Builder setVin(String vin) {
                mVin = vin;
                return this;
            }

            public Builder setModel(String model) {
                mModel = model;
                return this;
            }

            public Builder setYear(int year) {
                mYear = year;
                return this;
            }

            public Builder setMileage(double mileage) {
                mMileage = mileage;
                return this;
            }

            public Builder setElectric(boolean electric) {
                mIsElectric = electric;
                return this;
            }

            public VehicleRegistrationRequest build() {
                return new VehicleRegistrationRequest(mVin, mModel, mYear, mMileage, mIsElectric);
            }
        }
    }
}
```

---

## 3. AI Self-Correction Checklist

Before emitting Java code:
1. [ ] Are all methods restricted to 3 or fewer parameters? -> **Must be Yes**.
2. [ ] Are methods requiring 4+ parameters refactored into a Parameter Object or Builder Pattern? -> **Must be Yes**.
