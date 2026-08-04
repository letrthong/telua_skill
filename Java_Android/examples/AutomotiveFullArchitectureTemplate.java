package com.example.app.examples;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enterprise Production Automotive Architecture Reference Template.
 * Demonstrates the Seamless Integration of 5 Essential Design Patterns:
 *
 * 1. SINGLETON PATTERN: Centralized, thread-safe VehicleServiceLocator for VHAL manager access.
 * 2. REPOSITORY PATTERN: VehicleTelemetryRepository abstracts raw VHAL hardware details from UI/ViewModels.
 * 3. OBSERVER / LISTENER PATTERN: Asynchronous event callbacks notifying subscribers of vehicle property updates.
 * 4. STRATEGY PATTERN: Encapsulates signal-specific algorithms (Speed filtering, Temperature conversion, EV Battery range).
 * 5. FACTORY & ADAPTER PATTERN: TelemetryStrategyFactory creates strategies; VehicleDataFormatterAdapter formats raw payload into clean DTOs.
 */
public class AutomotiveFullArchitectureTemplate {

    // --- 1. DATA TRANSFER OBJECT (DTO) & ADAPTER PATTERN ---

    /**
     * Clean Data Record representing formatted vehicle telemetry.
     */
    public record VehicleTelemetryRecord(int propertyId, String formattedValue, long timestamp) {}

    /**
     * ADAPTER PATTERN: Adapts raw VHAL payload objects into clean VehicleTelemetryRecord DTOs.
     */
    public static final class VehicleDataFormatterAdapter {
        public static VehicleTelemetryRecord adapt(int propertyId, Object rawValue) {
            String formatted = (rawValue != null) ? rawValue.toString() : "N/A";
            return new VehicleTelemetryRecord(propertyId, formatted, System.currentTimeMillis());
        }
    }

    // --- 2. STRATEGY PATTERN & FACTORY PATTERN ---

    /**
     * STRATEGY PATTERN: Interface defining interchangeable vehicle telemetry signal processors.
     */
    public interface TelemetryProcessingStrategy {
        String processSignal(Object rawValue);
        int getTargetPropertyId();
    }

    public static class SpeedProcessingStrategy implements TelemetryProcessingStrategy {
        private static final String TAG = SpeedProcessingStrategy.class.getSimpleName();

        @Override
        public String processSignal(Object rawValue) {
            float speed = (rawValue instanceof Number n) ? n.floatValue() : 0.0f;
            Log.d(TAG, "Processing speed signal: " + speed + " km/h");
            return String.format("%.1f km/h", Math.max(0.0f, speed));
        }

        @Override
        public int getTargetPropertyId() {
            return 0x100; // Simulating PER_VEHICLE_SPEED
        }
    }

    public static class HvacProcessingStrategy implements TelemetryProcessingStrategy {
        private static final String TAG = HvacProcessingStrategy.class.getSimpleName();

        @Override
        public String processSignal(Object rawValue) {
            float tempC = (rawValue instanceof Number n) ? n.floatValue() : 22.0f;
            Log.d(TAG, "Processing HVAC temperature signal: " + tempC + "°C");
            return String.format("%.1f°C", tempC);
        }

        @Override
        public int getTargetPropertyId() {
            return 0x200; // Simulating HVAC_TEMPERATURE
        }
    }

    /**
     * FACTORY PATTERN: Instantiates the appropriate strategy based on VHAL property ID.
     */
    public static final class TelemetryStrategyFactory {
        public static Optional<TelemetryProcessingStrategy> getStrategy(int propertyId) {
            return switch (propertyId) {
                case 0x100 -> Optional.of(new SpeedProcessingStrategy());
                case 0x200 -> Optional.of(new HvacProcessingStrategy());
                default -> Optional.empty();
            };
        }
    }

    // --- 3. OBSERVER / LISTENER PATTERN ---

    /**
     * OBSERVER PATTERN: Callback contract implemented by UI / ViewModels to observe vehicle telemetry.
     */
    public interface OnTelemetryUpdateListener {
        void onTelemetryUpdated(VehicleTelemetryRecord record);
        void onTelemetryError(int propertyId, String errorMessage);
    }

