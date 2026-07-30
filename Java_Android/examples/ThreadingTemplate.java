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
 * 1. Background execution using ExecutorService (off UI thread).
 * 2. Safe result dispatching back to Main Thread using Handler(Looper.getMainLooper()).
 * 3. Lifecycle cleanup in release() to prevent Memory Leaks and ANR crashes.
 * 4. Refactoring-safe TAG naming convention.
 */
public class ThreadingTemplate<T> {
    private static final String TAG = ThreadingTemplate.class.getSimpleName();

    private ExecutorService mExecutor;
    private Handler mMainHandler;

    /**
     * Callback interface for receiving asynchronous execution results on the Main Thread.
     */
    public interface TaskCallback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    /**
     * Initializes background thread pool and main thread looper handler.
     */
    public void init() {
        if (mExecutor == null || mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }
        if (mMainHandler == null) {
            mMainHandler = new Handler(Looper.getMainLooper());
        }
    }

    /**
     * Executes a background task asynchronously and posts the result to the main thread callback.
     *
     * @param task Callable background operation
     * @param callback Callback triggered on the Main UI Thread
     */
    public void executeAsync(Callable<T> task, TaskCallback<T> callback) {
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(callback, "Callback cannot be null");

        if (mExecutor == null || mExecutor.isShutdown()) {
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
     * Must be called during component teardown (e.g., onDestroy).
     */
    public void release() {
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }

        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }
    }
}
