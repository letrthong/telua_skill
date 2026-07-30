package com.example.app.examples;

import android.content.Context;
import java.util.Objects;

/**
 * Standard Thread-Safe Singleton Reference Template.
 *
 * Key Principles Demonstrated:
 * 1. Bill Pugh Singleton Holder pattern for thread safety without synchronization overhead.
 * 2. Storing ApplicationContext to prevent Activity Context Memory Leaks.
 * 3. Private constructor preventing direct instantiation.
 */
public class SingletonTemplate {

    private final Context mApplicationContext;

    // Private constructor
    private SingletonTemplate(Context context) {
        // Safe: Store only ApplicationContext to prevent Memory Leaks
        this.mApplicationContext = context.getApplicationContext();
    }

    /**
     * Lazy-loaded, thread-safe instance holder created by the JVM classloader.
     */
    private static class InstanceHolder {
        private static SingletonTemplate sInstance;
    }

    /**
     * Thread-safe initialization method.
     * Must be called during Application onCreate().
     */
    public static synchronized void init(Context context) {
        Objects.requireNonNull(context, "Context cannot be null");
        if (InstanceHolder.sInstance == null) {
            InstanceHolder.sInstance = new SingletonTemplate(context);
        }
    }

    /**
     * Retrieves the global singleton instance.
     *
     * @return Thread-safe Singleton instance
     */
    public static SingletonTemplate getInstance() {
        if (InstanceHolder.sInstance == null) {
            throw new IllegalStateException("SingletonTemplate must be initialized before use!");
        }
        return InstanceHolder.sInstance;
    }

    public Context getApplicationContext() {
        return mApplicationContext;
    }
}