    // --- 4. SINGLETON PATTERN ---

    /**
     * SINGLETON PATTERN: Thread-safe Bill Pugh Holder managing single VHAL connection instance.
     */
    public static final class VehicleServiceLocator {
        private VehicleServiceLocator() {}

        private static class InstanceHolder {
            private static final VehicleServiceLocator INSTANCE = new VehicleServiceLocator();
        }

        public static VehicleServiceLocator getInstance() {
            return InstanceHolder.INSTANCE;
        }

        public boolean isVhalConnected() {
            return true; // Simulates VHAL active state
        }
    }

    // --- 5. REPOSITORY PATTERN (Combining all 5 patterns into Caller Context) ---

    /**
     * REPOSITORY PATTERN: Encapsulates VHAL connection, Strategy selection, Adapter formatting,
     * and Observer dispatching into a clean high-level repository API.
     */
    public static class VehicleTelemetryRepository {
        private static final String TAG = VehicleTelemetryRepository.class.getSimpleName();

        private final VehicleServiceLocator mServiceLocator;
        private final List<OnTelemetryUpdateListener> mObservers = new CopyOnWriteArrayList<>();
        private Handler mMainHandler;
        private boolean mIsInitialized;

        public VehicleTelemetryRepository() {
            this.mServiceLocator = VehicleServiceLocator.getInstance();
        }

        public synchronized void init() {
            if (mIsInitialized) {
                Log.d(TAG, "Repository already initialized.");
                return;
            }
            this.mMainHandler = new Handler(Looper.getMainLooper());
            this.mIsInitialized = true;
            Log.d(TAG, "VehicleTelemetryRepository initialized with ServiceLocator VHAL status: " +
                    mServiceLocator.isVhalConnected());
        }

        public void registerObserver(OnTelemetryUpdateListener observer) {
            Objects.requireNonNull(observer, "Observer cannot be null");
            if (!mObservers.contains(observer)) {
                mObservers.add(observer);
                Log.d(TAG, "Registered new OnTelemetryUpdateListener observer.");
            }
        }

        public void unregisterObserver(OnTelemetryUpdateListener observer) {
            if (observer != null) {
                mObservers.remove(observer);
                Log.d(TAG, "Unregistered OnTelemetryUpdateListener observer.");
            }
        }

        /**
         * Simulates receiving raw VHAL property event and delegates processing to Strategy + Adapter + Observer.
         */
        public void onRawVhalEventReceived(int propertyId, Object rawValue) {
            if (!mIsInitialized) {
                Log.w(TAG, "Repository not initialized. Ignoring VHAL event.");
                return;
            }

            // 1. FACTORY + STRATEGY PATTERN: Fetch and execute signal strategy
            Optional<TelemetryProcessingStrategy> strategyOpt = TelemetryStrategyFactory.getStrategy(propertyId);
            String processedValue = strategyOpt
                    .map(strategy -> strategy.processSignal(rawValue))
                    .orElseGet(() -> String.valueOf(rawValue));

            // 2. ADAPTER PATTERN: Adapt processed value into clean DTO record
            VehicleTelemetryRecord record = VehicleDataFormatterAdapter.adapt(propertyId, processedValue);

            // 3. OBSERVER PATTERN: Notify observers on Main Thread
            postToMainThread(() -> notifyObservers(record));
        }

        private void notifyObservers(VehicleTelemetryRecord record) {
            for (OnTelemetryUpdateListener observer : mObservers) {
                try {
                    observer.onTelemetryUpdated(record);
                } catch (Throwable t) {
                    Log.e(TAG, "Error executing observer callback", t);
                }
            }
        }

        private void postToMainThread(Runnable runnable) {
            synchronized (this) {
                if (mMainHandler != null) {
                    mMainHandler.post(runnable);
                }
            }
        }

        public synchronized void release() {
            if (!mIsInitialized) {
                return;
            }
            mObservers.clear();
            if (mMainHandler != null) {
                mMainHandler.removeCallbacksAndMessages(null);
                mMainHandler = null;
            }
            mIsInitialized = false;
            Log.d(TAG, "VehicleTelemetryRepository released.");
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }
    }
}
