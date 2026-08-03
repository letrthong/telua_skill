# Objects, DTO, DAO, and Business Logic Separation Rule (objects_and_data_structures_rule.md)

This document defines mandatory standards for separating DTOs (Data Transfer Objects), DAOs (Data Access Objects), Data Mappers/Parsers, and Business Logic Processors based on Clean Code Chapter 6 principles.

---

## 1. Core Rules

### Rule 1.1: Mandatory 3-Tier Decoupled Architecture (DTO vs. DAO vs. Business Logic)
To enable parallel team development, independent unit testing, and technology swappability, code **MUST** separate responsibilities into 3 distinct, decoupled layers:

1. **DTO Layer (Data Transfer Object / Record / Model):**
   * **Role:** "Data Container / Parcel". Pure immutable data structure containing fields and getters only.
   * **Restrictions:** **MUST NOT** contain SQL statements, database operations, network I/O, or business decision algorithms.
2. **DAO Layer (Data Access Object / Repository):**
   * **Role:** "Database Storekeeper". Encapsulates direct database queries (Room DB, SQLite, Shared Preferences, Realm).
   * **Restrictions:** **MUST NOT** contain UI code, network calls, or business decision logic. Communicates exclusively using DTOs.
3. **Business Logic Layer (Evaluator / Service / Processor):**
   * **Role:** "Business Decision Maker". Executes domain rules, calculations, and decision algorithms operating on DTOs.
   * **Restrictions:** **MUST NOT** execute direct SQL queries or hardcode database technologies.

### Rule 1.2: Law of Demeter (Principle of Least Knowledge)
* **Prohibition of "Train Wrecks":** A method should only invoke methods of direct dependencies, method parameters, or instantiated local objects. Do NOT navigate object graphs through long getter chains (`a.getB().getC().getD()`).
* **Encapsulation Solution:** Delegate behavior to the containing object (e.g., `user.getZipCode()` instead of `user.getAddress().getCity().getZipCode()`).

### Rule 1.3: Prefer Dedicated Value Objects & Encapsulated Boundary Conditions
* **Primitive Obsession Avoidance:** Wrap raw primitive types (e.g., `String vin`, `int speed`) into dedicated immutable Value Objects (or Java `record`) when domain rules exist.
* **Encapsulating Boundaries:** Encapsulate index calculations and boundary limit checks inside dedicated methods or domain models rather than scattering math offsets throughout business code.

> [!IMPORTANT]
> **Android Desugaring & Java Compatibility Note:**
> When using modern Java features like Java `record` (Java 16+) or Java Stream APIs on Android apps targeting `minSdkVersion` < 34, ensure **Core Library Desugaring** is enabled in `app/build.gradle`:
> ```groovy
> compileOptions {
>     coreLibraryDesugaringEnabled true
>     sourceCompatibility JavaVersion.VERSION_17
>     targetCompatibility JavaVersion.VERSION_17
> }
> ```

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Tightly Coupled Hybrid Class Mixing Data, SQL, and Business Logic):

```java
// BAD: 1 class mixes data fields, raw SQLite SQL queries, AND business overheat alerts!
public class UserVehicle {
    private String mVin;
    private double mTempCelsius;

    // Bad 1: Direct SQL queries mixed inside Data DTO!
    public void saveToDatabase(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put("vin", mVin);
        cv.put("temp", mTempCelsius);
        db.insert("vehicles", null, cv);
    }

    // Bad 2: Business alert logic mixed inside Data DTO!
    public boolean checkAndAlertOverheat() {
        if (mTempCelsius > 105.0) {
            Log.w("TAG", "Vehicle " + mVin + " is overheating!");
            return true;
        }
        return false;
    }
}
```

### ✅ BEST PRACTICE (Clean Decoupled Architecture: DTO + DAO + Business Processor):

```java
package com.example.app;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Objects;

/**
 * Demonstrates 100% Decoupled Architecture:
 * 1. VehicleDto: Pure DTO (Data Transfer Object).
 * 2. VehicleDao: Pure DAO (Data Access Object for SQLite/Room).
 * 3. VehicleHealthProcessor: Pure Business Logic Evaluator.
 */
public class ObjectsAndDataStructuresExample {

    /**
     * 1. DTO (Data Transfer Object): Pure Immutable Data Holder.
     */
    public record VehicleDto(String vin, int speed, double tempCelsius) {
        public VehicleDto {
            Objects.requireNonNull(vin, "VIN cannot be null");
        }
    }

    /**
     * 2. DAO (Data Access Object): Database Storekeeper (Encapsulates SQLite/Room I/O).
     */
    public static class VehicleDao {
        private static final String TABLE_VEHICLES = "vehicles";

        public void insertVehicle(SQLiteDatabase db, VehicleDto vehicle) {
            Objects.requireNonNull(db, "SQLiteDatabase cannot be null");
            Objects.requireNonNull(vehicle, "VehicleDto cannot be null");

            ContentValues values = new ContentValues();
            values.put("vin", vehicle.vin());
            values.put("speed", vehicle.speed());
            values.put("temp_celsius", vehicle.tempCelsius());

            db.insert(TABLE_VEHICLES, null, values);
        }
    }

    /**
     * 3. Business Logic Processor: Pure Domain Rules & Decision Maker.
     */
    public static class VehicleHealthProcessor {
        private static final String TAG = VehicleHealthProcessor.class.getSimpleName();
        private static final double OVERHEAT_THRESHOLD_CELSIUS = 105.0;

        public boolean isOverheating(VehicleDto vehicle) {
            Objects.requireNonNull(vehicle, "VehicleDto cannot be null");
            return vehicle.tempCelsius() > OVERHEAT_THRESHOLD_CELSIUS;
        }

        public void evaluateVehicleHealth(VehicleDto vehicle) {
            Objects.requireNonNull(vehicle, "VehicleDto cannot be null");

            if (isOverheating(vehicle)) {
                Log.w(TAG, "ALERT: Vehicle " + vehicle.vin() + " is overheating! Temp: " + vehicle.tempCelsius() + "C");
            } else {
                Log.d(TAG, "Vehicle " + vehicle.vin() + " is operating safely.");
            }
        }
    }
}
```

---

## 3. AI Self-Correction Checklist

Before emitting Java code:
1. [ ] Is DTO 100% clean of SQL queries and business logic? -> **Must be Yes**.
2. [ ] Is DAO isolated to database CRUD operations? -> **Must be Yes**.
3. [ ] Is Business Logic decoupled from database technologies? -> **Must be Yes**.
