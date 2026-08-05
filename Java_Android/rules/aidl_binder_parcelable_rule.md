# AIDL, Binder IPC & Parcelable Rules (aidl_binder_parcelable_rule.md)

This document defines mandatory standards for implementing Android IPC (Inter-Process Communication) via AIDL interfaces, Binder transactions, and Parcelable data transfer objects in Java/Android system service development.

---

## 1. AIDL Interface Definition Rules

### Rule 1.1: AIDL Interface Naming Convention
* **Interface files:** MUST use `I` prefix followed by `PascalCase` service name (e.g., `ICarDataService.aidl`, `IVehicleEventCallback.aidl`).
* **Callback interfaces:** MUST use the `I[Feature]Callback` suffix pattern (e.g., `IVehicleEventCallback.aidl`).
* **Package declaration:** MUST match the Java package structure exactly.

### Rule 1.2: `oneway` Keyword Usage
* **`oneway` MUST be used** for all callback/notification methods dispatched from server → client to prevent blocking the server's Binder thread while waiting for client acknowledgment.
* **`oneway` MUST NOT be used** for methods that return a value or where the caller must wait for a result.

```aidl
// ✅ CORRECT: oneway for server→client notification (non-blocking)
interface IVehicleEventCallback {
    oneway void onSpeedChanged(in VehicleData data);
    oneway void onServiceDisconnected(int reason);
}

// ✅ CORRECT: sync (no oneway) when return value is needed
interface ICarDataService {
    VehicleData getCurrentSpeed();                         // Returns value — sync
    void registerCallback(IVehicleEventCallback callback); // Registers only — can be sync
    oneway void unregisterCallback(IVehicleEventCallback callback); // Fire-and-forget — oneway
}
```

### Rule 1.3: AIDL Nullability Annotations
* Use `in`, `out`, or `inout` directional tags for all non-primitive parameters.
* Mark nullable object parameters explicitly with `@nullable` annotation in AIDL files.

---

## 2. Binder Thread Safety Rules

### Rule 2.1: NEVER Block the Binder Thread Pool
The Android Binder thread pool has a fixed maximum of **15-16 threads**. Blocking a Binder thread with long-running operations (I/O, SDK calls, sleep) starves other IPC calls system-wide, causing ANR.

* **MANDATORY:** Offload all heavy processing inside AIDL stub implementations to a dedicated `ExecutorService` background thread immediately.
* **MANDATORY:** Return control to the Binder thread within **5ms**.

```java
// ❌ ANTI-PATTERN: Blocking Binder thread directly
@Override
public VehicleData getCurrentSpeed() throws RemoteException {
    return mSdk.fetchSpeedFromHardware(); // DANGER: May block 500ms+ on Binder thread!
}

// ✅ REQUIRED: Offload to background executor, return cached value
@Override
public VehicleData getCurrentSpeed() throws RemoteException {
    return mCachedSpeed; // Return pre-fetched cached value instantly
}
```

### Rule 2.2: Binder Transaction Size Limit
* A single Binder transaction buffer is **limited to 1MB** shared across all pending transactions in the process.
* **FORBIDDEN:** Passing large bitmaps, large byte arrays, or bulk data objects directly through Binder transactions.
* **REQUIRED:** For large data, use `ParcelFileDescriptor` (shared memory / file descriptor passing) instead of embedding data in the `Parcel`.

### Rule 2.3: Exception Handling in AIDL Stubs
* Catch all exceptions inside AIDL stub method implementations. An uncaught exception thrown across a Binder transaction will crash the **calling** process.
* Wrap results in error-code response patterns or throw only declared `RemoteException`.

```java
@Override
public int getStatus() throws RemoteException {
    try {
        return mInternalService.queryStatus();
    } catch (IllegalStateException e) {
        Log.e(TAG, "Failed to query status", e);
        return STATUS_ERROR; // Never let exception propagate across Binder boundary unchecked
    }
}
```

---

## 3. Binder Death Recipient Rules

### Rule 3.1: Mandatory `linkToDeath()` for All Remote Clients
When storing a reference to a remote client `IBinder` (from a registered callback or connection), ALWAYS call `linkToDeath()` to receive notification when the client process dies.

* **FORBIDDEN:** Storing remote `IBinder` references without registering a `DeathRecipient`.
* **REQUIRED:** Call `unlinkToDeath()` in the matching cleanup/unregister path to prevent memory leaks.

```java
// ✅ REQUIRED PATTERN:
private final IBinder.DeathRecipient mClientDeathRecipient = new IBinder.DeathRecipient() {
    @Override
    public void binderDied() {
        Log.w(TAG, "Client process died. Cleaning up.");
        handleClientDeath();
    }
};

public void registerCallback(IVehicleEventCallback callback) {
    IBinder binder = callback.asBinder();
    try {
        binder.linkToDeath(mClientDeathRecipient, /* flags= */ 0);
        mCallbackList.register(callback);
    } catch (RemoteException e) {
        Log.e(TAG, "Client already dead during registration.", e);
    }
}

public void unregisterCallback(IVehicleEventCallback callback) {
    callback.asBinder().unlinkToDeath(mClientDeathRecipient, /* flags= */ 0);
    mCallbackList.unregister(callback);
}
```

### Rule 3.2: Use `RemoteCallbackList` for Multi-Client Management
When managing multiple registered client callbacks, ALWAYS use `RemoteCallbackList<T>` instead of a raw `List` or `Map`.

