package com.example.app.examples;

import android.util.Log;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Standard Listener Pattern Architecture Reference Template (Cloud Backend Domain).
 *
 * Pattern Architecture:
 * Source Service → Listener Interface → Owner Service
 *
 * Generic Cloud Domain Concrete Example:
 * CloudEventDispatcherService (Source Service) → CloudEventListener (Listener Interface) → CloudNotificationService (Owner Service)
 *
 * Key Principles Demonstrated:
 * 1. Source Service (CloudEventDispatcherService): Manages cloud telemetry event reception,
 *    listener subscription, and thread-safe event dispatching.
 * 2. Listener Interface (CloudEventListener): Contract defining event callbacks for inbound cloud data streams.
 * 3. Owner Service (CloudNotificationService): Implements CloudEventListener, registers with CloudEventDispatcherService,
 *    and processes alert notification and audit state logic.
 * 4. Thread-Safe Subscriptions: Uses CopyOnWriteArrayList for lock-free listener registration and iteration.
 * 5. Non-Blocking & Exception Isolation: Protects event publisher loop from exceptions thrown by subscribers.
 * 6. Idempotent Lifecycle: Symmetric init() and release() teardown logic.
 */
public class ListenerPatternTemplate {

    // Cloud Event Type Constants
    public static final int EVENT_TYPE_ALERT_HIGH = 100;
    public static final int EVENT_TYPE_ALERT_LOW = 101;
    public static final int EVENT_TYPE_TOGGLE_AUDIT = 102;
    public static final int ACTION_TRIGGER_EXECUTE = 0;
    public static final int ACTION_TRIGGER_CANCEL = 1;

    /**
     * 1. LISTENER INTERFACE
     * Contract implemented by Owner Service (CloudNotificationService) and observed by Source Service (CloudEventDispatcherService).
     */
    public interface CloudEventListener {
        /**
         * Called when a cloud event payload is received.
         *
         * @param eventType Cloud event type code (e.g. EVENT_TYPE_ALERT_HIGH).
         * @param actionCode Action trigger code (ACTION_TRIGGER_EXECUTE or ACTION_TRIGGER_CANCEL).
         */
        void onCloudEvent(int eventType, int actionCode);
    }

    /**
     * Data Holder representing a cloud telemetry event payload structure.
     */
    public static final class CloudEventPayload {
        private final int mEventType;
        private final int mActionCode;
        private final long mTimestamp;

        public CloudEventPayload(int eventType, int actionCode, long timestamp) {
            this.mEventType = eventType;
            this.mActionCode = actionCode;
            this.mTimestamp = timestamp;
        }

        public int getEventType() {
            return mEventType;
        }

        public int getActionCode() {
            return mActionCode;
        }

        public long getTimestamp() {
            return mTimestamp;
        }
    }

    /**
     * 2. SOURCE SERVICE: CloudEventDispatcherService
     * Responsible for capturing inbound cloud events and dispatching them to registered listeners.
     */
    public static class CloudEventDispatcherService {
        private static final String TAG = CloudEventDispatcherService.class.getSimpleName();
        private final List<CloudEventListener> mListeners = new CopyOnWriteArrayList<>();
        private boolean mIsInitialized;

        public synchronized void init() {
            if (mIsInitialized) {
                Log.d(TAG, "CloudEventDispatcherService already initialized.");
                return;
            }
            mIsInitialized = true;
            Log.d(TAG, "CloudEventDispatcherService initialized successfully.");
        }

        public void registerCloudEventListener(CloudEventListener listener) {
            Objects.requireNonNull(listener, "CloudEventListener cannot be null");
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
                Log.d(TAG, "Registered new CloudEventListener subscriber.");
            }
        }

        public void unregisterCloudEventListener(CloudEventListener listener) {
            if (listener != null) {
                mListeners.remove(listener);
                Log.d(TAG, "Unregistered CloudEventListener subscriber.");
            }
        }

