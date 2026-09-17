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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gold-Standard Benchmark: System Resource Leak Prevention & Symmetric Lifecycle Template.
 *
 * <p>Demonstrates full compliance with:
 * <ul>
 *   <li><b>rules/resource_leak_rule.md</b>:
 *     <ul>
 *       <li>Rule 1.1: Mandatory {@code try-with-resources} for {@link AutoCloseable} streams & sockets.</li>
 *       <li>Rule 1.2: Safe Database {@link Cursor} resource closure.</li>
 *       <li>Rule 1.3: Symmetric lifecycle registration & unregistration for {@link BroadcastReceiver}.</li>
 *     </ul>
 *   </li>
 *   <li><b>rules/exception_handling_rule.md</b>:
 *     <ul>
 *       <li>Rule 1.1: Zero empty catch blocks with structured logging and fallback.</li>
 *       <li>Rule 1.2: Catching specific exceptions ({@link IOException}, {@link IllegalArgumentException}).</li>
 *       <li>Rule 1.6: Layered defense null-safety ({@code @NonNull} + {@link Objects#requireNonNull}).</li>
 *     </ul>
 *   </li>
 *   <li><b>rules/magic_number_immutability_rule.md</b>: Extracted UPPER_SNAKE_CASE constants and immutable returns.</li>
 * </ul>
 *
 * <h3>The Problem (Silent System Resource Depletion):</h3>
 * <ul>
 *   <li><b>File Descriptor (FD) Leaks:</b> Failing to close an {@code InputStream} on error leaks OS file handles,
 *       eventually crashing the process with "Too many open files".</li>
 *   <li><b>Cursor Leaks:</b> Unclosed SQLite cursors waste native memory and trigger Android SQLite leak warnings.</li>
 *   <li><b>Receiver Leaks:</b> Registering a {@code BroadcastReceiver} without symmetric unregistration leaks
 *       the enclosing Activity/Context, or crashing with {@code IllegalArgumentException: Receiver not registered}.</li>
 * </ul>
 */
public class ResourceLeakSafetyTemplate {
    private static final String TAG = ResourceLeakSafetyTemplate.class.getSimpleName();

    private static final String ACTION_VEHICLE_STATE_CHANGED = "com.example.app.ACTION_VEHICLE_STATE_CHANGED";
    private static final int BUFFER_SIZE_BYTES = 4096;

    private final Context mContext;
    private final VehicleBroadcastReceiver mReceiver;
    private final AtomicBoolean mIsReceiverRegistered = new AtomicBoolean(false);

    /**
     * Constructs the resource manager with verified context.
     *
     * @param context Android context, must not be null.
     */
    public ResourceLeakSafetyTemplate(@NonNull final Context context) {
        mContext = Objects.requireNonNull(context, "Context must not be null");
        mReceiver = new VehicleBroadcastReceiver();
    }

    // =========================================================================
    // 1. SYMMETRIC LIFECYCLE MANAGEMENT (resource_leak_rule.md Rule 1.3)
    // =========================================================================

    /**
     * Symmetrically registers the broadcast receiver (e.g. called in onStart() or onResume()).
     * Idempotent: guarded by AtomicBoolean to prevent duplicate registration.
     */
    public synchronized void registerReceiver() {
        if (mIsReceiverRegistered.compareAndSet(false, true)) {
            final IntentFilter filter = new IntentFilter(ACTION_VEHICLE_STATE_CHANGED);
            mContext.registerReceiver(mReceiver, filter);
            Log.d(TAG, "VehicleBroadcastReceiver symmetrically registered.");
        }
    }

    /**
     * Symmetrically unregisters the broadcast receiver (e.g. called in onStop() or onDestroy()).
     * Idempotent: guarded by AtomicBoolean to prevent "Receiver not registered" crash.
     */
    public synchronized void unregisterReceiver() {
        if (mIsReceiverRegistered.compareAndSet(true, false)) {
            try {
                mContext.unregisterReceiver(mReceiver);
                Log.d(TAG, "VehicleBroadcastReceiver symmetrically unregistered.");
            } catch (IllegalArgumentException e) {
                // Rule 1.1: Log gracefully if Android OS already unregistered or discarded receiver
                Log.w(TAG, "Receiver was already unregistered by the system: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 2. FILE & STREAM I/O TRY-WITH-RESOURCES (resource_leak_rule.md Rule 1.1)
    // =========================================================================

    /**
     * Reads file lines safely using try-with-resources.
     *
     * @param sourceFile file to read.
     * @return unmodifiable list of file lines.
     * @throws IOException on read or I/O failure.
     */
    @NonNull
    public List<String> readTextFileLinesSafely(@NonNull final File sourceFile) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile must not be null");

        if (!sourceFile.exists() || !sourceFile.canRead()) {
            Log.w(TAG, "Cannot read file: does not exist or permission denied: " + sourceFile.getAbsolutePath());
            return Collections.emptyList();
        }

        final List<String> lines = new ArrayList<>();

        // Rule 1.1: Both FileInputStream and BufferedReader are automatically closed
        // even if an IOException is thrown during processing.
        try (FileInputStream fis = new FileInputStream(sourceFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr, BUFFER_SIZE_BYTES)) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        return Collections.unmodifiableList(lines);
    }

    /**
     * Writes binary payload safely using try-with-resources.
     *
     * @param destination destination file.
     * @param data binary payload.
     * @throws IOException on write error.
     */
    public void writePayloadSafely(@NonNull final File destination, @NonNull final byte[] data) throws IOException {
        Objects.requireNonNull(destination, "destination file must not be null");
        Objects.requireNonNull(data, "data buffer must not be null");

        // Rule 1.1: FileOutputStream guaranteed to be closed upon block exit
        try (FileOutputStream fos = new FileOutputStream(destination)) {
            fos.write(data);
            fos.flush();
        }
    }

    // =========================================================================
    // 3. DATABASE CURSOR MANAGEMENT (resource_leak_rule.md Rule 1.2)
    // =========================================================================

    /**
     * Extracts record IDs safely from a Database Cursor using try-with-resources.
     *
     * @param cursor SQLite cursor instance (implements Closeable/AutoCloseable).
     * @return unmodifiable list of extracted IDs.
     */
    @NonNull
    public List<String> processCursorSafely(@Nullable final Cursor cursor) {
        if (cursor == null) {
            return Collections.emptyList();
        }

        final List<String> recordIds = new ArrayList<>();

        // Rule 1.2: Cursor implements AutoCloseable in Android API 16+
        try (cursor) {
            final int idColumnIndex = cursor.getColumnIndex("record_id");
            if (idColumnIndex == -1) {
                Log.w(TAG, "Column 'record_id' not found in cursor result set.");
                return Collections.emptyList();
            }

            while (cursor.moveToNext()) {
                final String id = cursor.getString(idColumnIndex);
                if (id != null) {
                    recordIds.add(id);
                }
            }
        } // Cursor is unconditionally closed here, preventing SQLite native cursor leaks

        return Collections.unmodifiableList(recordIds);
    }

    /**
     * Lifecycle teardown releasing all lingering resources.
     */
    public void release() {
        unregisterReceiver();
    }

    /**
     * Internal BroadcastReceiver implementation.
     */
    private static final class VehicleBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (ACTION_VEHICLE_STATE_CHANGED.equals(intent.getAction())) {
                Log.i(TAG, "Received vehicle state change broadcast event.");
            }
        }
    }
}
