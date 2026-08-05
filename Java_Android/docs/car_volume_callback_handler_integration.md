# AOSP Integration Note: CarVolumeCallbackHandler (RemoteCallbackList with Custom Cookie)

This document logs the AOSP `CarVolumeCallbackHandler` pattern for managing AIDL multi-client callbacks with priority filtering, metadata cookies, and background thread broadcasting.

---

## 1. Source Reference & Reference Details
* **AOSP Source URL:** `https://cs.android.com/android/platform/superproject/+/android-latest-release:packages/services/Car/service/src/com/android/car/audio/CarVolumeCallbackHandler.java`
* **Package Location:** `com.android.car.audio`
* **Adopted Architectural Pattern:** Subclassing `RemoteCallbackList<E>`, attaching custom Cookie metadata (`CallerPriorityCookie`), tracking UID-to-Binder mappings via `SparseArray`, background thread dispatching (`HandlerThread`), and overriding `onCallbackDied()` for automatic dead-process cleanup.

---

## 2. Required Java Imports
```java
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.SparseArray;

import com.android.car.media.ICarVolumeCallback; // Target AIDL interface
import java.util.ArrayList;
import java.util.List;
```

---

## 3. Build System Dependencies
* **Gradle (`app/build.gradle`):**
  ```groovy
  dependencies {
      // Android Automotive Car Service framework libraries
      compileOnly files("${android.getSdkDirectory()}/optional/optional.json")
  }
  ```
* **AOSP Build (`Android.bp`):**
  ```blueprint
  java_service_library {
      name: "car-service-audio",
      srcs: ["src/**/*.java"],
      static_libs: [
          "android.car.builtin",
      ],
  }
  ```

---

## 4. Refactored Canonical Usage Pattern
```java
/**
 * Refactored AOSP CarVolumeCallbackHandler benchmark template conforming to workspace rules.
 */
public final class CarVolumeCallbackHandlerTemplate extends RemoteCallbackList<ICarVolumeCallback> {
    private static final String TAG = CarVolumeCallbackHandlerTemplate.class.getSimpleName();
    private static final String THREAD_NAME = "CarVolumeCallbackThread";

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;
    private final Object mLock = new Object();

    // Mapping UID to list of registered IBinders for rapid lookup and priority re-registration
    private final SparseArray<List<IBinder>> mUidToBindersMap = new SparseArray<>();

    public CarVolumeCallbackHandlerTemplate() {
        mHandlerThread = new HandlerThread(THREAD_NAME);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public void registerCallback(IBinder binder, int uid, boolean priority) {
        if (binder == null) {
            return;
        }
        ICarVolumeCallback callback = ICarVolumeCallback.Stub.asInterface(binder);
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

    public void dispatchVolumeChange(int zoneId, int groupId, int flags) {
        mHandler.post(() -> {
            int count = beginBroadcast();
            try {
                for (int i = 0; i < count; i++) {
                    ClientMetadataCookie cookie = (ClientMetadataCookie) getBroadcastCookie(i);
                    if (cookie != null && !cookie.mPriority) {
                        continue; // Filter out low-priority listeners
                    }
                    ICarVolumeCallback callback = getBroadcastItem(i);
                    if (callback != null) {
                        try {
                            callback.onGroupVolumeChanged(zoneId, groupId, flags);
                        } catch (RemoteException e) {
                            AppLogger.e(TAG, "Failed to dispatch volume change to client #" + i, e);
                        }
                    }
                }
            } finally {
                finishBroadcast(); // Always finish broadcast inside finally block
            }
        });
    }

    @Override
    public void onCallbackDied(ICarVolumeCallback callback, Object cookie) {
        if (cookie instanceof ClientMetadataCookie) {
            ClientMetadataCookie clientCookie = (ClientMetadataCookie) cookie;
            synchronized (mLock) {
                mUidToBindersMap.remove(clientCookie.mUid);
                AppLogger.w(TAG, "Cleaned up dead client UID: " + clientCookie.mUid);
            }
        }
    }

    public void release() {
        kill(); // Unregister all callbacks in RemoteCallbackList
        mHandlerThread.quitSafely();
    }

    private static final class ClientMetadataCookie {
        public final int mUid;
        public final boolean mPriority;

        ClientMetadataCookie(int uid, boolean priority) {
            this.mUid = uid;
            this.mPriority = priority;
        }
    }
}
```

---

## 5. Known Risks, Defensive Guardrails & Best Practices
* ⚠️ **Deadlock Risk:** Never hold `mLock` while calling `beginBroadcast()` / `finishBroadcast()`. Dispatching must be done on background `HandlerThread`.
* ⚠️ **Thread Leaks:** Always call `release()` / `mHandlerThread.quitSafely()` during service teardown.
* ⚠️ **`beginBroadcast()` Guard:** `finishBroadcast()` MUST be called inside a `try-finally` block to prevent deadlock on subsequent broadcasts if an exception occurs.
* ⚠️ **Binder Death Cleanup:** Overriding `onCallbackDied()` is mandatory when maintaining custom secondary maps (`mUidToBindersMap`) alongside `RemoteCallbackList`.
