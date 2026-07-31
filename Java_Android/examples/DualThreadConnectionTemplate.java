package com.example.app.examples;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard Dual-Thread Sequential Connection & Event Receiver Template.
 *
 * Demonstrates a clean event-driven dual-thread architecture:
 * 1. Connection Worker (mConnectExecutor): Executes the backend connection sequence.
 * 2. Event Receiver Worker (mEventReceiverExecutor): ONLY spawned/submitted from within
 *    the ConnectThread AFTER the backend connection is successfully established.
 * 3. Resource Efficiency: Prevents unnecessary background threads from waiting when offline or when connection fails.
 * 4. Idempotent Lifecycle: Safe against duplicate init() / release() calls and allows clean re-initialization.
 * 5. ANR Prevention & Main Thread Dispatch: Offloads all I/O to background threads and dispatches
 *    received events back to the UI Main Thread via Handler(Looper.getMainLooper()).
 */
public class DualThreadConnectionTemplate {
    private static final String TAG = DualThreadConnectionTemplate.class.getSimpleName();

    private ExecutorService mConnectExecutor;
    private ExecutorService mEventReceiverExecutor;
    private Handler mMainHandler;

    private final AtomicBoolean mIsConnected = new AtomicBoolean(false);
    private final AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private boolean mIsInitialized;

    /**
     * Callback interface for receiving event notifications on the Main UI Thread.
     */
    public interface OnEventListener {
        void onEventReceived(String eventData);
        void onConnectionReady();
        void onConnectionError(Throwable error);
    }

    /**
     * Initializes thread executors, handlers, and triggers the connection thread.
     * Idempotent method: Safe to call multiple times or after release().
     *
     * @param listener Listener callback for event and connection updates.
     */
    public synchronized void init(OnEventListener listener) {
        Objects.requireNonNull(listener, "OnEventListener cannot be null");

        if (mIsInitialized) {
            Log.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        mIsConnected.set(false);
        mIsRunning.set(true);

        if (mConnectExecutor == null || mConnectExecutor.isShutdown()) {
            mConnectExecutor = Executors.newSingleThreadExecutor();
        }
        if (mEventReceiverExecutor == null || mEventReceiverExecutor.isShutdown()) {
            mEventReceiverExecutor = Executors.newSingleThreadExecutor();
        }
        if (mMainHandler == null) {
            mMainHandler = new Handler(Looper.getMainLooper());
        }

        mIsInitialized = true;
        Log.d(TAG, "DualThreadConnectionTemplate initialized. Starting connection sequence.");

        // Step 1: Launch Connection Thread Task ONLY.
        // Event Receiver Thread will be launched inside ConnectThread AFTER connection succeeds.
        mConnectExecutor.execute(() -> runConnectTask(listener));
    }

    /**
     * Connection Worker Task: Connects to the backend service/socket/VHAL.
     * UPON SUCCESSFUL CONNECTION: Spawns the Event Receiver Thread task (mEventReceiverExecutor).
     */
    private void runConnectTask(OnEventListener listener) {
        try {
            Log.d(TAG, "[ConnectThread] Initiating backend connection...");
            
            // Simulate network / backend connection latency (e.g., 1.5 seconds)
            Thread.sleep(1500);

            mIsConnected.set(true);
            Log.d(TAG, "[ConnectThread] Backend connection established successfully.");

            // Notify UI Main Thread that connection is ready
            postToMainThread(() -> listener.onConnectionReady());

            // Step 2: Launch Event Receiver Thread Task ONLY after connection success!
            synchronized (this) {
                if (mIsRunning.get() && mEventReceiverExecutor != null && !mEventReceiverExecutor.isShutdown()) {
                    Log.d(TAG, "[ConnectThread] Triggering Event Receiver Thread now that backend is ready.");
                    mEventReceiverExecutor.execute(() -> runEventReceiverTask(listener));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "[ConnectThread] Connection task interrupted", e);
            mIsConnected.set(false);
            postToMainThread(() -> listener.onConnectionError(e));
        } catch (Exception e) {
            Log.e(TAG, "[ConnectThread] Failed to connect to backend", e);
            mIsConnected.set(false);
            postToMainThread(() -> listener.onConnectionError(e));
        }
    }

    /**
     * Event Receiver Worker Task: Executes event listening loop once backend connection is active.
     */
    private void runEventReceiverTask(OnEventListener listener) {
        try {
            Log.d(TAG, "[EventThread] Event receiver worker started. Listening for incoming events...");

            int eventCounter = 1;
            while (mIsRunning.get() && mIsConnected.get() && !Thread.currentThread().isInterrupted()) {
                // Simulate waiting for incoming data from socket / VHAL / backend
                Thread.sleep(2000);

                String eventPayload = "EVENT_PAYLOAD_#" + (eventCounter++);
                Log.d(TAG, "[EventThread] Received event: " + eventPayload);

                // Safely post received event to UI Main Thread
                postToMainThread(() -> listener.onEventReceived(eventPayload));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.d(TAG, "[EventThread] Event receiver loop interrupted.");
        } catch (Exception e) {
            Log.e(TAG, "[EventThread] Error in event receiver loop", e);
            postToMainThread(() -> listener.onConnectionError(e));
        }
    }

    /**
     * Safely posts a Runnable operation back to the Main UI Thread.
     */
    private void postToMainThread(Runnable runnable) {
        synchronized (this) {
            if (mMainHandler != null) {
                mMainHandler.post(runnable);
            }
        }
    }

    /**
     * Shuts down executors, cancels pending callbacks, and resets connection state.
     * Idempotent method: Safe to call multiple times. Reset state allows future re-initialization.
     */
    public synchronized void release() {
        if (!mIsInitialized) {
            Log.d(TAG, "Already released or not initialized. Skipping duplicate release() call.");
            return;
        }

        mIsRunning.set(false);
        mIsConnected.set(false);

        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }

        if (mConnectExecutor != null) {
            mConnectExecutor.shutdownNow();
            mConnectExecutor = null;
        }

        if (mEventReceiverExecutor != null) {
            mEventReceiverExecutor.shutdownNow();
            mEventReceiverExecutor = null;
        }

        mIsInitialized = false;
        Log.d(TAG, "DualThreadConnectionTemplate successfully released.");
    }

    public synchronized boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isConnected() {
        return mIsConnected.get();
    }
}
