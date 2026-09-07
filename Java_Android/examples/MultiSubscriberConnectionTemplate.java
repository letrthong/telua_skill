/*
 * Copyright (C) 2026 letrthong@gmail.com
 * Created & Maintained by: letrthong@gmail.com
 * Generated & Refactored by: Gemini 3.8 Pro (Google DeepMind)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.app.examples;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Multi-Subscriber Connection Sharing & Reconnection Benchmark Template.
 *
 * <p>Solves the classic "Stale Connection Bug" when multiple client classes (e.g. ClientA, ClientB)
 * share a single underlying connection resource (e.g. CarService, Bluetooth, Socket, Binder).
 *
 * <p>Key Principles Demonstrated:
 * 1. Push / Observer Re-share Pattern: Central manager maintains a thread-safe list of subscribers
 *    ({@link CopyOnWriteArrayList}) and broadcasts lifecycle events.
 * 2. Stale Reference Invalidation: When connection drops, subscribers are notified immediately
 *    to nullify their cached handle ({@code onConnectionLost()}).
 * 3. Atomic Re-share on Reconnect: When a new connection is established, the fresh connection handle
 *    is broadcast to all subscribers ({@code onConnectionRestored(newHandle)}), ensuring all classes
 *    seamlessly migrate away from the defunct connection.
 * 4. Late Subscriber Safety: Subscribing after a connection is already active immediately delivers
 *    the active connection handle without waiting for a reconnect cycle.
 * 5. Deadlock Prevention: Callbacks to subscribers are invoked strictly outside synchronization locks
 *    per {@code thread_safety_concurrency_rule.md} (Rule 1.5).
 */
public class MultiSubscriberConnectionTemplate {
    private static final String TAG = MultiSubscriberConnectionTemplate.class.getSimpleName();

    /**
     * Contract representing the underlying connection resource.
     */
    public interface ConnectionHandle {
        boolean isConnected();
        void transmitData(@NonNull String payload) throws Exception;
        void close();
    }

    /**
     * Callback contract for clients observing connection lifecycle transitions.
     */
    public interface ConnectionLifecycleSubscriber {
        /**
         * Invoked when a fresh connection has been established and published.
         *
         * @param handle the new active connection handle
         */
        void onConnectionRestored(@NonNull ConnectionHandle handle);

        /**
         * Invoked when the connection drops or is released.
         * Subscribers MUST invalidate/nullify any cached handle to avoid Stale Reference bugs.
         */
        void onConnectionLost();
    }

    // Thread-safe subscriber registry for lock-free multi-client traversal
    private final List<ConnectionLifecycleSubscriber> mSubscribers = new CopyOnWriteArrayList<>();

    // Volatile reference to current active connection (Safe Publication)
    private volatile ConnectionHandle mActiveConnection;

    private final AtomicBoolean mIsReconnecting = new AtomicBoolean(false);
    private ExecutorService mWorkerExecutor;
    private boolean mIsInitialized;

