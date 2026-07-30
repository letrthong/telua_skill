# Component Lifecycle & Constructor Rules (lifecycle_init_rule.md)

This document defines mandatory guidelines for class construction, explicit `init()` / `release()` lifecycle management, and strict restrictions on method calls inside constructors in Java/Android.

---

## 1. Core Rules

### Rule 1.1: Lightweight Constructors
Constructors **MUST** remain lightweight and side-effect free. They should only perform simple field assignments and dependency injection.

### Rule 1.2: Prohibition of Overridable Method Calls in Constructors
Never invoke overridable (non-private, non-final) methods inside a constructor. Subclasses overriding those methods will execute before their own constructor fields are initialized, leading to `NullPointerException` or uninitialized state bugs.

### Rule 1.3: Explicit `init()` and `release()` Lifecycle Pattern
For components that manage resources, listeners, threads, or complex state:
* **`init()` / `initialize()`:** Move heavy initialization, network setup, listener registration, or background thread starting into an explicit `init()` method.
* **`release()` / `destroy()`:** Provide a matching `release()` method to unregister listeners, shut down executors, clear handlers, and release native/heavy resources.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
public class BaseController {

    public BaseController() {
        // Bad 1: Heavy setup & starting threads directly in constructor
        startBackgroundSync(); 

        // Bad 2: Calling overridable method in constructor! Subclass state is not initialized yet.
        setupViews(); 
    }

    public void setupViews() {
        // Overridable method
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

```java
public class DataController {
    private final Context mContext;
    private ExecutorService mExecutor;

    // Correct: Constructor only assigns simple dependencies
    public DataController(Context context) {
        this.mContext = context;
    }

    // Correct: Explicit init() for heavy setup & resource allocation
    public void init() {
        if (mExecutor == null || mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }
        registerReceivers();
    }

    // Correct: Explicit release() for cleanup & resource disposal
    public void release() {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }
        unregisterReceivers();
    }

    private void registerReceivers() {
        // Private helper method - not overridable
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java class definition:
1. [ ] Is the constructor lightweight and restricted to simple parameter assignments? -> **Yes**.
2. [ ] Are overridable/public methods avoided inside the constructor? -> **Yes**.
3. [ ] Are heavy setups and resource allocation moved to an explicit `init()` method? -> **Yes**.
4. [ ] Is a symmetrical `release()` or `destroy()` method provided for teardown? -> **Yes**.