* `RemoteCallbackList` automatically removes dead client callbacks on `beginBroadcast()`.
* Thread-safe: `beginBroadcast()` / `finishBroadcast()` form an atomic broadcast block.
* `linkToDeath()` is handled internally by `RemoteCallbackList`.

```java
// ✅ REQUIRED: RemoteCallbackList for multi-client callback management
private final RemoteCallbackList<IVehicleEventCallback> mCallbackList =
        new RemoteCallbackList<>();

private void notifyAllClients(VehicleData data) {
    int count = mCallbackList.beginBroadcast();
    for (int i = 0; i < count; i++) {
        try {
            mCallbackList.getBroadcastItem(i).onSpeedChanged(data);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to notify client #" + i, e);
        }
    }
    mCallbackList.finishBroadcast();
}
```

---

## 4. Parcelable Implementation Rules

### Rule 4.1: Mandatory Parcelable Fields Order Consistency
* Fields written in `writeToParcel()` **MUST** be read back in `readFromParcel()` / `Parcel` constructor in the **exact same order**.
* Mismatched order causes silent data corruption — no compile-time error.

### Rule 4.2: Null Safety in Parcelable
* When writing nullable `String` or `Object` fields, use `dest.writeString()` (handles null safely) or `dest.writeValue()`.
* When reading, always guard against null return values from `parcel.readString()`.

### Rule 4.3: Mandatory `CREATOR` Static Field
Every `Parcelable` class **MUST** implement the `public static final Parcelable.Creator<T> CREATOR` field.

### Rule 4.4: `describeContents()` Return Value
* Return `0` for standard objects.
* Return `Parcelable.CONTENTS_FILE_DESCRIPTOR` only if the `Parcelable` contains a `ParcelFileDescriptor`.

### Rule 4.5: Prefer Java Records or `@AutoValue` for Simple DTOs
For immutable data-only Parcelable objects (no logic), prefer Java `record` style with manual Parcelable implementation or use the `@Parcelize` annotation (Kotlin) / `@AutoValue` (Java) to reduce boilerplate.

```java
// ✅ REQUIRED Parcelable Implementation Pattern:
public final class VehicleData implements Parcelable {
    private final float mSpeedKph;
    private final int mRpm;
    private final long mTimestampMs;

    public VehicleData(float speedKph, int rpm, long timestampMs) {
        this.mSpeedKph = speedKph;
        this.mRpm = rpm;
        this.mTimestampMs = timestampMs;
    }

    // Parcel read constructor — order MUST match writeToParcel exactly
    private VehicleData(Parcel in) {
        mSpeedKph = in.readFloat();
        mRpm = in.readInt();
        mTimestampMs = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(mSpeedKph);   // Order: 1
        dest.writeInt(mRpm);           // Order: 2
        dest.writeLong(mTimestampMs);  // Order: 3
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<VehicleData> CREATOR =
            new Parcelable.Creator<VehicleData>() {
                @Override
                public VehicleData createFromParcel(Parcel in) {
                    return new VehicleData(in);
                }

                @Override
                public VehicleData[] newArray(int size) {
                    return new VehicleData[size];
                }
            };

    // Accessors
    public float getSpeedKph() { return mSpeedKph; }
    public int getRpm() { return mRpm; }
    public long getTimestampMs() { return mTimestampMs; }
}
```

---

## 5. Security Rules for System Services

### Rule 5.1: Mandatory Permission Check Before Handling IPC Requests
Every AIDL stub method that exposes sensitive data or controls hardware **MUST** verify the calling process's permission before executing any logic.

```java
private static final String REQUIRED_PERMISSION = "android.permission.CAR_SPEED";

@Override
public VehicleData getCurrentSpeed() throws RemoteException {
    // MANDATORY: Check caller permission at the Binder boundary
    mContext.enforceCallingOrSelfPermission(REQUIRED_PERMISSION,
            "Caller lacks permission: " + REQUIRED_PERMISSION);
    return mCachedSpeed;
}
```

### Rule 5.2: Use `Binder.clearCallingIdentity()` for Self-Calls
When the service needs to call its own internal APIs or ContentProviders on behalf of itself (not the calling client), wrap the call with `Binder.clearCallingIdentity()` / `Binder.restoreCallingIdentity()`.

```java
@Override
public List<String> getSensitiveData() throws RemoteException {
    mContext.enforceCallingOrSelfPermission(REQUIRED_PERMISSION, null);
    long token = Binder.clearCallingIdentity();
    try {
        return mInternalRepository.fetchSensitiveData(); // Runs as system, not caller
    } finally {
        Binder.restoreCallingIdentity(token);
    }
}
```

---

## 6. AI Self-Correction & Verification Checklist

Before emitting any AIDL/Binder/IPC code:
1. [ ] Are all server→client callbacks declared with `oneway`? -> **Must be Yes**.
2. [ ] Are all heavy operations offloaded from the Binder thread to an `ExecutorService`? -> **Must be Yes**.
3. [ ] Is `RemoteCallbackList` used for multi-client callback management? -> **Must be Yes**.
4. [ ] Is `linkToDeath()` called for every registered remote client binder? -> **Must be Yes**.
5. [ ] Does every `Parcelable` have `CREATOR`, `writeToParcel()`, and Parcel constructor with **matching field order**? -> **Must be Yes**.
6. [ ] Is `enforceCallingOrSelfPermission()` called at the top of every sensitive AIDL stub method? -> **Must be Yes**.
7. [ ] Is `Binder.clearCallingIdentity()` used when making self-calls inside an IPC handler? -> **Must be Yes**.
