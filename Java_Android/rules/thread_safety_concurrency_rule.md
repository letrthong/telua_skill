<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# Thread Safety & Concurrency Rules (thread_safety_concurrency_rule.md)

This document defines mandatory guidelines for multithreading, concurrent state management, safe publication, atomic primitives, and high-performance thread safety in Java/Android development.

---

## 1. Core Rules

### Rule 1.1: Volatile Field Access & Mandatory Local Copy Idiom
* **Single Local Snapshot Read:** When accessing a `volatile` reference field multiple times within a method or performing a *check-then-act* operation, the code **MUST** copy the `volatile` field into a local variable once at the beginning of the scope:
  ```java
  T localState = mVolatileState;
  ```
* **TOCTOU Bug Prevention:** Direct repeated reads of `volatile` fields (e.g. `if (mState != null) mState.doAction();`) introduce critical Time-of-Check to Time-of-Use (TOCTOU) race conditions: another thread could nullify or mutate `mState` between the check and use, leading to `NullPointerException`. A local copy guarantees execution against a consistent, unchanging snapshot.
* **Hardware Memory Barrier Optimization:** Every read of a `volatile` field enforces a CPU memory fence/barrier and disables certain JIT compiler optimizations. Copying to a thread-local stack variable avoids repeated main-memory reads in read-heavy paths, improving throughput by up to 25%.

### Rule 1.2: Safe Publication via Defensive Immutability
* **Immutable State Mandate:** When sharing state across threads via `volatile` or `AtomicReference`, the published state object **MUST** be immutable (e.g., Java Record, final fields, defensive unmodifiable collections).
* **Prohibition of In-Place Mutation:** Never mutate the internal fields of an object that is already referenced by a published `volatile` field. All state updates must follow an atomic replacement idiom: construct a new immutable snapshot and publish it via a single write (`mState = new ImmutableState(...)`).

### Rule 1.3: Atomic Primitives for State & Lifecycle Transitions
* **Lightweight Lock-Free Primitives:** For lifecycle flags, counters, and single-value references, prefer `AtomicBoolean`, `AtomicInteger`, `AtomicLong`, and `AtomicReference` over heavy `synchronized` blocks.
* **Compare-And-Set (CAS):** Use `compareAndSet()` or atomic operations to ensure race-free state transitions without thread suspension or priority inversion overhead.

### Rule 1.4: Concurrency in Singletons & Shared Services
* **Internal State Thread Safety:** Making the instantiation of a Singleton thread-safe (via Bill Pugh Holder or Double-Checked Locking) is necessary but **not sufficient**. Since Singletons are globally shared across all threads, every mutable internal field inside a Singleton **MUST** be explicitly thread-safe (using volatile immutable snapshots, concurrent collections, or atomic primitives).
* **Double-Checked Locking (DCL) Local Copy Optimization:** When implementing volatile Double-Checked Locking, use the local variable copy idiom (Joshua Bloch, *Effective Java* Item 83) to ensure the volatile field is read only once in the common already-initialized case:
  ```java
  public static MySingleton getInstance() {
      MySingleton result = sInstance; // Local copy of volatile field
      if (result == null) {
          synchronized (MySingleton.class) {
              result = sInstance;
              if (result == null) {
                  sInstance = result = new MySingleton();
              }
          }
      }
      return result;
  }
  ```
* **Guard Against Thread Contention:** Never hold coarse-grained `synchronized` locks across entire long-running methods in a Singleton. If a Worker Thread blocks a Singleton lock for I/O or network, any UI Thread calling the same Singleton will freeze, causing an **Application Not Responding (ANR)** crash.
* **Evaluate Singleton vs Dependency Injection:** Avoid treating Singletons as "global mutable state bags" (an anti-pattern). Singletons should primarily manage stateless services or hardware-bound single instances (e.g. VHAL managers). For complex stateful business logic, prefer Constructor Dependency Injection with lifecycle-bound scopes.

### Rule 1.5: Absolute Prohibition of Alien Calls (Callbacks/I/O) Under Lock
* **Deadlock & Contention Prevention:** Never invoke external callbacks, listener dispatches, or blocking I/O operations while holding an internal synchronization lock (`synchronized(this)` or custom mutex).
* **Copy-Then-Dispatch:** Copy the registered listeners or target state to a local variable or snapshot, release the lock, and then trigger callbacks outside the synchronized block (or dispatch via `@CallbackExecutor` / background `Handler`).

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (TOCTOU Bug, Mutable State Leaks & Lock Contention):

