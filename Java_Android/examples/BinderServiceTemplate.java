package com.example.app.examples;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standard AIDL Binder System Service Reference Template.
 *
 * AOSP Reference Sources:
 * https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/core/java/android/os/RemoteCallbackList.java
 * https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/services/Car/service/src/com/android/car/
 *
 * Key Principles Demonstrated:
 * 1. Binder Thread Safety: All heavy processing is immediately offloaded from the Binder thread
 *    pool to a dedicated background ExecutorService. The Binder thread returns in < 5ms.
 * 2. RemoteCallbackList: Thread-safe multi-client callback management with automatic dead-client
 *    cleanup on beginBroadcast().
 * 3. Binder Death Recipient: linkToDeath() monitors for unexpected client process death,
 *    triggering automatic cleanup to prevent resource leaks.
 * 4. Parcelable IPC Data: VehicleData implements Parcelable with strict field-order consistency
 *    between writeToParcel() and the Parcel constructor.
 * 5. IPC Security: enforceCallingOrSelfPermission() guards every sensitive AIDL stub entry point.
 * 6. Identity Clearing: Binder.clearCallingIdentity() prevents privilege escalation when the
 *    service makes self-calls from within an IPC handler.
 */
public class BinderServiceTemplate {
    private static final String TAG = BinderServiceTemplate.class.getSimpleName();
    private static final String REQUIRED_PERMISSION = "android.permission.CAR_SPEED";

    // -----------------------------------------------------------------------------------------
    // 1. PARCELABLE DATA TRANSFER OBJECT
    // -----------------------------------------------------------------------------------------

    /**
     * Immutable Parcelable DTO for transporting vehicle data across Binder transactions.
     * Field write/read order in writeToParcel() and Parcel constructor MUST be identical.
     */
    public static final class VehicleData implements Parcelable {
        private final float mSpeedKph;
        private final int mRpm;
        private final long mTimestampMs;

        public VehicleData(float speedKph, int rpm, long timestampMs) {
            this.mSpeedKph = speedKph;
            this.mRpm = rpm;
            this.mTimestampMs = timestampMs;
        }

        /** Parcel read constructor — field order MUST match writeToParcel() exactly. */
        private VehicleData(Parcel in) {
            mSpeedKph = in.readFloat();    // Order: 1
            mRpm = in.readInt();           // Order: 2
            mTimestampMs = in.readLong();  // Order: 3
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeFloat(mSpeedKph);   // Order: 1
            dest.writeInt(mRpm);           // Order: 2
            dest.writeLong(mTimestampMs);  // Order: 3
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Parcelable.Creator<VehicleData> CREATOR =
                new Parcelable.Creator<VehicleData>() {
                    @Override
                    public VehicleData createFromParcel(Parcel in) {
                        return new VehicleData(in);
                    }

                    @Override
                    public VehicleData[] newArray(int size) {
                        return new VehicleData[size];
                    }
                };

        public float getSpeedKph() { return mSpeedKph; }
        public int getRpm() { return mRpm; }
        public long getTimestampMs() { return mTimestampMs; }

        @Override
        public String toString() {
            return "VehicleData{speed=" + mSpeedKph + "kph, rpm=" + mRpm + "}";
        }
    }

    // -----------------------------------------------------------------------------------------
    // 2. AIDL CALLBACK INTERFACE SIMULATION
    //    In production: declared in IVehicleEventCallback.aidl with oneway methods.
    //    oneway guarantees server never blocks waiting for client acknowledgment.
    // -----------------------------------------------------------------------------------------

    /**
     * Simulates the generated AIDL callback stub interface.
     * Production AIDL equivalent:
     * <pre>
     *   interface IVehicleEventCallback {
     *       oneway void onSpeedChanged(in VehicleData data);
     *       oneway void onServiceDisconnected(int reason);
     *   }
     * </pre>
     */
    public interface IVehicleEventCallback extends android.os.IInterface {
        void onSpeedChanged(VehicleData data) throws RemoteException;
        void onServiceDisconnected(int reason) throws RemoteException;
    }

    // -----------------------------------------------------------------------------------------
    // 3. BINDER DEATH RECIPIENT — Monitors client process death
    // -----------------------------------------------------------------------------------------

    private final class ClientDeathRecipient implements IBinder.DeathRecipient {
        private final IVehicleEventCallback mCallback;

        ClientDeathRecipient(IVehicleEventCallback callback) {
            this.mCallback = callback;
        }

        @Override
        public void binderDied() {
            Log.w(TAG, "Client process died unexpectedly. Cleaning up registration.");
            // Unlink and unregister on background thread to avoid blocking Binder thread
            mBackgroundExecutor.execute(() -> performClientCleanup(mCallback));
        }
    }

    // -----------------------------------------------------------------------------------------
    // 4. SERVICE IMPLEMENTATION
    // -----------------------------------------------------------------------------------------

    private final Context mContext;
    private final ExecutorService mBackgroundExecutor;

    /** Thread-safe multi-client callback list. Handles dead-client cleanup automatically. */
    private final RemoteCallbackList<IVehicleEventCallback> mCallbackList =
            new RemoteCallbackList<>();

    /** Cached latest vehicle data — returned instantly on Binder thread (< 5ms). */
    private final AtomicReference<VehicleData> mCachedSpeed =
            new AtomicReference<>(new VehicleData(0f, 0, 0L));

    private boolean mIsInitialized;

