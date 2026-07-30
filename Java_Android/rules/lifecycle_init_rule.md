# Component Lifecycle & Constructor Rules (lifecycle_init_rule.md)

This document defines mandatory guidelines for class construction, explicit `init()` / `release()` lifecycle management, idempotent initialization, and strict restrictions on method calls inside constructors in Java/Android.

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

### Rule 1.4: Idempotence & Safe Re-initialization
* **Idempotent `init()`:** Calling `init()` multiple times consecutively **MUST** be safe and do nothing if already initialized (prevent duplicate thread pools or duplicate receiver registrations).
* **Re-initialization after `release()`:** Calling `init()` AFTER a previous `release()` call **MUST** cleanly re-allocate thread pools and re-register resources without crashing or leaking state.
* **Idempotent `release()`:** Calling `release()` multiple times consecutively **MUST** be safe without throwing `NullPointerException` or `IllegalStateException`.

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

    // Bad 3: init() is NOT idempotent; calling init() twice creates duplicate receivers & leaks thread pool!
    public void init() {
        mExecutor = Executors.newSingleThreadExecutor(); // Overwrites existing executor without shutdown!
        registerReceivers(); // Duplicate registration!
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
    private boolean mIsInitialized;

    // Correct: Constructor only assigns simple dependencies
    public DataController(Context context) {
        this.mContext = Objects.requireNonNull(context, "Context cannot be null");
    }

    // Correct: Idempotent init() safe for double calls & re-initialization after release()
    public synchronized void init() {
        if (mIsInitialized) {
            // Already initialized - safe guard against redundant init calls
            return;
        }

        if (mExecutor == null || mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }

        registerReceivers();
        mIsInitialized = true;
    }

    // Correct: Idempotent release() safe for double calls & allows future re-init()
    public synchronized void release() {
        if (!mIsInitialized) {
            // Already released or never initialized - safe guard against double release
            return;
        }

        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }

        unregisterReceivers();
        mIsInitialized = false; // Reset state allowing clean re-initialization if init() is called again
    }

    private void registerReceivers() {
        // Private helper method - not overridable
    }

    private void unregisterReceivers() {
        // Private helper method - not overridable
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting any Java class definition:
1. [ ] Is the constructor lightweight and restricted to simple parameter assignments? -> **Must be Yes**.
2. [ ] Are overridable/public methods avoided inside the constructor? -> **Must be Yes**.
3. [ ] Is `init()` idempotent and safe if called multiple times or after `release()`? -> **Must be Yes**.
4. [ ] Is `release()` idempotent and safe if called multiple times? -> **Must be Yes**.