        /**
         * Simulates cloud event ingestion and dispatches payload to registered listener interfaces.
         */
        public void dispatchCloudEvent(int eventType, int actionCode) {
            if (!mIsInitialized) {
                Log.w(TAG, "Cannot dispatch cloud event: CloudEventDispatcherService not initialized.");
                return;
            }

            Log.d(TAG, String.format("Dispatching CloudEvent: eventType=%d, actionCode=%d to %d listeners.",
                    eventType, actionCode, mListeners.size()));

            for (CloudEventListener listener : mListeners) {
                try {
                    listener.onCloudEvent(eventType, actionCode);
                } catch (Throwable t) {
                    Log.e(TAG, "Error executing cloud event listener callback", t);
                }
            }
        }

        /**
         * Non-blocking callback dispatching with custom Executor parameter.
         */
        public void dispatchCloudEventAsync(int eventType, int actionCode, Executor executor) {
            Objects.requireNonNull(executor, "Executor cannot be null");
            executor.execute(() -> dispatchCloudEvent(eventType, actionCode));
        }

        public synchronized void release() {
            if (!mIsInitialized) {
                return;
            }
            mListeners.clear();
            mIsInitialized = false;
            Log.d(TAG, "CloudEventDispatcherService released.");
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }
    }

    /**
     * 3. OWNER SERVICE: CloudNotificationService
     * Implements CloudEventListener, registers with CloudEventDispatcherService, and handles notification alerts.
     */
    public static class CloudNotificationService implements CloudEventListener {
        private static final String TAG = CloudNotificationService.class.getSimpleName();
        
        private final CloudEventDispatcherService mEventDispatcher;
        private int mAlertThreshold = 10;
        private boolean mIsAuditEnabled;
        private boolean mIsInitialized;

        public CloudNotificationService(CloudEventDispatcherService eventDispatcher) {
            this.mEventDispatcher = Objects.requireNonNull(eventDispatcher, "CloudEventDispatcherService cannot be null");
        }

        public synchronized void init() {
            if (mIsInitialized) {
                Log.d(TAG, "CloudNotificationService already initialized.");
                return;
            }
            mIsInitialized = true;
            // Owner Service registers itself with Source Service
            mEventDispatcher.registerCloudEventListener(this);
            Log.d(TAG, "CloudNotificationService initialized and subscribed to CloudEventDispatcherService.");
        }

        @Override
        public void onCloudEvent(int eventType, int actionCode) {
            if (actionCode != ACTION_TRIGGER_EXECUTE) {
                return; // Only process execute triggers
            }

            switch (eventType) {
                case EVENT_TYPE_ALERT_HIGH -> adjustThreshold(1);
                case EVENT_TYPE_ALERT_LOW -> adjustThreshold(-1);
                case EVENT_TYPE_TOGGLE_AUDIT -> toggleAuditMode();
                default -> Log.d(TAG, "Unhandled event type in CloudNotificationService: " + eventType);
            }
        }

        private void adjustThreshold(int delta) {
            mAlertThreshold = Math.max(0, Math.min(30, mAlertThreshold + delta));
            mIsAuditEnabled = false;
            Log.d(TAG, "Alert threshold adjusted to: " + mAlertThreshold);
        }

        private void toggleAuditMode() {
            mIsAuditEnabled = !mIsAuditEnabled;
            Log.d(TAG, "Cloud audit logging state toggled. IsAuditEnabled: " + mIsAuditEnabled);
        }

        public synchronized void release() {
            if (!mIsInitialized) {
                return;
            }
            // Unregister listener from Source Service during lifecycle teardown
            mEventDispatcher.unregisterCloudEventListener(this);
            mIsInitialized = false;
            Log.d(TAG, "CloudNotificationService released.");
        }

        public int getAlertThreshold() {
            return mAlertThreshold;
        }

        public boolean isAuditEnabled() {
            return mIsAuditEnabled;
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }
    }
}