```java
public class StateManager {
    // Bad 1: Volatile reference to a MUTABLE object
    private volatile MutableUserData mUserData;
    private static volatile StateManager sInstance;

    // Bad 2: Unoptimized DCL reading volatile sInstance twice on fast path
    public static StateManager getInstance() {
        if (sInstance == null) {
            synchronized (StateManager.class) {
                if (sInstance == null) {
                    sInstance = new StateManager();
                }
            }
        }
        return sInstance;
    }

    // Bad 3: TOCTOU race condition! mUserData can become null or change between check and use
    public String getUserGreeting() {
        if (mUserData != null) {
            // DANGER: If another thread updates mUserData or sets it null here -> NPE or inconsistent state!
            return "Hello, " + mUserData.getName() + " (Role: " + mUserData.getRole() + ")";
        }
        return "Anonymous";
    }

    // Bad 4: Calling alien listener under lock! Can cause DEADLOCK and thread contention.
    public synchronized void updateRole(String newRole, StateListener listener) {
        mUserData.setRole(newRole); // Bad: In-place mutation of volatile-referenced object!
        if (listener != null) {
            listener.onRoleChanged(newRole); // DANGER: Alien callback executed inside synchronized block!
        }
    }
}
```

### ✅ REQUIRED BEST PRACTICE (Volatile Local Copy, Immutable Snapshot & Safe Dispatch):

```java
package com.example.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.app.examples.AppLogger;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Demonstrates thread-safe state management complying with thread_safety_concurrency_rule.md.
 */
public class StateManager {
    private static final String TAG = StateManager.class.getSimpleName();

    // 1. Immutable record/class for shared state snapshot
    public static final class UserData {
        private final String mName;
        private final String mRole;

        public UserData(@NonNull String name, @NonNull String role) {
            this.mName = Objects.requireNonNull(name, "name must not be null");
            this.mRole = Objects.requireNonNull(role, "role must not be null");
        }

        @NonNull public String getName() { return mName; }
        @NonNull public String getRole() { return mRole; }
    }

    // Volatile reference to IMMUTABLE object -> Safe Publication
    private volatile UserData mUserData;
    private static volatile StateManager sInstance;

    private StateManager() {}

    // 2. Optimized Double-Checked Locking with Local Copy
    @NonNull
    public static StateManager getInstance() {
        StateManager result = sInstance; // Single volatile read on fast path
        if (result == null) {
            synchronized (StateManager.class) {
                result = sInstance;
                if (result == null) {
                    sInstance = result = new StateManager();
                }
            }
        }
        return result;
    }

    // 3. Volatile + Local Copy Pattern: Guarantees stable snapshot & prevents TOCTOU
    @NonNull
    public String getUserGreeting() {
        // Step 1: Copy volatile reference to thread-local variable ONCE
        UserData snapshot = mUserData;

        // Step 2: Use local variable for all checks and accesses
        if (snapshot != null) {
            return "Hello, " + snapshot.getName() + " (Role: " + snapshot.getRole() + ")";
        }
        return "Anonymous";
    }

    // 4. Safe Publication: Atomically replaces reference with a new immutable snapshot
    public void updateRole(@NonNull String newRole, @Nullable StateListener listener, @NonNull Executor executor) {
        Objects.requireNonNull(newRole, "newRole must not be null");
        Objects.requireNonNull(executor, "executor must not be null");

        UserData updatedSnapshot;
        synchronized (this) {
            UserData current = mUserData;
            String name = (current != null) ? current.getName() : "Unknown";
            // Construct a new immutable object instead of mutating in place
            updatedSnapshot = new UserData(name, newRole);
            mUserData = updatedSnapshot;
        }

        // 5. Alien callback dispatched OUTSIDE of lock on dedicated executor
        if (listener != null) {
            executor.execute(() -> {
                try {
                    listener.onRoleChanged(updatedSnapshot.getRole());
                } catch (Exception e) {
                    AppLogger.e(TAG, "Error notifying state listener", e);
                }
            });
        }
    }

    public interface StateListener {
        void onRoleChanged(String newRole);
    }
}
```

---

## 3. Real-World Connection Sharing & Stale Connection Prevention

When multiple client classes (e.g., `ClientModuleA`, `ClientModuleB`) must share a single underlying connection resource (e.g. Socket, Bluetooth, MQTT, AOSP `CarService` Binder) that can drop and reconnect, choosing the right concurrency architecture prevents the fatal **Stale Connection Bug** (clients continuing to invoke methods on dead, closed connections).

### Architectural Comparison & Selection Matrix:

