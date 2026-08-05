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

import android.car.VehicleAreaType;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard Android Automotive CarPropertyManager VHAL Subscription Template.
 *
 * AOSP Reference Source:
 * https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/services/Car/car-lib/src/android/car/hardware/property/CarPropertyManager.java
 *
 * Key Principles Demonstrated:
 * 1. Asynchronous VHAL Property Subscription: Subscribes to vehicle properties (e.g. Speed, HVAC, EV Battery)
 *    using CarPropertyManager.CarPropertyEventCallback.
 * 2. Rate Limit Optimization: Configures proper sensor rates (SENSOR_RATE_ONCHANGE vs update rate Hz)
 *    to prevent CPU spikes and IPC overload.
 * 3. Thread-Safe Event Dispatching: Uses ConcurrentHashMap for registered property tracking and posts UI updates to Main Looper.
 * 4. Error & Resilience Handling: Gracefully handles VHAL error events (onErrorEvent) and property access exceptions.
 * 5. Idempotent Teardown: Unregisters all callbacks in release() to guarantee zero resource leaks or orphan VHAL listeners.
 */
public class CarPropertySubscriptionTemplate {
    private static final String TAG = CarPropertySubscriptionTemplate.class.getSimpleName();
    public static final float SENSOR_RATE_ONCHANGE = CarPropertyManager.SENSOR_RATE_ONCHANGE;
    public static final float SENSOR_RATE_FAST_HZ = 10.0f; // 10Hz updates for vehicle speed

    private CarPropertyManager mCarPropertyManager;
    private Handler mMainHandler;
    private ExecutorService mWorkerExecutor;

    private final Set<Integer> mSubscribedProperties = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean mIsSubscribed = new AtomicBoolean(false);
    private boolean mIsInitialized;

    /**
     * Listener callback interface for receiving vehicle property change events on the UI Main Thread.
     */
    public interface OnVehiclePropertyChangeListener {
        void onPropertyChanged(int propertyId, int areaId, Object value);
        void onPropertyError(int propertyId, int areaId, int errorCode);
    }

