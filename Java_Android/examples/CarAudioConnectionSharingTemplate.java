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
import android.car.media.CarAudioManager;
import android.car.media.CarAudioManager.CarVolumeCallback;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Concise Benchmark Template: Car.createCar + CarAudioManager Sharing to Class A & Class B.
 *
 * <p>Demonstrates the single-thread model solving the AOSP CarAudioService restart problem:
 * 1. Runs completely on a SINGLE background {@link HandlerThread} (Zero Main Thread ANR risk).
 * 2. Directly connects to {@link Car#createCar} with the background handler.
 * 3. When CarService connects or restarts (ready = true), fetches the fresh {@link CarAudioManager}
 *    and immediately pushes it to both Class A ({@link ModuleA}) and Class B ({@link ServiceB}).
 * 4. Module A automatically re-registers its {@link CarVolumeCallback} on the new binder.
 * 5. When CarService disconnects or crashes (ready = false), immediately pushes {@code null}
 *    to both classes to invalidate stale references and prevent DeadObjectException.
 */
public class CarAudioConnectionSharingTemplate {
    private static final String TAG = CarAudioConnectionSharingTemplate.class.getSimpleName();

    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;
    private Car mCar;

    // Directly references Class A and Class B (Clean and straightforward)
    private ModuleA mModuleA;
    private ServiceB mServiceB;

    /**
     * Initializes the connection pipeline on a single dedicated background thread.
     *
     * @param context  Application Context
     * @param moduleA  Client Class A (Volume Control)
     * @param serviceB Client Class B (Media Routing)
     */
    public synchronized void init(@NonNull Context context, @NonNull ModuleA moduleA, @NonNull ServiceB serviceB) {
        Objects.requireNonNull(context, "context must not be null");
        this.mModuleA = Objects.requireNonNull(moduleA, "moduleA must not be null");
        this.mServiceB = Objects.requireNonNull(serviceB, "serviceB must not be null");

        if (mWorkerThread != null && mWorkerThread.isAlive()) {
            AppLogger.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        // 1. Start single dedicated background thread
        mWorkerThread = new HandlerThread("CarAudioWorkerThread");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        Context appContext = context.getApplicationContext();

        // 2. Offload Car.createCar directly onto this worker thread
        mWorkerHandler.post(() -> {
            AppLogger.d(TAG, "[WorkerThread] Connecting to CarService via Car.createCar...");
            mCar = Car.createCar(
                    appContext,
                    mWorkerHandler, // Ensures callbacks fire on this exact worker thread
                    Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
                    (car, ready) -> onCarLifecycleChanged(car, ready)
            );
        });
    }

    /**
     * Handles CarService connection & restart lifecycle sequentially on the single worker thread.
     */
    private void onCarLifecycleChanged(@Nullable Car car, boolean ready) {
        if (ready && car != null) {
            // CARSERVICE IS READY / RECONNECTED: Obtain fresh CarAudioManager
            CarAudioManager audioManager = (CarAudioManager) car.getCarManager(Car.AUDIO_SERVICE);
            AppLogger.d(TAG, "[WorkerThread] CarAudioService Ready -> Re-sharing fresh CarAudioManager to Class A & B");

            // Push fresh instance directly to Class A & Class B
            mModuleA.onCarAudioUpdated(audioManager);
            mServiceB.onCarAudioUpdated(audioManager);
        } else {
            // CARSERVICE CRASHED / DISCONNECTED: Revoke CarAudioManager to prevent dead calls
            AppLogger.w(TAG, "[WorkerThread] CarAudioService Disconnected -> Revoking CarAudioManager from Class A & B");

            mModuleA.onCarAudioUpdated(null);
            mServiceB.onCarAudioUpdated(null);
        }
    }

    /**
     * Cleans up connection and terminates worker thread. Idempotent per lifecycle_init_rule.md.
     */
    public synchronized void release() {
        if (mCar != null && mCar.isConnected()) {
            try {
                mCar.disconnect();
            } catch (Exception e) {
                AppLogger.e(TAG, "Error disconnecting Car", e);
            }
            mCar = null;
        }

        if (mWorkerHandler != null) {
            mWorkerHandler.removeCallbacksAndMessages(null);
            mWorkerHandler = null;
        }

        if (mWorkerThread != null) {
            mWorkerThread.quitSafely(); // Clean teardown of single worker thread
            mWorkerThread = null;
        }

        AppLogger.d(TAG, "CarAudioConnectionSharingTemplate released.");
    }

    // =========================================================================
    // CONCRETE CLIENT CLASS A & CLASS B IMPLEMENTATIONS
    // =========================================================================

    /**
     * Class A: Volume Control Module (Registers CarVolumeCallback).
     */
    public static class ModuleA {
        private static final String CLIENT_TAG = ModuleA.class.getSimpleName();
        private volatile CarAudioManager mCarAudio;

        private final CarVolumeCallback mVolumeCallback = new CarVolumeCallback() {
            @Override
            public void onGroupVolumeChanged(int zoneId, int groupId, int flags) {
                AppLogger.d(CLIENT_TAG, "[Callback] Volume changed: Zone %d Group %d", zoneId, groupId);
            }

            @Override
            public void onMasterMuteChanged(int zoneId, int flags) {
                AppLogger.d(CLIENT_TAG, "[Callback] Master mute changed for Zone %d", zoneId);
            }
        };

        /**
         * Called when CarAudioService connects, restarts, or disconnects.
         */
        public void onCarAudioUpdated(@Nullable CarAudioManager newManager) {
            this.mCarAudio = newManager;

            if (newManager != null) {
                // CRITICAL IN AOSP: Automatically re-register callback with the fresh Binder instance
                try {
                    newManager.registerCarVolumeCallback(mVolumeCallback);
                    AppLogger.d(CLIENT_TAG, "CarVolumeCallback successfully re-registered on fresh CarAudioManager.");
                } catch (Exception e) {
                    AppLogger.e(CLIENT_TAG, "Failed to re-register volume callback", e);
                }
            } else {
                AppLogger.w(CLIENT_TAG, "CarAudioService disconnected. Reference nullified safely.");
            }
        }

        public void setVolume(int groupId, int volumeIndex) {
            CarAudioManager local = mCarAudio;
            if (local != null) {
                try {
                    local.setGroupVolume(CarAudioManager.PRIMARY_AUDIO_ZONE, groupId, volumeIndex, 0);
                } catch (Exception e) {
                    AppLogger.e(CLIENT_TAG, "Error setting volume", e);
                }
            }
        }
    }

    /**
     * Class B: Media Routing & Acoustics Service.
     */
    public static class ServiceB {
        private static final String CLIENT_TAG = ServiceB.class.getSimpleName();
        private volatile CarAudioManager mCarAudio;

        /**
         * Called when CarAudioService connects, restarts, or disconnects.
         */
        public void onCarAudioUpdated(@Nullable CarAudioManager newManager) {
            this.mCarAudio = newManager;
            if (newManager != null) {
                AppLogger.d(CLIENT_TAG, "Received fresh CarAudioManager. Ready to adjust cabin acoustics.");
            } else {
                AppLogger.w(CLIENT_TAG, "CarAudioService disconnected. Reference nullified.");
            }
        }

        public void adjustCabinFade(float frontValue) {
            CarAudioManager local = mCarAudio;
            if (local != null) {
                try {
                    local.setFadeTowardFront(frontValue);
                } catch (Exception e) {
                    AppLogger.e(CLIENT_TAG, "Error adjusting cabin fade", e);
                }
            }
        }
    }
}
