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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resilient Connection Sharing via Provider Indirection & Auto-Reconnect Template.
 *
 * <p>Demonstrates the "Provider Indirection Pattern", the cleanest alternative to manual listener re-sharing:
 * 1. ZERO Stale References: Consumer classes (ClientA, ClientB) NEVER cache the raw {@link ConnectionSession}
 *    reference in their own instance fields. Instead, they hold a reference to the {@link ConnectionProvider}.
 * 2. On-Demand Fetch via Volatile Local Copy: Every operation fetches the active session on demand
 *    via {@link #getActiveConnection()}, protected by the Local Copy idiom (Rule 1.1 in
 *    {@code thread_safety_concurrency_rule.md}).
 * 3. Transparent Auto-Reconnect: When a connection failure occurs, the Provider triggers an idempotent
 *    background reconnection. Once the new connection is published to {@code mActiveSession}, all consumer
 *    classes automatically and instantly begin using the new connection on their next call.
 * 4. Resilient Execution Wrapper: {@link #executeWithConnection(ConnectionAction)} automatically catches
 *    connection drops, marks the session as dead, triggers background reconnection, and isolates errors.
 * 5. Safe Singleton Instance: Follows Double-Checked Locking with Local Copy optimization.
 */
public class ResilientConnectionShareTemplate {
    private static final String TAG = ResilientConnectionShareTemplate.class.getSimpleName();

    /**
     * Functional interface representing an operation executed against an active connection.
     *
     * @param <T> the connection type
     */
    @FunctionalInterface
    public interface ConnectionAction<T> {
        void run(@NonNull T connection) throws Exception;
    }

    /**
     * Underlying connection session contract.
     */
    public interface ConnectionSession {
        boolean isAlive();
        void writeMessage(@NonNull String message) throws Exception;
        void disconnect();
    }

    // Singleton instance (DCL with Local Copy optimization)
    private static volatile ResilientConnectionShareTemplate sInstance;

    // Volatile reference to current active session -> Safe Publication across threads
    private volatile ConnectionSession mActiveSession;

    // Concurrency control flags
    private final AtomicBoolean mIsReconnecting = new AtomicBoolean(false);
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);

    private ExecutorService mIoExecutor;

    private ResilientConnectionShareTemplate() {}

    /**
     * Thread-safe Singleton access using Double-Checked Locking with Local Copy optimization.
     */
    @NonNull
    public static ResilientConnectionShareTemplate getInstance() {
        ResilientConnectionShareTemplate localInstance = sInstance; // Single volatile read
        if (localInstance == null) {
            synchronized (ResilientConnectionShareTemplate.class) {
                localInstance = sInstance;
                if (localInstance == null) {
                    sInstance = localInstance = new ResilientConnectionShareTemplate();
                }
            }
        }
        return localInstance;
    }

    /**
     * Initializes the manager and starts the initial background connection.
     * Idempotent per lifecycle_init_rule.md.
     */
    public synchronized void init() {
        if (mIsInitialized.get()) {
            AppLogger.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        if (mIoExecutor == null || mIoExecutor.isShutdown()) {
            mIoExecutor = Executors.newSingleThreadExecutor();
        }

        mIsInitialized.set(true);
        AppLogger.d(TAG, "ResilientConnectionShareTemplate initialized. Triggering initial connect.");
        triggerReconnect();
    }

    /**
     * Returns a stable snapshot of the active connection session, if connected.
     * Implements Rule 1.1 Volatile Local Copy Idiom.
     *
     * @return Optional containing active session, or empty if disconnected
     */
    @NonNull
    public Optional<ConnectionSession> getActiveConnection() {
        // Volatile Local Copy Pattern: Guarantees single main-memory read and consistent snapshot
        ConnectionSession localSession = mActiveSession;
        if (localSession != null && localSession.isAlive()) {
            return Optional.of(localSession);
        }
        return Optional.empty();
    }

    /**
     * Executes an action against the active connection. If connection is dead or fails mid-execution,
     * automatically triggers background reconnection.
     *
     * @param action the action to execute with active connection
     * @return {@code true} if action executed successfully, {@code false} otherwise
     */
    public boolean executeWithConnection(@NonNull ConnectionAction<ConnectionSession> action) {
        Objects.requireNonNull(action, "action must not be null");

        // Single read of volatile reference
        ConnectionSession localSession = mActiveSession;

        if (localSession == null || !localSession.isAlive()) {
            AppLogger.w(TAG, "Connection unavailable. Triggering background reconnect...");
            triggerReconnect();
            return false;
        }

        try {
            action.run(localSession);
            return true;
        } catch (Exception e) {
            AppLogger.e(TAG, "Connection action failed with error. Invalidating session & reconnecting...", e);
            handleConnectionFailure(localSession);
            return false;
        }
    }

    /**
     * Invalidates a failing session and requests a reconnection.
     */
    private void handleConnectionFailure(@NonNull ConnectionSession deadSession) {
        synchronized (this) {
            // Only nullify if mActiveSession is still the dead one (avoid overwriting a newer session)
            if (mActiveSession == deadSession) {
                mActiveSession = null;
                deadSession.disconnect();
            }
        }
        triggerReconnect();
    }

    /**
     * Triggers asynchronous reconnection. Idempotent via compareAndSet flag.
     */
    public void triggerReconnect() {
        if (!mIsInitialized.get()) {
            AppLogger.w(TAG, "Provider not initialized. Ignoring reconnect request.");
            return;
        }

        if (!mIsReconnecting.compareAndSet(false, true)) {
            AppLogger.d(TAG, "Reconnection already in progress. Duplicate call ignored.");
            return;
        }

        mIoExecutor.execute(this::performReconnection);
    }

    private void performReconnection() {
        try {
            AppLogger.d(TAG, "[WorkerThread] Connecting to target server/service...");

            // Simulate connection handshake
            ConnectionSession newSession = new SimulatedSession("car-vhal-bus.internal", 8888);

            // Safe Publication: atomic assignment to volatile field
            synchronized (this) {
                mActiveSession = newSession;
            }
            AppLogger.d(TAG, "[WorkerThread] New connection published. All consumer classes now have instant access.");

        } catch (Exception e) {
            AppLogger.e(TAG, "[WorkerThread] Reconnection failed", e);
        } finally {
            mIsReconnecting.set(false);
        }
    }

    /**
     * Cleans up resources and disconnects active session. Idempotent per lifecycle_init_rule.md.
     */
    public synchronized void release() {
        if (!mIsInitialized.get()) {
            AppLogger.d(TAG, "Already released or not initialized.");
            return;
        }

        ConnectionSession sessionToClose = mActiveSession;
        mActiveSession = null;
        if (sessionToClose != null) {
            sessionToClose.disconnect();
        }

        if (mIoExecutor != null) {
            mIoExecutor.shutdownNow();
            mIoExecutor = null;
        }

        mIsInitialized.set(false);
        AppLogger.d(TAG, "ResilientConnectionShareTemplate successfully released.");
    }

    // =========================================================================
    // CONCRETE CONSUMER CLIENTS (Zero Stale References via Provider Indirection)
    // =========================================================================

    /**
     * Consumer Client A: Audio Controller.
     * Holds NO raw Connection reference; calls Provider directly.
     */
    public static class AudioControllerA {
        private static final String CLIENT_TAG = AudioControllerA.class.getSimpleName();
        private final ResilientConnectionShareTemplate mConnectionProvider;

        public AudioControllerA(@NonNull ResilientConnectionShareTemplate provider) {
            this.mConnectionProvider = Objects.requireNonNull(provider, "provider cannot be null");
        }

        public void adjustVolume(int zoneId, int volumeLevel) {
            // Executes on-demand: Always gets latest active connection or triggers auto-reconnect
            boolean success = mConnectionProvider.executeWithConnection(session -> {
                session.writeMessage("SET_VOLUME zone=" + zoneId + " level=" + volumeLevel);
                AppLogger.d(CLIENT_TAG, "Volume adjustment transmitted successfully.");
            });

            if (!success) {
                AppLogger.w(CLIENT_TAG, "Volume command queued/skipped: Connection reconnecting.");
            }
        }
    }

    /**
     * Consumer Client B: Hvac Climate Controller.
     * Also shares the exact same Provider; immune to connection drops and recreation.
     */
    public static class HvacControllerB {
        private static final String CLIENT_TAG = HvacControllerB.class.getSimpleName();
        private final ResilientConnectionShareTemplate mConnectionProvider;

        public HvacControllerB(@NonNull ResilientConnectionShareTemplate provider) {
            this.mConnectionProvider = Objects.requireNonNull(provider, "provider cannot be null");
        }

        public void setCabinTemperature(float tempCelsius) {
            boolean success = mConnectionProvider.executeWithConnection(session -> {
                session.writeMessage("SET_TEMP temp=" + tempCelsius);
                AppLogger.d(CLIENT_TAG, "HVAC temperature transmitted successfully.");
            });

            if (!success) {
                AppLogger.w(CLIENT_TAG, "HVAC command deferred: Connection reconnecting.");
            }
        }
    }

    /**
     * Simulated connection session implementation.
     */
    private static class SimulatedSession implements ConnectionSession {
        private final String mHost;
        private final int mPort;
        private boolean mIsAlive;

        public SimulatedSession(String host, int port) {
            this.mHost = host;
            this.mPort = port;
            this.mIsAlive = true;
        }

        @Override
        public boolean isAlive() {
            return mIsAlive;
        }

        @Override
        public void writeMessage(@NonNull String message) throws Exception {
            if (!mIsAlive) {
                throw new IllegalStateException("Session disconnected from " + mHost + ":" + mPort);
            }
            AppLogger.d(TAG, "[Session %s:%d] Message dispatched: %s", mHost, mPort, message);
        }

        @Override
        public void disconnect() {
            mIsAlive = false;
            AppLogger.d(TAG, "[Session %s:%d] Disconnected.", mHost, mPort);
        }
    }
}
