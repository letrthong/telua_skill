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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Concrete Benchmark Template: Media Playback State Management via Volatile + Local Copy.
 *
 * <p>Demonstrates the standard solution to the classic "Time-of-check to time-of-use" (TOCTOU)
 * race condition and NullPointerException in Android multithreaded applications.
 *
 * <h3>Real-World Problem:</h3>
 * In a media player app, a Background Audio Service updates the playback track, while the UI Main Thread
 * or Notification Manager reads that track to display title, artist, and duration.
 *
 * <h3>❌ The Bug Without Local Copy:</h3>
 * <pre>{@code
 * public void updateUi() {
 *     if (mCurrentTrack != null) {
 *         // RACE CONDITION: Background thread stops playback and executes 'mCurrentTrack = null' here!
 *         mTitleView.setText(mCurrentTrack.getTitle()); // CRASH: NullPointerException!
 *     }
 * }
 * }</pre>
 *
 * <h3>✅ The Solution With Local Copy:</h3>
 * <pre>{@code
 * public void updateUi() {
 *     TrackInfo snapshot = mCurrentTrack; // Step 1: Capture stable local snapshot
 *     if (snapshot != null) {
 *         // Even if background thread sets mCurrentTrack = null, 'snapshot' remains intact!
 *         mTitleView.setText(snapshot.getTitle()); // 100% crash-free!
 *     }
 * }
 * }</pre>
 */
public class MediaPlaybackStateTemplate {
    private static final String TAG = MediaPlaybackStateTemplate.class.getSimpleName();

    /**
     * Immutable snapshot data class representing track information.
     * All fields are final to guarantee Safe Publication across threads.
     */
    public static final class TrackInfo {
        private final String mTitle;
        private final String mArtist;
        private final long mDurationMs;
        private final boolean mIsPlaying;

        public TrackInfo(@NonNull String title, @NonNull String artist, long durationMs, boolean isPlaying) {
            this.mTitle = Objects.requireNonNull(title, "title must not be null");
            this.mArtist = Objects.requireNonNull(artist, "artist must not be null");
            this.mDurationMs = Math.max(0L, durationMs);
            this.mIsPlaying = isPlaying;
        }

        @NonNull public String getTitle() { return mTitle; }
        @NonNull public String getArtist() { return mArtist; }
        public long getDurationMs() { return mDurationMs; }
        public boolean isPlaying() { return mIsPlaying; }

        @NonNull
        public String formatDisplay() {
            String state = mIsPlaying ? "▶ Playing" : "⏸ Paused";
            return String.format("%s: %s - %s (%ds)", state, mTitle, mArtist, mDurationMs / 1000);
        }
    }

    // Volatile reference to an IMMUTABLE snapshot -> Visibility across all threads
    private volatile TrackInfo mCurrentTrack;

    // Thread-safe lifecycle flag
    private final AtomicBoolean mIsInitialized = new AtomicBoolean(false);

    /**
     * Initializes the player state holder. Idempotent per lifecycle_init_rule.md.
     */
    public synchronized void init() {
        if (mIsInitialized.get()) {
            AppLogger.d(TAG, "Already initialized. Skipping duplicate init() call.");
            return;
        }

        mCurrentTrack = null;
        mIsInitialized.set(true);
        AppLogger.d(TAG, "MediaPlaybackStateTemplate initialized.");
    }

    /**
     * Background Audio Service calls this method when a new song starts playing.
     * Atomic publish: Writes to volatile reference.
     *
     * @param newTrack the new immutable track info; must not be null
     */
    public void publishNewTrack(@NonNull TrackInfo newTrack) {
        Objects.requireNonNull(newTrack, "newTrack must not be null");
        // Single volatile write -> published atomically to all reader threads
        mCurrentTrack = newTrack;
        AppLogger.d(TAG, "[BackgroundService] Published new track: %s", newTrack.getTitle());
    }

    /**
     * Background Audio Service calls this when playback stops or audio session is destroyed.
     */
    public void stopPlayback() {
        mCurrentTrack = null;
        AppLogger.d(TAG, "[BackgroundService] Playback stopped. Track cleared to null.");
    }

    /**
     * UI Thread / Notification Manager calls this method to format the notification banner.
     *
     * <p>Demonstrates the IDIOMATIC Volatile Local Copy Pattern:
     * 1. Reads volatile reference ONCE into thread-local variable {@code snapshot}.
     * 2. All subsequent checks and string building use {@code snapshot}.
     * 3. Guarantees 100% crash immunity against concurrent {@link #stopPlayback()} calls.
     *
     * @return display string for UI / Notification
     */
    @NonNull
    public String getNotificationDisplayText() {
        // --- IDIOMATIC PATTERN ---
        // Step 1: Capture volatile reference into a local snapshot variable
        TrackInfo snapshot = mCurrentTrack;

        // Step 2: Use local variable for ALL logic in this method
        if (snapshot == null) {
            return "No track currently playing";
        }

        // Even if another thread executes stopPlayback() (mCurrentTrack = null) right now,
        // our 'snapshot' reference remains valid and consistent!
        return snapshot.formatDisplay();
    }

    /**
     * Checks if a specific song is currently playing.
     *
     * <p>Demonstrates safe Check-Then-Act without TOCTOU race conditions:
     * We avoid {@code if (mCurrentTrack != null && mCurrentTrack.getTitle().equals(...))} which
     * can throw NullPointerException if another thread clears {@code mCurrentTrack} between checks.
     *
     * @param targetTitle song title to check
     * @return {@code true} if current track matches target title and is playing
     */
    public boolean isTrackPlaying(@Nullable String targetTitle) {
        if (targetTitle == null) {
            return false;
        }

        // Single read snapshot
        TrackInfo snapshot = mCurrentTrack;

        // Both condition check and field access operate on the exact same snapshot
        return snapshot != null && snapshot.isPlaying() && targetTitle.equalsIgnoreCase(snapshot.getTitle());
    }

    /**
     * Returns an immutable snapshot of the current track, if any.
     *
     * @return current track snapshot, or null if stopped
     */
    @Nullable
    public TrackInfo getCurrentTrackSnapshot() {
        return mCurrentTrack; // Returning immutable object reference is 100% thread-safe
    }

    /**
     * Cleans up the state holder. Idempotent per lifecycle_init_rule.md.
     */
    public synchronized void release() {
        if (!mIsInitialized.get()) {
            AppLogger.d(TAG, "Already released or not initialized.");
            return;
        }

        mCurrentTrack = null;
        mIsInitialized.set(false);
        AppLogger.d(TAG, "MediaPlaybackStateTemplate released.");
    }

    public boolean isInitialized() {
        return mIsInitialized.get();
    }
}
