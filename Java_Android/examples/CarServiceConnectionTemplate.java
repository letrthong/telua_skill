/*
 * Copyright (C) 2026 letrthong@gmail.com
 * Created & Maintained by: letrthong@gmail.com
 * Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
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

import android.car.Car;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard Non-Blocking Android Automotive CarService Connection Template.
 *
 * AOSP Reference Source:
 * https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/services/Car/car-lib/src/android/car/Car.java
 *
 * Key Principles Demonstrated:
 * 1. Background Offloading: Executes Car.createCar(...) on a dedicated background thread executor
 *    (mCarConnectExecutor) to guarantee zero blocking on the caller / UI thread.
 * 2. Flexible Timeout Strategy: Supports both CAR_WAIT_TIMEOUT_WAIT_FOREVER (for System/Core Car Apps)
 *    and explicit timeout durations (e.g., 5000ms for Third-Party Apps per api_timeout_resilience_rule.md).
 * 3. Asynchronous Lifecycle Callback: Handles Car.createCar statusChangeListener (car, ready)
 *    to handle both connected (ready = true) and disconnected/crash (ready = false) states.
 * 4. Idempotent Lifecycle: Safe against duplicate init() and release() calls, with clean re-initialization.
 * 5. Safe Resource Teardown: Disconnects Car instance and shuts down background executors in release().
 */
public class CarServiceConnectionTemplate {
    private static final String TAG = CarServiceConnectionTemplate.class.getSimpleName();
    public static final long DEFAULT_SYSTEM_TIMEOUT_MS = Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER;
    public static final long DEFAULT_APP_TIMEOUT_MS = 5_000L;

    private ExecutorService mCarConnectExecutor;
    private Handler mMainHandler;

    private Car mCar;
    private CarPropertyManager mCarPropertyManager;
    
    private final AtomicBoolean mIsConnected = new AtomicBoolean(false);
    private boolean mIsInitialized;

    /**
     * Listener callback for receiving CarService connection status updates on the UI Main Thread.
     */
    public interface OnCarServiceListener {
        void onCarServiceConnected(Car car, CarPropertyManager propertyManager);
        void onCarServiceDisconnected();
        void onCarServiceError(Throwable throwable);
    }

    /**
     * Overloaded init using default system wait forever strategy (recommended for System/Core Car Apps).
     */
    public synchronized void init(Context context, OnCarServiceListener listener) {
        init(context, DEFAULT_SYSTEM_TIMEOUT_MS, listener);
    }

    /**
     * Initializes background executors and offloads Car.createCar(...) to a background thread with custom timeout.
     * Idempotent method: Safe to call multiple times or after release().
     *
     * @param context Application or Service Context.
     * @param waitTimeoutMs Connection wait timeout in milliseconds (or CAR_WAIT_TIMEOUT_WAIT_FOREVER).
     * @param listener Callback listener for connection state updates.
     */
    public synchronized void init(Context context, long waitTimeoutMs, OnCarServiceListener listener) {
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(listener, "OnCarServiceListener cannot be null");

        if (mIsInitialized) {
            Log.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        Context appContext = context.getApplicationContext();

        if (mCarConnectExecutor == null || mCarConnectExecutor.isShutdown()) {
            mCarConnectExecutor = Executors.newSingleThreadExecutor();
        }
        if (mMainHandler == null) {
            mMainHandler = new Handler(Looper.getMainLooper());
        }

        mIsInitialized = true;
        Log.d(TAG, "CarServiceConnectionTemplate initialized. Offloading Car.createCar to background thread with timeout: " + waitTimeoutMs + "ms");

        // Offload Car.createCar(...) to background thread executor to prevent blocking
        mCarConnectExecutor.execute(() -> connectToCarService(appContext, waitTimeoutMs, listener));
    }

    /**
     * Executes Car.createCar(...) on the background executor thread.
     *
     * Reference API:
     * https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/services/Car/car-lib/src/android/car/Car.java
     */
    private void connectToCarService(Context context, long waitTimeoutMs, OnCarServiceListener listener) {
        try {
            Log.d(TAG, "[BackgroundThread] Initiating Car.createCar asynchronous connection...");

            // Call Car.createCar on background thread with specified wait timeout
            mCar = Car.createCar(
                    context,
                    mMainHandler, // Handler to dispatch Car.createCar callbacks
                    waitTimeoutMs,
                    (car, ready) -> handleCarServiceLifecycle(car, ready, listener)
            );

        } catch (Exception e) {
            Log.e(TAG, "[BackgroundThread] Error during Car.createCar invocation", e);
            mIsConnected.set(false);
            postToMainThread(() -> listener.onCarServiceError(e));
        }
    }

    /**
     * Handles CarService lifecycle state changes triggered by Android Automotive.
     */
    private void handleCarServiceLifecycle(Car car, boolean ready, OnCarServiceListener listener) {
        synchronized (this) {
            if (ready && car != null) {
                mCar = car;
                mIsConnected.set(true);
                Log.d(TAG, "[CarLifecycle] CarService connected successfully! Ready = true.");

                try {
                    // Fetch CarPropertyManager from connected Car instance
                    mCarPropertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
                    
                    // Post success update back to UI Main Thread
                    postToMainThread(() -> listener.onCarServiceConnected(mCar, mCarPropertyManager));
                } catch (Exception e) {
                    Log.e(TAG, "[CarLifecycle] Failed to obtain CarPropertyManager", e);
                    postToMainThread(() -> listener.onCarServiceError(e));
                }
            } else {
                mIsConnected.set(false);
                mCarPropertyManager = null;
                Log.w(TAG, "[CarLifecycle] CarService disconnected or crashed! Ready = false.");

                // Post disconnect update back to UI Main Thread
                postToMainThread(() -> listener.onCarServiceDisconnected());
            }
        }
    }

    /**
     * Safely posts a Runnable operation to the Main UI Thread.
     */
    private void postToMainThread(Runnable runnable) {
        synchronized (this) {
            if (mMainHandler != null) {
                mMainHandler.post(runnable);
            }
        }
    }

    /**
     * Disconnects Car instance, shuts down background executors, and clears handlers.
     * Idempotent method: Safe to call multiple times. Reset state allows future re-initialization.
     */
    public synchronized void release() {
        if (!mIsInitialized) {
            Log.d(TAG, "Already released or not initialized. Skipping duplicate release() call.");
            return;
        }

        mIsConnected.set(false);

        if (mCar != null && mCar.isConnected()) {
            try {
                Log.d(TAG, "Disconnecting Car instance during release().");
                mCar.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting Car instance", e);
            }
            mCar = null;
        }
        mCarPropertyManager = null;

        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }

        if (mCarConnectExecutor != null) {
            mCarConnectExecutor.shutdownNow();
            mCarConnectExecutor = null;
        }

        mIsInitialized = false;
        Log.d(TAG, "CarServiceConnectionTemplate successfully released.");
    }

    public synchronized boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isConnected() {
        return mIsConnected.get();
    }

    public synchronized Car getCar() {
        return mCar;
    }

    public synchronized CarPropertyManager getCarPropertyManager() {
        return mCarPropertyManager;
    }
}
