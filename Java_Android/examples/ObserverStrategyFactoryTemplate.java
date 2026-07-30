package com.example.app.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Integrated Observer, Strategy, and Factory Pattern Reference Template.
 *
 * Key Principles Demonstrated:
 * 1. Observer Pattern: Event-driven notification dispatching (addObserver, removeObserver, notifyObservers).
 * 2. Strategy Pattern: Encapsulating interchangeable payment algorithms.
 * 3. Factory Pattern: Centralizing strategy instantiation with Java 17+ switch expressions and Optional.
 * 4. Loose Coupling & SRP: Decoupling core checkout logic from side-effects (Email, Rewards).
 */
public class ObserverStrategyFactoryTemplate {

    // --- 1. OBSERVER PATTERN ---

    /**
     * Observer interface for receiving post-payment notification events.
     */
    public interface PaymentObserver {
        void onPaymentCompleted(int amount, String method);
    }

    /**
     * Concrete Observer 1: Email Notification Service.
     */
    public static class EmailNotifier implements PaymentObserver {
        @Override
        public void onPaymentCompleted(int amount, String method) {
            System.out.println("[Email Service] Sending receipt of $" + amount + " via " + method + ".");
        }
    }

    /**
     * Concrete Observer 2: Customer Loyalty Points System.
     */
    public static class RewardPointSystem implements PaymentObserver {
        @Override
        public void onPaymentCompleted(int amount, String method) {
            int points = amount / 10;
            System.out.println("[Reward System] Added " + points + " points to customer account.");
        }
    }

    // --- 2. STRATEGY PATTERN ---

    /**
     * Payment Strategy interface.
     */
    public interface PaymentStrategy {
        void pay(int amount);
        String getMethodName();
    }

    public static class CreditCardPayment implements PaymentStrategy {
        @Override
        public void pay(int amount) {
            System.out.println("Paid $" + amount + " using Credit Card.");
        }

        @Override
        public String getMethodName() {
            return "Credit Card";
        }
    }

    public static class PayPalPayment implements PaymentStrategy {
        @Override
        public void pay(int amount) {
            System.out.println("Paid $" + amount + " using PayPal.");
        }

        @Override
        public String getMethodName() {
            return "PayPal";
        }
    }

    // --- 3. FACTORY PATTERN ---

    public static class PaymentFactory {
        public static Optional<PaymentStrategy> getStrategy(String type) {
            if (type == null || type.isBlank()) {
                return Optional.empty();
            }

            return switch (type.toUpperCase()) {
                case "CARD" -> Optional.of(new CreditCardPayment());
                case "PAYPAL" -> Optional.of(new PayPalPayment());
                default -> Optional.empty();
            };
        }
    }

    // --- 4. CONTEXT & SUBJECT (ShoppingCart) ---

    public static class ShoppingCart {
        private PaymentStrategy mStrategy;
        private final List<PaymentObserver> mObservers = new ArrayList<>();

        public void setPaymentStrategy(PaymentStrategy strategy) {
            this.mStrategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        }

        public void addObserver(PaymentObserver observer) {
            if (observer != null && !mObservers.contains(observer)) {
                mObservers.add(observer);
            }
        }

        public void removeObserver(PaymentObserver observer) {
            mObservers.remove(observer);
        }

        public void notifyObservers(int amount, String method) {
            for (PaymentObserver observer : mObservers) {
                observer.onPaymentCompleted(amount, method);
            }
        }

        public void checkout(int amount) {
            if (mStrategy == null) {
                throw new IllegalStateException("Payment strategy not set!");
            }

            // 1. Execute payment via Strategy
            mStrategy.pay(amount);

            // 2. Trigger background side-effects via Observers
            notifyObservers(amount, mStrategy.getMethodName());
        }
    }
}
