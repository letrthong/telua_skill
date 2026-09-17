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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Gold-Standard Benchmark: Safe Handler Memory Leak Prevention Template.
 *
 * <p>Demonstrates full compliance with:
 * <ul>
 *   <li><b>rules/handler_rule.md</b>:
 *     <ul>
 *       <li>Rule 1.1: Guarded local assignment and null-safety on chained getters.</li>
 *       <li>Rule 1.2: Mandatory {@code removeCallbacksAndMessages(null)} in teardown.</li>
 *     </ul>
 *   </li>
 *   <li><b>rules/lifecycle_init_rule.md</b>: Idempotent {@code init()} / {@code release()}.</li>
 *   <li><b>rules/magic_number_immutability_rule.md</b>: Elimination of magic numbers into UPPER_SNAKE_CASE.</li>
 *   <li><b>rules/naming_rule.md</b>: Android field prefixes ({@code m}/{@code s}) and JLS modifier ordering.</li>
 * </ul>
 *
 * <h3>The Problem (Classic Android Memory Leak):</h3>
 * Non-static inner classes (including anonymous {@code Handler} or {@code Runnable} instances)
 * retain an implicit strong reference to their enclosing class (e.g. {@code Activity} or {@code Service}).
 * If a message is posted with delay (e.g. 10 seconds), the MessageQueue holds this message until it fires.
 * If the user finishes or destroys the Activity before the timer fires, the Activity CANNOT be garbage-collected,
 * causing a severe memory leak, window leak, or NPE crash when the handler tries to update destroyed views.
 *
 * <h3>The Solution:</h3>
 * <ol>
 *   <li>Declare the Handler as a <b>static nested class</b> (breaks the implicit outer reference).</li>
 *   <li>Hold a <b>WeakReference</b> to the target callback or host component.</li>
 *   <li>Always clear pending messages via {@code removeCallbacksAndMessages(null)} during lifecycle teardown.</li>
 * </ol>
 */
public class SafeHandlerLeakPreventionTemplate {
    private static final String TAG = SafeHandlerLeakPreventionTemplate.class.getSimpleName();

    // Extracted named constants eliminating magic numbers (magic_number_immutability_rule.md)
    private static final int MSG_HEARTBEAT = 1001;
    private static final int MSG_TASK_COMPLETED = 1002;
    private static final long DEFAULT_DELAY_MILLIS = 3_000L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 5_000L;

    /**
     * Interface defining host actions invoked by the SafeHandler.
     */
    public interface HostCallback {
        void onHeartbeatReceived(int tickCount);
        void onDelayedTaskCompleted(@NonNull String resultPayload);
    }

    private final SafeIncomingHandler mHandler;
    private final HostCallback mCallback;
    private boolean mIsActive;

    /**
     * Constructs the controller with layered defense null-safety.
     *
     * @param callback host action receiver, must not be null.
     */
    public SafeHandlerLeakPreventionTemplate(@NonNull final HostCallback callback) {
        mCallback = Objects.requireNonNull(callback, "HostCallback must not be null");
        // Instantiate static handler attached to Main Looper
        mHandler = new SafeIncomingHandler(Looper.getMainLooper(), mCallback);
        mIsActive = true;
    }

    /**
     * Posts a delayed message safely.
     *
     * @param payload task result text to transmit upon delay completion.
     */
    public synchronized void scheduleDelayedTask(@NonNull final String payload) {
        if (!mIsActive) {
            Log.w(TAG, "Cannot schedule delayed task: component is already released.");
            return;
        }
        Objects.requireNonNull(payload, "Payload must not be null");

        final Message msg = mHandler.obtainMessage(MSG_TASK_COMPLETED, payload);
        mHandler.sendMessageDelayed(msg, DEFAULT_DELAY_MILLIS);
    }

    /**
     * Starts periodic heartbeat signaling.
     */
    public synchronized void startHeartbeat() {
        if (!mIsActive) {
            Log.w(TAG, "Cannot start heartbeat: component is already released.");
            return;
        }
        // Remove duplicate pending heartbeats before scheduling a new tick
        mHandler.removeMessages(MSG_HEARTBEAT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_HEARTBEAT, 1, 0), HEARTBEAT_INTERVAL_MILLIS);
    }

    /**
     * Mandatory lifecycle teardown method (handler_rule.md Rule 1.2).
     * Must be called in Activity.onDestroy(), Fragment.onDestroyView(), or Service.onDestroy().
     * Idempotent: safe to call multiple times without side-effects.
     */
    public synchronized void release() {
        if (!mIsActive) {
            return;
        }
        mIsActive = false;

        // Rule 1.2: Remove all pending callbacks and messages to eliminate memory leak
        mHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "SafeHandler pending callbacks cleared and lifecycle released.");
    }

    /**
     * Static nested Handler subclass.
     *
     * <p>Being static, it does NOT retain an implicit reference to {@link SafeHandlerLeakPreventionTemplate}.
     * It accesses the callback exclusively through a {@link WeakReference}.
     */
    private static final class SafeIncomingHandler extends Handler {
        private final WeakReference<HostCallback> mWeakCallback;

        public SafeIncomingHandler(@NonNull final Looper looper, @NonNull final HostCallback callback) {
            super(Objects.requireNonNull(looper, "Looper must not be null"));
            mWeakCallback = new WeakReference<>(Objects.requireNonNull(callback, "Callback must not be null"));
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
            final HostCallback callback = mWeakCallback.get();

            // Null-check: if the host Activity/Service was destroyed and garbage-collected,
            // callback will be null. Discard message safely without crash or memory leak.
            if (callback == null) {
                Log.w(TAG, "HostCallback is already garbage-collected. Message dropped: " + msg.what);
                return;
            }

            switch (msg.what) {
                case MSG_HEARTBEAT:
                    final int currentTick = msg.arg1;
                    callback.onHeartbeatReceived(currentTick);

                    // Reschedule next heartbeat tick if host is still alive
                    final Message nextMsg = obtainMessage(MSG_HEARTBEAT, currentTick + 1, 0);
                    sendMessageDelayed(nextMsg, HEARTBEAT_INTERVAL_MILLIS);
                    break;

                case MSG_TASK_COMPLETED:
                    if (msg.obj instanceof String payload) {
                        callback.onDelayedTaskCompleted(payload);
                    }
                    break;

                default:
                    Log.w(TAG, "Unhandled message code: " + msg.what);
                    break;
            }
        }
    }
}
