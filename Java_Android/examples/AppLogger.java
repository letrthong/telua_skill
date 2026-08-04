package com.example.app.examples;

import android.util.Log;

/**
 * Production-Grade AppLogger Reference Utility.
 * Complies strictly with log_rule.md standards:
 *
 * 1. Encapsulates BuildConfig.DEBUG checks internally to avoid cluttering caller code with 'if (DEBUG)' blocks.
 * 2. Formats messages using varargs (Object... args) only when DEBUG is enabled, avoiding string concatenation overhead.
 * 3. Truncates TAG strings to 23 characters for legacy Android devices (API < 26) to prevent IllegalArgumentException.
 * 4. Error (e) and Warning (w) logs are allowed in production release builds for diagnostic tracking.
 */
public final class AppLogger {
    // In production, set to BuildConfig.DEBUG
    private static final boolean DEBUG = true; 
    private static final int MAX_TAG_LENGTH = 23;

    private AppLogger() {} // Prevent instantiation

    /**
     * Formats and truncates TAG string to 23 chars for legacy Android compatibility.
     */
    private static String sanitizeTag(String tag) {
        if (tag == null || tag.isBlank()) return "AppLogger";
        return (tag.length() > MAX_TAG_LENGTH) ? tag.substring(0, MAX_TAG_LENGTH) : tag;
    }

    /**
     * Verbose log — Guarded internally by DEBUG flag.
     */
    public static void v(String tag, String message, Object... args) {
        if (DEBUG) {
            Log.v(sanitizeTag(tag), (args != null && args.length > 0) ? String.format(message, args) : message);
        }
    }

    /**
     * Debug log — Guarded internally by DEBUG flag. 1-line clean call in caller.
     */
    public static void d(String tag, String message, Object... args) {
        if (DEBUG) {
            Log.d(sanitizeTag(tag), (args != null && args.length > 0) ? String.format(message, args) : message);
        }
    }

    /**
     * Info log — Always executes in Release and Debug.
     */
    public static void i(String tag, String message, Object... args) {
        Log.i(sanitizeTag(tag), (args != null && args.length > 0) ? String.format(message, args) : message);
    }

    /**
     * Warning log — Always executes in Release and Debug.
     */
    public static void w(String tag, String message, Object... args) {
        Log.w(sanitizeTag(tag), (args != null && args.length > 0) ? String.format(message, args) : message);
    }

    /**
     * Error log — Always executes in Release and Debug for critical error tracking.
     */
    public static void e(String tag, String message, Throwable throwable) {
        Log.e(sanitizeTag(tag), message, throwable);
    }
}
