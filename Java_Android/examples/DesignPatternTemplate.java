package com.example.app.examples;

import android.util.Log;

import java.util.Objects;
import java.util.Optional;

/**
 * Standard Strategy & Factory Design Pattern Reference Template.
 *
 * Key Principles Demonstrated:
 * 1. Strategy Pattern: Decoupling algorithms into interchangeable interfaces.
 * 2. Factory Pattern: Encapsulating object creation logic using Java 17+ switch expressions.
 * 3. Null Safety: Utilizing java.util.Optional to eliminate NullPointerExceptions.
 * 4. Context Decoupling: Eliminating if-else conditional branches in the caller context.
 * 5. Logging Standard: Using android.util.Log with dynamic TAG instead of System.out.println.
 */
public class DesignPatternTemplate {

    /**
     * Strategy Interface defining payment behavior.
     */
    public interface PaymentStrategy {
        void pay(int amount);
        String getMethodName();
    }

    /**
     * Concrete Strategy 1: Credit Card Payment.
     */
    public static class CreditCardPayment implements PaymentStrategy {
        private static final String TAG = CreditCardPayment.class.getSimpleName();

        @Override
        public void pay(int amount) {
            Log.d(TAG, String.format("Paid $%d using Credit Card.", amount));
        }

        @Override
        public String getMethodName() {
            return "Credit Card";
        }
    }

    /**
     * Concrete Strategy 2: PayPal Payment.
     */
    public static class PayPalPayment implements PaymentStrategy {
        private static final String TAG = PayPalPayment.class.getSimpleName();

        @Override
        public void pay(int amount) {
            Log.d(TAG, String.format("Paid $%d using PayPal.", amount));
        }

        @Override
        public String getMethodName() {
            return "PayPal";
        }
    }

    /**
     * Factory class responsible for instantiating the appropriate strategy.
     */
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

    /**
     * Context class executing the strategy without conditional branching.
     */
    public static class ShoppingCart {
        private PaymentStrategy mStrategy;

        public void setPaymentStrategy(PaymentStrategy strategy) {
            this.mStrategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        }

        public void checkout(int amount) {
            if (mStrategy != null) {
                mStrategy.pay(amount);
            } else {
                throw new IllegalStateException("Payment strategy not set!");
            }
        }
    }
}
