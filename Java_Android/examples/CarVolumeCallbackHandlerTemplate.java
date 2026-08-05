/*
 * Copyright (C) 2026 letrthong@gmail.com
 * Created & Maintained by: letrthong@gmail.com
 * Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
 * Adapted from AOSP CarVolumeCallbackHandler (Apache 2.0 License)
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
package com.example.android.template;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced Multi-Client Callback Manager Component Pattern (Subclassed RemoteCallbackList).
 *
 * <p>AOSP Reference Source:
 * <ul>
 *   <li>Source URL: <a href="https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/services/Car/service/src/com/android/car/audio/CarVolumeCallbackHandler.java">AOSP CarVolumeCallbackHandler.java</a></li>
 *   <li>Integration Registry Note: {@code docs/car_volume_callback_handler_integration.md}</li>
 * </ul>
 *
 * <p>Architectural Pattern Explanation (Mẫu thiết kế Component quản lý Callback đa Client nâng cao):
 * <ul>
 *   <li><b>RemoteCallbackList Subclassing:</b> Directly extends {@link RemoteCallbackList} to encapsulate
 *       multi-client AIDL callback registration, unregistration, and broadcasting lifecycle logic.</li>
 *   <li><b>Custom Metadata Cookie:</b> Attaches an immutable {@link ClientMetadataCookie} (containing client UID,
 *       priority level, and registration flags) to each registered AIDL interface via {@link #register(IInterface, Object)}.</li>
 *   <li><b>Secondary UID Mapping:</b> Maintains a thread-safe {@link SparseArray} mapping UIDs to list of client {@link IBinder}
 *       instances for rapid UID-level priority updates and batch operations.</li>
 *   <li><b>Background HandlerThread Offloading:</b> Dispatches callback iterations onto a dedicated background
 *       {@link HandlerThread} to ensure Binder call execution stays within &lt; 5ms and prevents server-side ANRs.</li>
 *   <li><b>Automatic Binder Death Teardown:</b> Overrides {@link #onCallbackDied(IInterface, Object)} to clean up
 *       obsolete client UID maps whenever a remote client process unexpectedly dies.</li>
 * </ul>
 *
 * @param <T> The AIDL callback interface extending {@link IInterface}
 */
public class CarVolumeCallbackHandlerTemplate<T extends IInterface> extends RemoteCallbackList<T> {
    private static final String TAG = CarVolumeCallbackHandlerTemplate.class.getSimpleName();
    private static final String THREAD_NAME = "CarVolumeCallbackThread";

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final Object mLock = new Object();

    // Secondary map tracking UIDs to list of registered IBinders for client process management
    private final SparseArray<List<IBinder>> mUidToBindersMap = new SparseArray<>();

    /**
     * Listener dispatcher functional interface for background thread execution.
     *
     * @param <T> The AIDL interface type
     */
    @FunctionalInterface
    public interface CallbackDispatcher<T> {
        void dispatch(T callback, ClientMetadataCookie cookie) throws RemoteException;
    }

    public CarVolumeCallbackHandlerTemplate() {
        mHandlerThread = new HandlerThread(THREAD_NAME);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    /**
     * Registers a remote AIDL client callback with UID metadata and priority status.
     *
     * @param callback The AIDL interface stub
     * @param binder   The raw IBinder reference from client
     * @param uid      The calling client UID
     * @param priority True if client requires high-priority event dispatching
     */
    public void registerClientCallback(T callback, IBinder binder, int uid, boolean priority) {
        if (callback == null || binder == null) {
            return;
        }
        synchronized (mLock) {
            register(callback, new ClientMetadataCookie(uid, priority));
            List<IBinder> binders = mUidToBindersMap.get(uid);
            if (binders == null) {
                binders = new ArrayList<>();
                mUidToBindersMap.put(uid, binders);
            }
            if (!binders.contains(binder)) {
                binders.add(binder);
            }
        }
    }

    /**
     * Unregisters a remote client callback.
     *
     * @param callback The AIDL interface stub
     * @param binder   The raw IBinder reference
     * @param uid      The calling client UID
     */
    public void unregisterClientCallback(T callback, IBinder binder, int uid) {
        if (callback == null || binder == null) {
            return;
        }
        synchronized (mLock) {
            unregister(callback);
            List<IBinder> binders = mUidToBindersMap.get(uid);
            if (binders != null) {
                binders.remove(binder);
                if (binders.isEmpty()) {
                    mUidToBindersMap.remove(uid);
                }
            }
        }
    }

    /**
     * Dispatches an event to all registered listeners asynchronously on the Handler thread.
     *
     * @param dispatcher Functional interface performing the AIDL remote method call
     */
    public void dispatchAsync(CallbackDispatcher<T> dispatcher) {
        if (dispatcher == null) {
            return;
        }
        mHandler.post(() -> {
            int count = beginBroadcast();
            try {
                for (int i = 0; i < count; i++) {
                    ClientMetadataCookie cookie = (ClientMetadataCookie) getBroadcastCookie(i);
                    T callback = getBroadcastItem(i);
                    if (callback != null && cookie != null) {
                        try {
                            dispatcher.dispatch(callback, cookie);
                        } catch (RemoteException e) {
                            AppLogger.e(TAG, "Failed to dispatch callback to client #" + i, e);
                        }
                    }
                }
            } finally {
                finishBroadcast(); // MANDATORY: Always finish broadcast inside finally block
            }
        });
    }

    /**
     * Invoked automatically by RemoteCallbackList when a client process dies.
     *
     * @param callback The callback whose process died
     * @param cookie   The attached cookie containing caller metadata
     */
    @Override
    public void onCallbackDied(T callback, Object cookie) {
        if (cookie instanceof ClientMetadataCookie) {
            ClientMetadataCookie clientCookie = (ClientMetadataCookie) cookie;
            synchronized (mLock) {
                mUidToBindersMap.remove(clientCookie.mUid);
                AppLogger.w(TAG, "Client process died. Removed UID: " + clientCookie.mUid);
            }
        }
    }

    /**
     * Idempotent lifecycle teardown method releasing handler threads and unregistering all callbacks.
     */
    public void release() {
        kill(); // Safely unregisters all remote callbacks in RemoteCallbackList
        mHandlerThread.quitSafely();
    }

    /**
     * Immutable cookie container storing client metadata.
     */
    public static final class ClientMetadataCookie {
        public final int mUid;
        public final boolean mPriority;

        public ClientMetadataCookie(int uid, boolean priority) {
            this.mUid = uid;
            this.mPriority = priority;
        }

        public int getUid() {
            return mUid;
        }

        public boolean isPriority() {
            return mPriority;
        }
    }
}
