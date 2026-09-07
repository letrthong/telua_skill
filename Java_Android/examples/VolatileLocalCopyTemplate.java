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

import android.util.Log;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard Volatile + Local Copy (Immutable Snapshot) Template.
 *
 * Demonstrates the idiomatic multithreading pattern for read-heavy state:
 * 1. {@code volatile} reference to an IMMUTABLE object guarantees Visibility
 *    (a writer thread's update is immediately seen by all reader threads).
 * 2. Copying the volatile reference into a LOCAL variable once at the top of a
 *    method guarantees Consistency (a stable snapshot for the whole method scope),
 *    avoiding "Time-of-check to time-of-use" (TOCTOU) bugs.
 * 3. Local copy also avoids repeated main-memory reads of the volatile field,
 *    improving performance in read-heavy code paths.
 * 4. Idempotent lifecycle: safe against duplicate init() / release() calls.
 *
 * @param <T> the immutable state type managed by this holder
 */
public class VolatileLocalCopyTemplate<T> {
    private static final String TAG = VolatileLocalCopyTemplate.class.getSimpleName();

    // Volatile reference to an IMMUTABLE object -> Visibility across threads.
    private volatile T mState;

    // Atomic flag for lifecycle state (thread-safe, no volatile boolean needed).
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);

    /**
     * Initializes the holder with an initial immutable state.
     * Idempotent: safe to call multiple times or after release().
     *
     * @param initialState the initial immutable state; must not be {@code null}
     */
    public synchronized void init(T initialState) {
        Objects.requireNonNull(initialState, "initialState must not be null");
        if (mIsInitialized.get()) {
            Log.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }
        // Atomic publish: readers see either the old or the new state, never a torn one.
        mState = initialState;
        mIsInitialized.set(true);
        Log.d(TAG, "VolatileLocalCopyTemplate initialized.");
    }

    /**
     * Atomically publishes a NEW immutable state.
     * The object MUST be immutable; never mutate the object already referenced by mState.
     *
     * @param newState the new immutable state; must not be {@code null}
     */
    public void updateState(T newState) {
        Objects.requireNonNull(newState, "newState must not be null");
        // Single volatile write -> atomic publish to all reader threads.
        mState = newState;
    }

    /**
     * Read-heavy method demonstrating the IDIOMATIC volatile + local copy pattern.
     *
     * <p>We copy the volatile reference into a local variable ONCE, then use the
     * local variable for the entire method. This guarantees that all fields read
     * belong to the SAME snapshot, even if another thread updates mState mid-method.
     *
     * @return a short description built from the stable local snapshot
     */
    public String describeState() {
        // --- IDIOMATIC PATTERN ---
        // 1. Copy volatile reference to a thread-local variable (single read).
        T localState = mState;

        // 2. Use the local variable for ALL subsequent logic (stable snapshot).
        if (localState == null) {
            return "State not set yet";
        }
        return "Current state: " + localState.toString();
    }

    /**
     * Example of a read-then-act method that MUST use the local snapshot to avoid
     * TOCTOU bugs: the check and the use operate on the same consistent snapshot.
     *
     * @param expected the value to compare against the current state
     * @return {@code true} if the current state equals {@code expected}
     */
    public boolean isStateEqualTo(T expected) {
        // Single local snapshot -> check and use are consistent.
        T localState = mState;
        return localState != null && localState.equals(expected);
    }

    /**
     * Cleans up the holder state. Idempotent: safe to call multiple times.
     */
    public synchronized void release() {
        if (!mIsInitialized.get()) {
            Log.d(TAG, "Already released or not initialized. Skipping duplicate release() call.");
            return;
        }
        mState = null;
        mIsInitialized.set(false);
        Log.d(TAG, "VolatileLocalCopyTemplate released.");
    }

    /**
     * @return {@code true} if the holder has been initialized and not yet released
     */
    public boolean isInitialized() {
        return mIsInitialized.get();
    }
}