    /**
     * Initializes the connection manager. Idempotent per lifecycle_init_rule.md.
     */
    public synchronized void init() {
        if (mIsInitialized) {
            AppLogger.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        if (mWorkerExecutor == null || mWorkerExecutor.isShutdown()) {
            mWorkerExecutor = Executors.newSingleThreadExecutor();
        }

        mIsInitialized = true;
        AppLogger.d(TAG, "MultiSubscriberConnectionTemplate initialized.");
    }

    /**
     * Registers a subscriber (e.g. ClientA or ClientB).
     * If a connection is already alive, immediately dispatches the active handle (Late Subscriber pattern).
     *
     * @param subscriber client listening for connection updates
     */
    public void registerSubscriber(@NonNull ConnectionLifecycleSubscriber subscriber) {
        Objects.requireNonNull(subscriber, "subscriber must not be null");

        if (!mSubscribers.contains(subscriber)) {
            mSubscribers.add(subscriber);
            AppLogger.d(TAG, "Registered new ConnectionLifecycleSubscriber: %s", subscriber.getClass().getSimpleName());
        }

        // Late Subscriber Pattern: If connection is already up, publish immediately
        ConnectionHandle snapshot = mActiveConnection;
        if (snapshot != null && snapshot.isConnected()) {
            try {
                subscriber.onConnectionRestored(snapshot);
            } catch (Exception e) {
                AppLogger.e(TAG, "Error notifying late subscriber on initial connect", e);
            }
        }
    }

    /**
     * Unregisters a subscriber during its lifecycle teardown.
     *
     * @param subscriber client to remove
     */
    public void unregisterSubscriber(@Nullable ConnectionLifecycleSubscriber subscriber) {
        if (subscriber != null) {
            mSubscribers.remove(subscriber);
            AppLogger.d(TAG, "Unregistered ConnectionLifecycleSubscriber: %s", subscriber.getClass().getSimpleName());
        }
    }

    /**
     * Triggers asynchronous reconnection (e.g. after connection drop or error).
     * Idempotent & thread-safe: guarantees only 1 reconnection task runs at a time.
     */
    public void reconnect() {
        if (!mIsInitialized) {
            AppLogger.w(TAG, "Manager not initialized. Cannot reconnect.");
            return;
        }

        if (!mIsReconnecting.compareAndSet(false, true)) {
            AppLogger.d(TAG, "Reconnection already in progress. Ignoring duplicate request.");
            return;
        }

        mWorkerExecutor.execute(this::performReconnectionSequence);
    }

    /**
     * Background execution sequence for connection teardown, recreation, and multi-client re-sharing.
     */
    private void performReconnectionSequence() {
        try {
            AppLogger.d(TAG, "[WorkerThread] Starting reconnection sequence...");

            // 1. Invalidate and close stale connection
            ConnectionHandle staleHandle;
            synchronized (this) {
                staleHandle = mActiveConnection;
                mActiveConnection = null;
            }

            if (staleHandle != null) {
                staleHandle.close();
            }

            // 2. Notify all subscribers that connection is lost (invalidate cached references)
            // Dispatched strictly outside synchronized block to prevent deadlocks
            broadcastConnectionLost();

            // 3. Instantiate fresh connection handle (simulated connection establishment)
            ConnectionHandle freshConnection = new SimulatedConnectionHandle("192.168.1.50", 9000);

            // 4. Atomically publish fresh connection
            synchronized (this) {
                mActiveConnection = freshConnection;
            }
            AppLogger.d(TAG, "[WorkerThread] New connection established successfully!");

            // 5. Broadcast new connection to all subscribers (Class A, Class B, etc.)
            broadcastConnectionRestored(freshConnection);

        } catch (Exception e) {
            AppLogger.e(TAG, "[WorkerThread] Failed to reconnect", e);
        } finally {
            mIsReconnecting.set(false);
        }
    }

    private void broadcastConnectionLost() {
        for (ConnectionLifecycleSubscriber subscriber : mSubscribers) {
            try {
                subscriber.onConnectionLost();
            } catch (Exception e) {
                AppLogger.e(TAG, "Subscriber threw exception during onConnectionLost", e);
            }
        }
    }

    private void broadcastConnectionRestored(@NonNull ConnectionHandle handle) {
        for (ConnectionLifecycleSubscriber subscriber : mSubscribers) {
            try {
                subscriber.onConnectionRestored(handle);
            } catch (Exception e) {
                AppLogger.e(TAG, "Subscriber threw exception during onConnectionRestored", e);
            }
        }
    }

    /**
     * Shuts down manager and notifies all subscribers. Idempotent per lifecycle_init_rule.md.
     */
    public synchronized void release() {
        if (!mIsInitialized) {
            AppLogger.d(TAG, "Already released or not initialized.");
            return;
        }

        ConnectionHandle connectionToClose = mActiveConnection;
        mActiveConnection = null;
        if (connectionToClose != null) {
            connectionToClose.close();
        }

        broadcastConnectionLost();
        mSubscribers.clear();

        if (mWorkerExecutor != null) {
            mWorkerExecutor.shutdownNow();
            mWorkerExecutor = null;
        }

        mIsInitialized = false;
        AppLogger.d(TAG, "MultiSubscriberConnectionTemplate successfully released.");
    }

    // =========================================================================
    // CONCRETE CLIENT IMPLEMENTATIONS (Demonstrating Stale Reference Prevention)
    // =========================================================================

    /**
     * Concrete Client A: Telemetry Transmission Service.
     * Demonstrates safe connection caching and atomic reference swap on reconnect.
     */
    public static class TelemetryServiceA implements ConnectionLifecycleSubscriber {
        private static final String CLIENT_TAG = TelemetryServiceA.class.getSimpleName();
        private volatile ConnectionHandle mCachedConnection;

        @Override
        public void onConnectionRestored(@NonNull ConnectionHandle handle) {
            this.mCachedConnection = Objects.requireNonNull(handle, "handle cannot be null");
            AppLogger.d(CLIENT_TAG, "Received FRESH connection handle. Cache updated.");
        }

        @Override
        public void onConnectionLost() {
            this.mCachedConnection = null;
            AppLogger.w(CLIENT_TAG, "Connection lost! Nullified cached handle to prevent stale calls.");
        }

        public void sendTelemetry(@NonNull String metric) {
            // Local copy snapshot pattern prevents TOCTOU
            ConnectionHandle localHandle = mCachedConnection;
            if (localHandle == null || !localHandle.isConnected()) {
                AppLogger.w(CLIENT_TAG, "Cannot send telemetry: Connection not available or dead.");
                return;
            }

            try {
                localHandle.transmitData("Telemetry: " + metric);
            } catch (Exception e) {
                AppLogger.e(CLIENT_TAG, "Transmission failed", e);
            }
        }
    }

    /**
     * Concrete Client B: Diagnostics & Health Monitor.
     * Demonstrates independent subscriber receiving the exact same fresh connection instance.
     */
    public static class DiagnosticsServiceB implements ConnectionLifecycleSubscriber {
        private static final String CLIENT_TAG = DiagnosticsServiceB.class.getSimpleName();
        private volatile ConnectionHandle mCachedConnection;

        @Override
        public void onConnectionRestored(@NonNull ConnectionHandle handle) {
            this.mCachedConnection = handle;
            AppLogger.d(CLIENT_TAG, "Diagnostics received FRESH connection. Resuming health checks.");
        }

        @Override
        public void onConnectionLost() {
            this.mCachedConnection = null;
            AppLogger.w(CLIENT_TAG, "Diagnostics paused: Connection lost.");
        }

        public void pingHealth() {
            ConnectionHandle localHandle = mCachedConnection;
            if (localHandle != null && localHandle.isConnected()) {
                try {
                    localHandle.transmitData("PING_DIAGNOSTICS");
                } catch (Exception e) {
                    AppLogger.e(CLIENT_TAG, "Ping failed", e);
                }
            }
        }
    }

    /**
     * Simulated connection handle implementation for testing and demonstration.
     */
    private static class SimulatedConnectionHandle implements ConnectionHandle {
        private final String mEndpoint;
        private final int mPort;
        private boolean mIsConnected;

        public SimulatedConnectionHandle(String endpoint, int port) {
            this.mEndpoint = endpoint;
            this.mPort = port;
            this.mIsConnected = true;
        }

        @Override
        public boolean isConnected() {
            return mIsConnected;
        }

        @Override
        public void transmitData(@NonNull String payload) throws Exception {
            if (!mIsConnected) {
                throw new IllegalStateException("Socket is closed to " + mEndpoint + ":" + mPort);
            }
            AppLogger.d(TAG, "[Connection %s:%d] Sent payload: %s", mEndpoint, mPort, payload);
        }

        @Override
        public void close() {
            mIsConnected = false;
            AppLogger.d(TAG, "[Connection %s:%d] Closed cleanly.", mEndpoint, mPort);
        }
    }
}