    public BinderServiceTemplate(Context context) {
        mContext = Objects.requireNonNull(context, "Context cannot be null")
                .getApplicationContext();
        mBackgroundExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Initializes background polling for hardware data.
     * Idempotent: safe to call multiple times.
     */
    public synchronized void init() {
        if (mIsInitialized) {
            Log.d(TAG, "Already initialized. Skipping duplicate init().");
            return;
        }
        mIsInitialized = true;
        mBackgroundExecutor.execute(this::startHardwarePollingLoop);
        Log.d(TAG, "BinderServiceTemplate initialized.");
    }

    // -----------------------------------------------------------------------------------------
    // 5. AIDL STUB METHOD IMPLEMENTATIONS
    // -----------------------------------------------------------------------------------------

    /**
     * AIDL stub: Returns current cached vehicle speed.
     * Security: enforceCallingOrSelfPermission() guards this entry point.
     * Binder Safety: Returns cached value instantly — zero blocking.
     */
    public VehicleData getCurrentSpeed() throws RemoteException {
        enforcePermission();
        // Return pre-fetched atomic cached value — Binder thread returns immediately
        return mCachedSpeed.get();
    }

    /**
     * AIDL stub: Registers a client callback with death monitoring.
     * linkToDeath() ensures cleanup if client process is killed.
     */
    public void registerCallback(IVehicleEventCallback callback) throws RemoteException {
        enforcePermission();
        Objects.requireNonNull(callback, "Callback cannot be null");

        IBinder binder = callback.asBinder();
        ClientDeathRecipient deathRecipient = new ClientDeathRecipient(callback);

        try {
            // MANDATORY: Register death listener before storing callback reference
            binder.linkToDeath(deathRecipient, /* flags= */ 0);
            mCallbackList.register(callback);
            Log.d(TAG, "Client callback registered. Total clients: " + mCallbackList.getRegisteredCallbackCount());
        } catch (RemoteException e) {
            // Client died between the IPC call and linkToDeath — discard safely
            Log.w(TAG, "Client already dead during registerCallback(). Discarding.", e);
        }
    }

    /**
     * AIDL stub: Unregisters client callback and removes death monitoring.
     * Declared oneway in AIDL: fire-and-forget, never blocks caller.
     */
    public void unregisterCallback(IVehicleEventCallback callback) {
        if (callback == null) return;
        callback.asBinder().unlinkToDeath(/* recipient — stored per-binder */ null, 0);
        mCallbackList.unregister(callback);
        Log.d(TAG, "Client callback unregistered.");
    }

    /**
     * AIDL stub: Fetches sensitive internal data using identity clearing.
     * Binder.clearCallingIdentity() ensures the self-call runs as the system process,
     * not as the calling client, preventing privilege escalation.
     */
    public String getSensitiveSystemData() throws RemoteException {
        enforcePermission();

        // Clear caller identity so internal repo call runs with system UID, not caller UID
        long identityToken = Binder.clearCallingIdentity();
        try {
            return fetchSensitiveDataInternal();
        } finally {
            // MANDATORY: Always restore in finally block
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    // -----------------------------------------------------------------------------------------
    // 6. PRIVATE HELPERS
    // -----------------------------------------------------------------------------------------

    /**
     * Enforces the required permission at the Binder transaction boundary.
     * MUST be called at the top of every sensitive AIDL stub method.
     */
    private void enforcePermission() {
        mContext.enforceCallingOrSelfPermission(REQUIRED_PERMISSION,
                "Caller lacks required permission: " + REQUIRED_PERMISSION);
    }

    /**
     * Background loop: Polls hardware and updates cached speed data.
     * Runs entirely on mBackgroundExecutor — never on Binder thread pool.
     */
    private void startHardwarePollingLoop() {
        Log.d(TAG, "Hardware polling loop started.");
        while (mIsInitialized && !Thread.currentThread().isInterrupted()) {
            VehicleData freshData = pollHardwareData();
            mCachedSpeed.set(freshData);
            broadcastSpeedUpdate(freshData);
            sleepSafely(100L); // Poll at 10Hz
        }
    }

    /**
     * Broadcasts speed update to all registered clients via RemoteCallbackList.
     * beginBroadcast() / finishBroadcast() form an atomic broadcast block.
     * Dead clients are automatically removed by RemoteCallbackList.
     */
    private void broadcastSpeedUpdate(VehicleData data) {
        int count = mCallbackList.beginBroadcast();
        for (int i = 0; i < count; i++) {
            try {
                mCallbackList.getBroadcastItem(i).onSpeedChanged(data);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to dispatch speed update to client #" + i, e);
            }
        }
        mCallbackList.finishBroadcast();
    }

    /** Cleans up a dead client's callback registration. */
    private void performClientCleanup(IVehicleEventCallback deadCallback) {
        mCallbackList.unregister(deadCallback);
        Log.d(TAG, "Dead client cleaned up.");
    }

    /** Simulates hardware polling — replace with actual VHAL/HAL API call. */
    private VehicleData pollHardwareData() {
        float speed = 60.0f + (float) (Math.random() * 10);
        int rpm = 2000 + (int) (Math.random() * 500);
        return new VehicleData(speed, rpm, System.currentTimeMillis());
    }

    /** Simulates an internal data fetch requiring system identity. */
    private String fetchSensitiveDataInternal() {
        return "SYSTEM_VIN_DATA_12345";
    }

    /** Sleeps safely — interrupts thread if interrupted. */
    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Tears down the service: kills polling loop, kills callback list, shuts down executor.
     * Idempotent: safe to call multiple times.
     */
    public synchronized void release() {
        if (!mIsInitialized) return;

        mIsInitialized = false;
        mCallbackList.kill(); // Disables all future broadcasts and releases all callbacks
        mBackgroundExecutor.shutdownNow();
        Log.d(TAG, "BinderServiceTemplate released.");
    }
}
