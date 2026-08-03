# Design Patterns & Architecture Rules (design_pattern_architecture_rule.md)

This document defines mandatory architectural standards, MVC/MVVM separation of concerns, design pattern integration rules (Strategy, Observer, Factory), and non-blocking callback protocols.

---

## 1. Core Architectural Rules

### Rule 1.1: Strict Separation of Concerns (MVC / MVVM Architecture)
* **Model:** Handles raw business data, domain logic, and state. Must have zero references to UI/View components.
* **View (UI Layer):** Responsible strictly for rendering UI elements and forwarding user interactions. Must NOT execute business logic, payment processing, or database operations directly.
* **Controller / ViewModel / Processor:** Coordinates interaction between Model and View while delegating specialized tasks to Design Pattern implementations.

### Rule 1.2: Prefer Abstraction & Loose Coupling (Dependency Inversion)
Code must depend on abstractions (interfaces or abstract classes), never on concrete implementations. High-level business processors must not instantiate low-level concrete services directly.

### Rule 1.3: Mandatory Use of Standard Design Patterns
* **Strategy Pattern:** Encapsulate interchangeable algorithms, payment gateways, or processing behaviors into interface-based strategies.
* **Observer / Callback Pattern:** Use event-driven subscriptions to trigger side-effects (e.g., email notifications, inventory updates, point allocation) without tight coupling.
* **Factory Pattern:** Centralize complex object creation logic into specialized Factory classes.

### Rule 1.4: Non-Blocking Asynchronous Callback Dispatching Protocol
* **Interface Naming & Functional Annotation:**
  * Event Listeners **MUST** use the `On[Event]Listener` prefix (e.g., `OnCarServiceListener`).
  * Asynchronous Callbacks **MUST** use the `[Feature]Callback` suffix (e.g., `ResultCallback`).
  * Single-method interfaces **MUST** be annotated with `@FunctionalInterface`.
* **Non-Blocking Dispatching:** Never invoke listener callbacks directly inline on heavy worker threads or lock-sensitive threads without offloading.
  * **AOSP Executor Pattern:** Accept an `@CallbackExecutor Executor executor` parameter and dispatch callbacks via `executor.execute(() -> listener.onSuccess(data))`.
  * **Main Looper Handler Pattern:** Post callbacks back to the Main UI Thread using `mMainHandler.post(() -> listener.onSuccess(data))`.
* **Exception Isolation:** Wrap callback invocations inside a `try-catch (Throwable t)` block to isolate publisher loops from unexpected runtime exceptions thrown by subscriber implementations.

### Rule 1.5: The 4 Rules of Simple Design (Kent Beck)
Every software module designed or refactored in this codebase **MUST** satisfy Kent Beck's 4 rules in order of priority:
1. **Passes all tests:** The implementation passes 100% of accompanying unit tests.
2. **Contains no duplication (DRY):** Zero copy-pasted or redundant logic.
3. **Expresses programmer intent:** Clear, self-documenting naming and architecture.
4. **Minimizes classes and methods:** Avoid over-engineering or creating superfluous abstractions.

### Rule 1.6: Code Smells Warning System
Actively detect and eliminate the 6 classic architectural code smells:
* **Rigidity:** Code is hard to change; a small change causes a cascade of dependent edits.
* **Fragility:** A single edit breaks unrelated features in unexpected places.
* **Immobility:** Internal logic cannot be reused in another module or project.
* **Needless Complexity:** Over-engineered patterns where simple methods suffice.
* **Needless Repetition:** Copy-pasted code blocks instead of shared helpers.
* **Opacity:** Obscure, hard-to-read code logic.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Tightly Coupled Monolithic Code & Direct Inline Blocking Callbacks):

```java
// Bad: Tightly coupled, inline tight side-effects, direct blocking callback invocation
public class OrderProcessor {
    public interface OrderCallback {
        void onPaid(String orderId) throws Exception; // Bad: Checked exception in callback!
    }

    public void checkout(String orderId, int amount, String paymentType, OrderCallback callback) {
        if (paymentType.equals("MOMO")) {
            System.out.println("Processing MoMo...");
        } else if (paymentType.equals("STRIPE")) {
            System.out.println("Processing Stripe...");
        }
        
        // BAD: Direct blocking callback call inline! If subscriber hangs, publisher thread freezes!
        try {
            callback.onPaid(orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### ✅ REQUIRED BEST PRACTICE (Decoupled Patterns & Non-Blocking Async Callbacks):

#### 1. Strategy Pattern (Payment Gateways)
```java
public interface PaymentStrategy {
    boolean processPayment(int amount);
    String getGatewayName();
}

public class StripePayment implements PaymentStrategy {
    @Override
    public boolean processPayment(int amount) {
        return true;
    }
    @Override
    public String getGatewayName() { return "Stripe"; }
}

public class MomoPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(int amount) {
        return true;
    }
    @Override
    public String getGatewayName() { return "MoMo Wallet"; }
}
```

#### 2. Factory Pattern (Gateway Creation)
```java
public class PaymentFactory {
    public static Optional<PaymentStrategy> createPaymentGateway(String methodType) {
        return switch (methodType.toUpperCase()) {
            case "STRIPE" -> Optional.of(new StripePayment());
            case "MOMO" -> Optional.of(new MomoPayment());
            default -> Optional.empty();
        };
    }
}
```

#### 3. Non-Blocking Observer / Callback Protocol
```java
@FunctionalInterface
public interface OrderObserver {
    void onOrderPaid(String orderId, int amount);
}

public class OrderProcessor {
    private static final String TAG = OrderProcessor.class.getSimpleName();

    private PaymentStrategy mPaymentStrategy;
    private final List<OrderObserver> mObservers = new ArrayList<>();

    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.mPaymentStrategy = strategy;
    }

    public void registerObserver(OrderObserver observer) {
        this.mObservers.add(observer);
    }

    public void checkout(Executor callbackExecutor, String orderId, int amount) {
        Objects.requireNonNull(callbackExecutor, "Executor cannot be null");
        if (mPaymentStrategy == null) {
            throw new IllegalStateException("Payment strategy not configured!");
        }

        boolean success = mPaymentStrategy.processPayment(amount);
        if (success) {
            notifyObserversAsync(callbackExecutor, orderId, amount);
        }
    }

    /**
     * Dispatches callbacks asynchronously via Executor so publisher thread never hangs.
     */
    private void notifyObserversAsync(Executor executor, String orderId, int amount) {
        for (OrderObserver observer : mObservers) {
            executor.execute(() -> {
                try {
                    observer.onOrderPaid(orderId, amount);
                } catch (Throwable t) {
                    Log.e(TAG, "Unhandled exception in observer callback execution", t);
                }
            });
        }
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting code architecture or interfaces:
1. [ ] Is business logic separated from the View / UI layer? -> **Must be Yes**.
2. [ ] Are algorithms/gateways decoupled using the Strategy Pattern? -> **Must be Yes**.
3. [ ] Are object creation responsibilities delegated to Factory classes? -> **Must be Yes**.
4. [ ] Are side-effects (notifications, inventory, logging) handled via Observer events? -> **Must be Yes**.
5. [ ] Are callbacks dispatched asynchronously via `@CallbackExecutor Executor` or `Handler`? -> **Must be Yes**.
6. [ ] Are callback invocations isolated inside `try-catch (Throwable t)` guards? -> **Must be Yes**.
