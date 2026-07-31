package com.example.app.examples;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard Production-Grade Producer-Consumer Thread Pattern Template.
 *
 * Key Principles Demonstrated:
 * 1. Backpressure Control: Bounded Blocking Queue (capacity: 100) prevents OutOfMemory (OOM) errors
 *    and thread hangs when request volume bursts suddenly.
 * 2. Non-Blocking Producer: Uses queue.offer(task, 1000ms) to ensure producers never hang indefinitely.
 * 3. Dedicated Background Consumer: Runs continuous event loop polling tasks off the queue.
 * 4. Safe UI Thread Dispatch: Posts processing results back to Main UI Thread using Handler(Looper.getMainLooper()).
 * 5. Idempotent Teardown: Safely shuts down worker threads and drains remaining queue items during release().
 */
public class ProducerConsumerTemplate {
    private static final String TAG = ProducerConsumerTemplate.class.getSimpleName();
    
    // Extracted named constants eliminating magic numbers
    private static final int MAX_QUEUE_CAPACITY = 100;
    private static final long QUEUE_OFFER_TIMEOUT_MS = 1_000L;
    private static final long POLL_TIMEOUT_MS = 2_000L;

    private BlockingQueue<RequestTask> mTaskQueue;
    private ExecutorService mConsumerExecutor;
    private Handler mMainHandler;

    private final AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private boolean mIsInitialized;

    /**
     * 1. Data DTO: Immutable data container for queued request tasks.
     */
    public record RequestTask(String requestId, String payload, long timestamp) {
        public RequestTask {
            Objects.requireNonNull(requestId, "RequestId cannot be null");
            Objects.requireNonNull(payload, "Payload cannot be null");
        }
    }

    /**
     * Listener callback for receiving processed results on the Main UI Thread.
     */
    public interface OnTaskProcessedListener {
        void onTaskSuccess(RequestTask task, String processedResult);
        void onTaskDropped(RequestTask task, String reason);
        void onError(Throwable throwable);
    }

    /**
     * Initializes thread pools, bounded queue, and main handler.
     * Idempotent method: Safe to call multiple times or after release().
     */
    public synchronized void init(OnTaskProcessedListener listener) {
        Objects.requireNonNull(listener, "OnTaskProcessedListener cannot be null");

        if (mIsInitialized) {
            Log.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        if (mTaskQueue == null) {
            mTaskQueue = new ArrayBlockingQueue<>(MAX_QUEUE_CAPACITY);
        }
        if (mConsumerExecutor == null || mConsumerExecutor.isShutdown()) {
            mConsumerExecutor = Executors.newSingleThreadExecutor();
        }
        if (mMainHandler == null) {
            mMainHandler = new Handler(Looper.getMainLooper());
        }

        mIsRunning.set(true);
        mIsInitialized = true;
        Log.d(TAG, "ProducerConsumerTemplate initialized with max queue capacity: " + MAX_QUEUE_CAPACITY);

        // Start background consumer worker loop
        mConsumerExecutor.execute(() -> runConsumerLoop(listener));
    }

    /**
     * Producer Method: Submits a new request task into the bounded queue safely.
     * Uses offer with timeout to handle backpressure and prevent producer thread hangs.
     *
     * @param task RequestTask to be queued for processing
     * @param listener Callback listener for dropped task notifications
     */
    public void produceTask(RequestTask task, OnTaskProcessedListener listener) {
        Objects.requireNonNull(task, "RequestTask cannot be null");
        Objects.requireNonNull(listener, "OnTaskProcessedListener cannot be null");

        if (!mIsRunning.get()) {
            Log.w(TAG, "Cannot produce task. System is stopped or not initialized.");
            postToMainThread(() -> listener.onTaskDropped(task, "System not running"));
            return;
        }

        try {
            // Attempt to offer task to bounded queue within 1000ms timeout
            boolean added = mTaskQueue.offer(task, QUEUE_OFFER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!added) {
                Log.w(TAG, "Queue full! Dropping request task ID: " + task.requestId());
                postToMainThread(() -> listener.onTaskDropped(task, "Queue capacity full (Backpressure)"));
            } else {
                Log.d(TAG, "Task successfully queued. ID: " + task.requestId() + " | Current Queue Size: " + mTaskQueue.size());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Interrupted while offering task to queue", e);
            postToMainThread(() -> listener.onTaskDropped(task, "Producer thread interrupted"));
        }
    }

    /**
     * Consumer Loop: Continuously polls tasks from the queue and processes them in background.
     */
    private void runConsumerLoop(OnTaskProcessedListener listener) {
        Log.d(TAG, "[ConsumerThread] Background consumer worker thread started.");

        while (mIsRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Poll task from queue with 2000ms timeout boundary
                RequestTask task = mTaskQueue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (task != null) {
                    processSingleTask(task, listener);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.d(TAG, "[ConsumerThread] Consumer loop interrupted. Exiting.");
                break;
            } catch (Exception e) {
                Log.e(TAG, "[ConsumerThread] Error processing task", e);
                postToMainThread(() -> listener.onError(e));
            }
        }

        Log.d(TAG, "[ConsumerThread] Background consumer worker thread terminated.");
    }

    /**
     * Helper method to process a single task and post result back to UI Thread.
     */
    private void processSingleTask(RequestTask task, OnTaskProcessedListener listener) {
        Log.d(TAG, "[ConsumerThread] Processing task ID: " + task.requestId());

        // Simulate CPU computation / VHAL processing / network I/O
        String resultPayload = "PROCESSED_RESULT_FOR_" + task.payload();

        // Dispatch processed result back to Main UI Thread
        postToMainThread(() -> listener.onTaskSuccess(task, resultPayload));
    }

    /**
     * Safely posts a Runnable operation to the Main UI Thread.
     */
    private void postToMainThread(Runnable runnable) {
        synchronized (this) {
            if (mMainHandler != null) {
                mMainHandler.post(runnable);
            }
        }
    }

    /**
     * Shuts down consumer thread pools, clears pending queue items, and cleans up handlers.
     * Idempotent method: Safe to call multiple times. Reset state allows future re-initialization.
     */
    public synchronized void release() {
        if (!mIsInitialized) {
            Log.d(TAG, "Already released or not initialized. Skipping duplicate release() call.");
            return;
        }

        mIsRunning.set(false);

        if (mTaskQueue != null) {
            mTaskQueue.clear();
            mTaskQueue = null;
        }

        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }

        if (mConsumerExecutor != null) {
            mConsumerExecutor.shutdownNow();
            mConsumerExecutor = null;
        }

        mIsInitialized = false;
        Log.d(TAG, "ProducerConsumerTemplate successfully released.");
    }

    public synchronized boolean isInitialized() {
        return mIsInitialized;
    }

    public boolean isRunning() {
        return mIsRunning.get();
    }
}
