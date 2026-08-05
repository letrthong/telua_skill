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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Standard Threading & Asynchronous Task Manager Template.
 * 
 * Key Principles Demonstrated:
 * 1. Idempotent init(): Safe against duplicate init() calls and allows clean re-initialization after release().
 * 2. Background execution using ExecutorService (off UI thread).
 * 3. Safe result dispatching back to Main Thread using Handler(Looper.getMainLooper()).
 * 4. Lifecycle cleanup in release() to prevent Memory Leaks and ANR crashes.
 * 5. Refactoring-safe TAG naming convention and Android Log standards.
 */
public class ThreadingTemplate<T> {
    private static final String TAG = ThreadingTemplate.class.getSimpleName();

    private ExecutorService mExecutor;
    private Handler mMainHandler;
    private boolean mIsInitialized;

    /**
     * Callback interface for receiving asynchronous execution results on the Main Thread.
     */
    public interface TaskCallback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    /**
     * Initializes background thread pool and main thread looper handler.
     * Idempotent method: Safe to call multiple times or after release().
     */
    public synchronized void init() {
        if (mIsInitialized) {
            // Safe guard against duplicate init calls
            Log.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        if (mExecutor == null || mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }
        if (mMainHandler == null) {
            mMainHandler = new Handler(Looper.getMainLooper());
        }

        mIsInitialized = true;
        Log.d(TAG, "ThreadingTemplate successfully initialized.");
    }

    /**
     * Executes a background task asynchronously and posts the result to the main thread callback.
     *
     * @param task Callable background operation
     * @param callback Callback triggered on the Main UI Thread
     */
    public synchronized void executeAsync(Callable<T> task, TaskCallback<T> callback) {
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(callback, "Callback cannot be null");

        // Automatically re-initialize if called after release()
        if (!mIsInitialized) {
            init();
        }

        mExecutor.execute(() -> {
            try {
                T result = task.call();
                // Safely post successful result to Main Thread
                if (mMainHandler != null) {
                    mMainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error executing background task", e);
                // Safely post exception to Main Thread
                if (mMainHandler != null) {
                    mMainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Cleans up all pending callbacks and shuts down background thread pools.
     * Idempotent method: Safe to call multiple times. Reset state allows future re-initialization.
     */
    public synchronized void release() {
        if (!mIsInitialized) {
            Log.d(TAG, "Already released or not initialized. Skipping duplicate release() call.");
            return;
        }

        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }

        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }

        mIsInitialized = false;
        Log.d(TAG, "ThreadingTemplate successfully released.");
    }

    public synchronized boolean isInitialized() {
        return mIsInitialized;
    }
}
