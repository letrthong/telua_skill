# Design Patterns & Architecture Rules (design_pattern_architecture_rule.md)

This document defines mandatory architectural standards, MVC/MVVM separation of concerns, and design pattern integration rules (Strategy, Observer, Factory, etc.) in code design.

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
* **Observer Pattern:** Use event-driven subscriptions to trigger side-effects (e.g., email notifications, inventory updates, point allocation) without tight coupling.
* **Factory Pattern:** Centralize complex object creation logic into specialized Factory classes.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):
Monolithic code mixing UI/Main logic with concrete payment gateways, hardcoded creation, and manual inline side-effects.

```java
// Bad: Tightly coupled, no patterns used, mixed responsibilities
public class OrderProcessor {
    public void checkout(String orderId, int amount, String paymentType) {
        // Hardcoded concrete creation & tight coupling
        if (paymentType.equals("MOMO")) {
            System.out.println("Processing MoMo...");
        } else if (paymentType.equals("STRIPE")) {
            System.out.println("Processing Stripe...");
        }
        
        // Inline tight side-effects
        sendEmail(orderId);
        updateInventory(orderId);
        addVipPoints(amount);
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

#### 1. Strategy Pattern (Payment Gateways)
```java
public interface PaymentStrategy {
    boolean processPayment(int amount);
    String getGatewayName();
}

public class StripePayment implements PaymentStrategy {
    @Override
    public boolean processPayment(int amount) {
        // Stripe integration logic
        return true;
    }
    @Override
    public String getGatewayName() { return "Stripe"; }
}

public class MomoPayment implements PaymentStrategy {
    @Override
    public boolean processPayment(int amount) {
        // MoMo integration logic
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

#### 3. Observer Pattern (Decoupled Event Subscribers)
```java
public interface OrderObserver {
    void onOrderPaid(String orderId, int amount);
}

public class EmailInvoiceService implements OrderObserver {
    @Override
    public void onOrderPaid(String orderId, int amount) {
        // Send email invoice
    }
}

public class InventoryService implements OrderObserver {
    @Override
    public void onOrderPaid(String orderId, int amount) {
        // Deduct inventory stock
    }
}

public class VipPointService implements OrderObserver {
    @Override
    public void onOrderPaid(String orderId, int amount) {
        // Award VIP points
    }
}
```

#### 4. Decoupled Context / Processor
```java
public class OrderProcessor {
    private PaymentStrategy mPaymentStrategy;
    private final List<OrderObserver> mObservers = new ArrayList<>();

    public void setPaymentStrategy(PaymentStrategy strategy) {
        this.mPaymentStrategy = strategy;
    }

    public void registerObserver(OrderObserver observer) {
        this.mObservers.add(observer);
    }

    public void checkout(String orderId, int amount) {
        if (mPaymentStrategy == null) {
            throw new IllegalStateException("Payment strategy not configured!");
        }

        boolean success = mPaymentStrategy.processPayment(amount);
        if (success) {
            notifyObservers(orderId, amount);
        }
    }

    private void notifyObservers(String orderId, int amount) {
        for (OrderObserver observer : mObservers) {
            observer.onOrderPaid(orderId, amount);
        }
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before designing or generating code architecture:
1. [ ] Is the business logic separated from the View / UI layer? -> **Must be Yes**.
2. [ ] Are algorithms/gateways decoupled using the Strategy Pattern? -> **Must be Yes**.
3. [ ] Are object creation responsibilities delegated to Factory classes? -> **Must be Yes**.
4. [ ] Are side-effects (notifications, inventory, logging) handled via Observer events? -> **Must be Yes**.
5. [ ] Are high-level modules depending on interfaces instead of concrete implementations? -> **Must be Yes**.
