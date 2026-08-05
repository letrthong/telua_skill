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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Standard Third-Party Library Latency & Timeout Resilience Template.
 *
 * Key Principles Demonstrated:
 * 1. Offloading potentially blocking (3-5 seconds) third-party SDK calls off the Main UI Thread.
 * 2. Enforcing explicit timeout boundaries using Future.get(timeout, TimeUnit.SECONDS).
 * 3. Graceful handling of TimeoutException to prevent UI freezes and ANR crashes.
 * 4. Idempotent lifecycle management (init / release) with thread pool cleanup.
 */
public class ApiTimeoutResilienceTemplate<T> {
    private static final String TAG = ApiTimeoutResilienceTemplate.class.getSimpleName();
    private static final long DEFAULT_TIMEOUT_SECONDS = 5;

    private ExecutorService mExecutor;
    private boolean mIsInitialized;

    /**
     * Mock third-party library interface with potential blocking latency.
     */
    public interface ThirdPartySdkApi<T> {
        T fetchRemoteData() throws Exception;
    }

    /**
     * Initializes the background executor service.
     * Idempotent method: Safe to call multiple times or after release().
     */
    public synchronized void init() {
        if (mIsInitialized) {
            Log.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        if (mExecutor == null || mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }

        mIsInitialized = true;
        Log.d(TAG, "ApiTimeoutResilienceTemplate successfully initialized.");
    }

    /**
     * Executes a blocking third-party SDK call off the main thread with a 5-second timeout boundary.
     *
     * @param sdkCall Blocking call to third-party SDK interface
     * @return Optional containing the result, or Optional.empty() if timed out or failed
     */
    public synchronized Optional<T> executeWithTimeout(ThirdPartySdkApi<T> sdkCall) {
        return executeWithTimeout(sdkCall, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Executes a blocking third-party SDK call off the main thread with a custom timeout boundary.
     *
     * @param sdkCall Blocking call to third-party SDK interface
     * @param timeout Maximum duration to wait before timing out
     * @param unit Time unit of the timeout duration
     * @return Optional containing the result, or Optional.empty() if timed out or failed
     */
    public synchronized Optional<T> executeWithTimeout(ThirdPartySdkApi<T> sdkCall, long timeout, TimeUnit unit) {
        Objects.requireNonNull(sdkCall, "Third-party SDK call cannot be null");
        Objects.requireNonNull(unit, "TimeUnit cannot be null");

        if (!mIsInitialized) {
            init();
        }

        Callable<T> task = sdkCall::fetchRemoteData;
        Future<T> future = mExecutor.submit(task);

        try {
            // Block background thread for at most specified timeout duration (e.g., 5 seconds)
            T result = future.get(timeout, unit);
            return Optional.ofNullable(result);
        } catch (TimeoutException e) {
            Log.w(TAG, String.format("Third-party SDK call timed out after %d %s. Interrupting thread.", timeout, unit.name().toLowerCase()), e);
            future.cancel(true); // Interrupt background execution
            return Optional.empty(); // Graceful fallback
        } catch (Exception e) {
            Log.e(TAG, "Error occurred during third-party SDK execution", e);
            return Optional.empty(); // Graceful fallback
        }
    }

    /**
     * Cleans up background thread pool resources.
     * Idempotent method: Safe to call multiple times.
     */
    public synchronized void release() {
        if (!mIsInitialized) {
            Log.d(TAG, "Already released or not initialized. Skipping duplicate release() call.");
            return;
        }

        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }

        mIsInitialized = false;
        Log.d(TAG, "ApiTimeoutResilienceTemplate successfully released.");
    }
}
