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

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Benchmark Template: Object Lifetime Across Thread Shutdown (HEAP vs STACK).
 *
 * <p>Demonstrates the core Java memory model concept that an object created inside a
 * background thread <b>survives</b> after the thread terminates, because:
 * <ul>
 *   <li>The actual object data lives on the <b>HEAP</b> (shared across all threads).</li>
 *   <li>The local variable {@code objA} is only a <b>reference</b> on the thread's STACK,
 *       which is destroyed when the thread dies.</li>
 *   <li>As long as another thread (here the Main Thread via {@link AtomicReference})
 *       still holds a reference, the Garbage Collector (GC) keeps the object alive.</li>
 * </ul>
 *
 * <p>Complies with {@code rules/heap_stack_object_lifetime_rule.md}:
 * <ul>
 *   <li><b>Rule 2.1</b> — Publish the reference <i>before</i> the thread ends.</li>
 *   <li><b>Rule 2.2</b> — Safe publication via {@link AtomicReference} (happens-before).</li>
 *   <li><b>Rule 2.3</b> — Published object is immutable (final field, no setters).</li>
 *   <li><b>Rule 2.4</b> — {@code shutdown()} does NOT kill published HEAP objects.</li>
 *   <li><b>Rule 2.5</b> — Uses {@code awaitTermination()} instead of a busy-wait loop.</li>
 * </ul>
 *
 * <p>Complies with {@code rules/exception_handling_rule.md}:
 * <ul>
 *   <li><b>Rule 1.1</b> — Absolute prohibition of empty catch blocks (proper recovery & thread interrupt restoration).</li>
 *   <li><b>Rule 1.2</b> — Catch specific exceptions (e.g., {@link InterruptedException}).</li>
 *   <li><b>Rule 1.4</b> — Defensive handling and fallback when retrieving references to prevent NPE.</li>
 *   <li><b>Rule 1.5</b> — Command Query Separation (CQS) adhered to in methods.</li>
 *   <li><b>Rule 1.6</b> — Layered defense null-safety ({@code @NonNull} + {@link Objects#requireNonNull}).</li>
 * </ul>
 *
 * <p>Complies with {@code rules/magic_number_immutability_rule.md}:
 * <ul>
 *   <li><b>Rule 1.1</b> — Elimination of magic numbers and hardcoded string literals into UPPER_SNAKE_CASE constants.</li>
 *   <li><b>Rule 1.2</b> — Immutable local variables and parameter references via {@code final}.</li>
 * </ul>
 */
public final class SharedObjectShutdownTemplate {

    // Extracted named constants eliminating magic numbers and hardcoded strings (magic_number_immutability_rule.md)
    private static final long TERMINATION_TIMEOUT_SECONDS = 5L;
    private static final String DEFAULT_MESSAGE_CONTENT = "I'm still alive even though the thread is dead!";

    /**
     * Immutable message object shared across threads.
     * All fields are final to guarantee safe publication (Rule 2.3).
     * Complies with exception_handling_rule.md (Rule 1.6: Layered Defense).
     */
    public static final class MessageObject {
        private final String mContent;

        /**
         * Constructs a MessageObject with layered defense null-safety.
         *
         * @param content non-null payload text.
         */
        public MessageObject(@NonNull final String content) {
            // Rule 1.6: Compile-time contract (@NonNull) + runtime fail-fast boundary check (Objects.requireNonNull)
            mContent = Objects.requireNonNull(content, "content must not be null");
        }

        /**
         * CQS: Pure command method without hidden side-effects.
         */
        public void showMessage() {
            // Usage site: NO redundant null-check needed because mContent is guaranteed non-null
            System.out.println("Object State: " + mContent);
        }

        /**
         * CQS: Pure query method.
         *
         * @return guaranteed non-null content string.
         */
        @NonNull
        public String getContent() {
            return mContent;
        }
    }

    private SharedObjectShutdownTemplate() {
        // Prevent instantiation — utility demo class.
    }

    public static void main(final String[] args) {
        // 1. Create a thread-safe "container" in the Main Thread (acting as Object B).
        //    Guarantees happens-before relationship between background writer and main reader.
        final AtomicReference<MessageObject> sharedContainer = new AtomicReference<>();

        // 2. Create a Thread Pool (Worker Thread)
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        System.out.println("--- THREAD STARTED ---");
        executor.execute(() -> {
            try {
                // 3. Create Object A INSIDE the child thread.
                //    Memory allocation: the actual object is created in the HEAP memory.
                //    The variable 'objA' is just a reference stored in this Thread's STACK.
                final MessageObject objA = new MessageObject(DEFAULT_MESSAGE_CONTENT);

                // 4. Share Object A to the outside container (Rule 2.1: publish BEFORE thread ends).
                //    We are copying the memory address (reference) and giving it to the Main Thread.
                sharedContainer.set(objA);

                System.out.println("Child Thread: Object A created and shared successfully.");
            } catch (Exception e) {
                // Rule 1.1: Never swallow exceptions in asynchronous background tasks
                System.err.println("Child Thread encountered an unexpected error: " + e.getMessage());
            }
        });

        // 5. Command the Thread to SHUTDOWN (Rule 2.4: does NOT kill published objects).
        executor.shutdown();

        // 6. Wait until the thread is completely terminated (Rule 2.5: awaitTermination,
        //    NOT a busy-wait loop which would burn 100% CPU).
        try {
            if (!executor.awaitTermination(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // Rule 1.1 & 1.2: Specific exception caught, emergency pool termination,
            // and interrupt status restored to preserve execution state.
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            System.err.println("Main Thread interrupted during executor termination: " + e.getMessage());
        }
        System.out.println("--- THREAD IS COMPLETELY SHUTDOWN ---");

        // =========================================================================
        // EXPLANATION: WHY DOES THE OBJECT SURVIVE AFTER SHUTDOWN?
        // =========================================================================
        // 1. When the thread shuts down, its STACK memory is destroyed.
        //    This means the local variable 'objA' is gone forever.
        // 2. HOWEVER, the actual object data was allocated on the global HEAP memory.
        // 3. Because 'sharedContainer' (which lives in the Main Thread) still
        //    holds a copy of that reference pointing to the HEAP,
        //    Java's Garbage Collector (GC) sees that the object is still "in use".
        // 4. Therefore, GC does NOT delete it, and the object survives safely.
        // =========================================================================

        // 7. Retrieve and check Object A from the Main Thread (Rule 1.4 & Rule 1.6)
        final MessageObject retrievedObj = sharedContainer.get();

        if (retrievedObj != null) {
            System.out.print("Main Thread checking: ");
            retrievedObj.showMessage(); // Guaranteed non-null, prints state safely
        } else {
            // Rule 1.4: Fallback branch for unpopulated/timed-out reference
            System.err.println("Main Thread checking: Object was not published or task timed out.");
        }
    }
}