    /**
     * Internal CarPropertyEventCallback implementation delegating events to UI listeners.
     */
    private final CarPropertyManager.CarPropertyEventCallback mCarPropertyCallback =
            new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue value) {
                    if (value == null) {
                        return;
                    }
                    int propId = value.getPropertyId();
                    int areaId = value.getAreaId();
                    Object propValue = value.getValue();

                    Log.d(TAG, String.format("VHAL Property Change: propId=0x%X, areaId=%d, value=%s",
                            propId, areaId, propValue));

                    postToMainThread(() -> dispatchPropertyChanged(propId, areaId, propValue));
                }

                @Override
                public void onErrorEvent(int propertyId, int zone) {
                    Log.e(TAG, String.format("VHAL Property Error: propId=0x%X, zone/areaId=%d", propertyId, zone));
                    postToMainThread(() -> dispatchPropertyError(propertyId, zone, -1));
                }
            };

    private OnVehiclePropertyChangeListener mUserListener;

    /**
     * Initializes CarPropertySubscriptionTemplate and subscribes to default vehicle properties.
     * Idempotent method: Safe against duplicate init calls.
     *
     * @param propertyManager Connected CarPropertyManager instance.
     * @param listener Callback listener for UI updates.
     */
    public synchronized void init(CarPropertyManager propertyManager, OnVehiclePropertyChangeListener listener) {
        Objects.requireNonNull(propertyManager, "CarPropertyManager cannot be null");
        Objects.requireNonNull(listener, "OnVehiclePropertyChangeListener cannot be null");

        if (mIsInitialized) {
            Log.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        this.mCarPropertyManager = propertyManager;
        this.mUserListener = listener;
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mWorkerExecutor = Executors.newSingleThreadExecutor();
        this.mIsInitialized = true;

        Log.d(TAG, "CarPropertySubscriptionTemplate initialized. Registering default VHAL property subscriptions...");

        // Offload property registration to worker thread executor to prevent blocking caller
        mWorkerExecutor.execute(this::subscribeDefaultVehicleProperties);
    }

    /**
     * Subscribes to standard vehicle properties on background thread executor.
     */
    private void subscribeDefaultVehicleProperties() {
        // Subscribe to Vehicle Speed (Fast 10Hz updates)
        subscribeProperty(VehiclePropertyIds.PER_VEHICLE_SPEED, SENSOR_RATE_FAST_HZ);

        // Subscribe to HVAC Target Temperature (On-Change updates)
        subscribeProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, SENSOR_RATE_ONCHANGE);

        // Subscribe to EV Battery Level (On-Change updates)
        subscribeProperty(VehiclePropertyIds.EV_BATTERY_LEVEL, SENSOR_RATE_ONCHANGE);

        mIsSubscribed.set(true);
        Log.d(TAG, "All default VHAL properties subscribed successfully.");
    }

    /**
     * Subscribes a specific VHAL property with a target update rate.
     *
     * @param propertyId AOSP VehiclePropertyIds constant.
     * @param rateHz Update frequency rate in Hz (or SENSOR_RATE_ONCHANGE).
     */
    public void subscribeProperty(int propertyId, float rateHz) {
        synchronized (this) {
            if (!mIsInitialized || mCarPropertyManager == null) {
                Log.w(TAG, "Cannot subscribe property 0x" + Integer.toHexString(propertyId) + ": Not initialized.");
                return;
            }

            try {
                boolean registered = mCarPropertyManager.registerCallback(
                        mCarPropertyCallback,
                        propertyId,
                        rateHz
                );

                if (registered) {
                    mSubscribedProperties.add(propertyId);
                    Log.d(TAG, String.format("Successfully registered VHAL property 0x%X at rate %.1f Hz",
                            propertyId, rateHz));
                } else {
                    Log.w(TAG, "CarPropertyManager.registerCallback returned false for property 0x" +
                            Integer.toHexString(propertyId));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to register VHAL property callback for 0x" + Integer.toHexString(propertyId), e);
            }
        }
    }

    /**
     * Unsubscribes a specific VHAL property.
     */
    public void unsubscribeProperty(int propertyId) {
        synchronized (this) {
            if (!mIsInitialized || mCarPropertyManager == null) {
                return;
            }

            try {
                mCarPropertyManager.unregisterCallback(mCarPropertyCallback, propertyId);
                mSubscribedProperties.remove(propertyId);
                Log.d(TAG, "Unregistered VHAL property: 0x" + Integer.toHexString(propertyId));
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering VHAL property 0x" + Integer.toHexString(propertyId), e);
            }
        }
    }

    private void dispatchPropertyChanged(int propertyId, int areaId, Object value) {
        if (mUserListener != null) {
            mUserListener.onPropertyChanged(propertyId, areaId, value);
        }
    }

    private void dispatchPropertyError(int propertyId, int areaId, int errorCode) {
        if (mUserListener != null) {
            mUserListener.onPropertyError(propertyId, areaId, errorCode);
        }
    }

    private void postToMainThread(Runnable runnable) {
        synchronized (this) {
            if (mMainHandler != null) {
                mMainHandler.post(runnable);
            }
        }
    }

    /**
     * Unregisters all VHAL property callbacks, shuts down background executors, and clears references.
     * Idempotent teardown method: Safe to call multiple times.
     */
    public synchronized void release() {
        if (!mIsInitialized) {
            Log.d(TAG, "Already released or not initialized. Skipping duplicate release() call.");
            return;
        }

        Log.d(TAG, "Releasing CarPropertySubscriptionTemplate. Unregistering " +
                mSubscribedProperties.size() + " VHAL property callbacks...");

        if (mCarPropertyManager != null) {
            try {
                // Unregister all callbacks to prevent memory leaks and VHAL IPC overhead
                mCarPropertyManager.unregisterCallback(mCarPropertyCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering CarPropertyManager callbacks during release", e);
            }
        }

        mSubscribedProperties.clear();
        mIsSubscribed.set(false);

        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }

        if (mWorkerExecutor != null) {
            mWorkerExecutor.shutdownNow();
            mWorkerExecutor = null;
        }

        mCarPropertyManager = null;
        mUserListener = null;
        mIsInitialized = false;

        Log.d(TAG, "CarPropertySubscriptionTemplate successfully released.");
    }

    public synchronized boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isSubscribed() {
        return mIsSubscribed.get();
    }

    public Set<Integer> getSubscribedProperties() {
        return Set.copyOf(mSubscribedProperties);
    }
}