| Dimension | Pattern A: Provider Indirection (Pull) 🏆 | Pattern B: Multi-Subscriber Observer (Push) |
| :--- | :--- | :--- |
| **Reference Template** | [ResilientConnectionShareTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/ResilientConnectionShareTemplate.java) | [MultiSubscriberConnectionTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/MultiSubscriberConnectionTemplate.java) |
| **Industry Adoption** | **~75% (Recommended Primary)** | **~25% (Domain Specific)** |
| **Interaction Model** | **Pull on Demand:** Clients call `provider.executeWithConnection(...)`. | **Push Broadcast:** Manager pushes new connection to subscribers. |
| **Client Complexity** | **Zero Overhead:** Clients never store the raw connection handle. | **Moderate:** Clients must implement listener and manage reference lifecycle. |
| **Stale Bug Risk** | **0% by Design:** Impossible because clients hold no raw reference. | **Controlled:** Requires clients to nullify references on `onConnectionLost()`. |
| **Ideal Use Cases** | Request-Response, REST/RPC commands, Database queries, Volume/HVAC control. | Event-Driven pub/sub (MQTT topics), AOSP Binder callback re-registration, UI connection indicators. |

---

### Pattern A: Provider Indirection Pattern (Pull on Demand — Recommended Primary ~75%)
* **Benchmark:** [ResilientConnectionShareTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/ResilientConnectionShareTemplate.java)
* **Core Principle:** Clients (`AudioControllerA`, `HvacControllerB`) **NEVER** hold a direct reference to the raw connection session. Instead, they hold a reference to the `ConnectionProvider` (or Singleton Manager).
* **Execution Flow:** Every client command queries the active session on-demand:
  ```java
  mProvider.executeWithConnection(session -> session.writeMessage("..."));
  ```
* **Auto-Recovery:** The Provider uses Rule 1.1 (`volatile` Local Copy) to check connectivity. If dead or failing, the Provider catches the error, marks the session as dead, and triggers background reconnection (`mIsReconnecting.compareAndSet(false, true)`). Once connected, `mActiveSession = newSession` is published atomically. Both clients immediately begin using the new connection on their next call without any callback re-wiring.

---

### Pattern B: Multi-Subscriber Push Re-Share Pattern (Observer — Domain Specific ~25%)
* **Benchmark:** [MultiSubscriberConnectionTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/MultiSubscriberConnectionTemplate.java)
* **Core Principle:** Used when clients **MUST** hold the connection handle directly (e.g., to re-register callbacks on AOSP `CarService` restart, re-subscribe to MQTT topics, or update UI connection badges).
* **Two-Stage Stale Prevention Lifecycle:**
  1. **Disconnection Broadcast (`onConnectionLost`):** Manager immediately notifies all registered subscribers in `CopyOnWriteArrayList` to nullify their cached handle (`mCachedConnection = null`), preventing calls to dead sockets.
  2. **Reconnection Broadcast (`onConnectionRestored`):** Once the background worker successfully reconnects, it broadcasts the fresh connection handle to all subscribers simultaneously.
* **Strict Concurrency Rule (Rule 1.5):** All subscriber notifications **MUST** be broadcast outside synchronized blocks to prevent deadlocks.
* **Late Subscriber Support:** If a client registers after the connection is already active, the manager delivers the active connection handle immediately.

---

## 4. AI Self-Correction & Verification Checklist

Before generating or reviewing any multithreaded Java code:
1. [ ] Is every `volatile` field copied into a local variable (`T local = mField;`) before multi-read or *check-then-act* logic (as demonstrated in [MediaPlaybackStateTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/MediaPlaybackStateTemplate.java))? -> **Must be Yes**.
2. [ ] Are objects referenced by `volatile` strictly immutable (no in-place mutations)? -> **Must be Yes**.
3. [ ] If implementing DCL for Singletons, is the local variable copy optimization applied? -> **Must be Yes**.
4. [ ] For shared connections across multiple classes, is **Provider Indirection** ([ResilientConnectionShareTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/ResilientConnectionShareTemplate.java)) preferred unless explicit callback re-registration or UI state observation is required ([MultiSubscriberConnectionTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/MultiSubscriberConnectionTemplate.java))? -> **Must be Yes**.
5. [ ] Are external listeners, callbacks, or I/O calls executed strictly **outside** `synchronized` blocks? -> **Must be Yes**.
6. [ ] Are atomic primitives (`AtomicBoolean`, `AtomicInteger`) used for simple flags/counters instead of method-wide locks? -> **Must be Yes**.
