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

import android.content.Context;
import java.util.Objects;

/**
 * Standard Thread-Safe Singleton Reference Template.
 *
 * Key Principles Demonstrated:
 * 1. Volatile Double-Checked Locking (DCL) for thread safety with minimal synchronization overhead.
 *    NOTE: Bill Pugh Holder is NOT used here because this singleton requires a {@link Context}
 *    parameter at creation time; Bill Pugh only works for parameterless constructors.
 * 2. Storing ApplicationContext to prevent Activity Context Memory Leaks.
 * 3. Private constructor preventing direct instantiation.
 */
public class SingletonTemplate {

    // 'volatile' is MANDATORY: prevents instruction reordering so the partially-constructed
    // instance is never published to other threads (see singleton_thread_safety_rule.md Rule 1.2).
    private static volatile SingletonTemplate sInstance;

    private final Context mApplicationContext;

    // Private constructor
    private SingletonTemplate(Context context) {
        // Safe: Store only ApplicationContext to prevent Memory Leaks
        this.mApplicationContext = context.getApplicationContext();
    }

    /**
     * Thread-safe initialization method (idempotent).
     * Must be called during Application onCreate().
     */
    public static void init(Context context) {
        Objects.requireNonNull(context, "Context cannot be null");
        if (sInstance == null) {
            synchronized (SingletonTemplate.class) {
                if (sInstance == null) {
                    sInstance = new SingletonTemplate(context);
                }
            }
        }
    }

    /**
     * Retrieves the global singleton instance.
     *
     * @return Thread-safe Singleton instance
     * @throws IllegalStateException if {@link #init(Context)} has not been called yet
     */
    public static SingletonTemplate getInstance() {
        // Local copy snapshot: avoids TOCTOU race between the null check and the return.
        SingletonTemplate instance = sInstance;
        if (instance == null) {
            throw new IllegalStateException("SingletonTemplate must be initialized before use!");
        }
        return instance;
    }

    public Context getApplicationContext() {
        return mApplicationContext;
    }
}
