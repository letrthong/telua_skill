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
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Gold-Standard Benchmark: Safe Builder Pattern & Parameter Object Template.
 *
 * <p>Demonstrates full compliance with:
 * <ul>
 *   <li><b>rules/parameter_count_and_builder_rule.md</b>:
 *     <ul>
 *       <li>Rule 1.1: Method parameter count $\le 3$.</li>
 *       <li>Rule 1.2: Encapsulation into immutable Parameter Object / DTO.</li>
 *       <li>Rule 1.3: Fluent static nested Builder with rigorous validation in {@code build()}.</li>
 *     </ul>
 *   </li>
 *   <li><b>rules/objects_and_data_structures_rule.md</b>: Separation of Data Structures from Services.</li>
 *   <li><b>rules/encapsulation_rule.md</b>: Defensive copying of internal collections.</li>
 *   <li><b>rules/magic_number_immutability_rule.md</b>: Elimination of magic numbers & final fields.</li>
 *   <li><b>rules/exception_handling_rule.md</b>: Rule 1.6 Layered Defense ({@code @NonNull} + {@link Objects#requireNonNull}).</li>
 * </ul>
 *
 * <h3>The Problem (Excessive Method Parameters & Telescoping Constructors):</h3>
 * Methods or constructors taking 4+ arguments (especially multiple consecutive {@code String} or
 * {@code int} values) suffer from accidental argument swapping, poor readability, and rigid maintenance.
 *
 * <h3>The Solution:</h3>
 * <ol>
 *   <li>Encapsulate all configuration options into an immutable Parameter Object.</li>
 *   <li>Provide a fluent {@code Builder} with chainable setter methods returning {@code this}.</li>
 *   <li>Perform centralized boundary validation and null-safety fail-fast checks inside {@code build()}.</li>
 *   <li>Make the target configuration object effectively immutable with defensive copying.</li>
 * </ol>
 */
public final class SafeBuilderPatternTemplate {

    // Default configuration constants (magic_number_immutability_rule.md)
    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 5_000L;
    private static final long MIN_TIMEOUT_MS = 1_000L;
    private static final long MAX_TIMEOUT_MS = 60_000L;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int MAX_ALLOWED_RETRIES = 10;

    private SafeBuilderPatternTemplate() {
        // Prevent instantiation — utility template container.
    }

    /**
     * Service method showing adherence to Rule 1.1: Exactly 1 clean Parameter Object argument.
     *
     * @param config validated, immutable telemetry configuration parameter object.
     */
    public static void initializeTelemetryService(@NonNull final VehicleTelemetryConfig config) {
        Objects.requireNonNull(config, "VehicleTelemetryConfig must not be null");

        // Service consumes verified, non-null, immutable configuration safely
        System.out.println("Initializing Telemetry Service for Vehicle: " + config.getVin());
        System.out.println("Endpoint: " + config.getServerEndpoint() + " (Timeout: " + config.getConnectTimeoutMs() + "ms)");
        System.out.println("Max Retries: " + config.getMaxRetries() + " | SSL Enabled: " + config.isSslEnabled());
        System.out.println("Custom Headers Count: " + config.getCustomHeaders().size());
    }

    /**
     * Immutable Data Object configured strictly via its nested {@link Builder}.
     */
    public static final class VehicleTelemetryConfig {
        private final String mVin;
        private final String mServerEndpoint;
        private final long mConnectTimeoutMs;
        private final int mMaxRetries;
        private final boolean mSslEnabled;
        private final Map<String, String> mCustomHeaders;

        /**
         * Private constructor: Can ONLY be instantiated by the {@link Builder}.
         */
        private VehicleTelemetryConfig(@NonNull final Builder builder) {
            mVin = builder.mVin;
            mServerEndpoint = builder.mServerEndpoint;
            mConnectTimeoutMs = builder.mConnectTimeoutMs;
            mMaxRetries = builder.mMaxRetries;
            mSslEnabled = builder.mSslEnabled;
            // Rule 1.2 encapsulation_rule.md: Defensive unmodifiable copy of mutable collection
            mCustomHeaders = Collections.unmodifiableMap(new HashMap<>(builder.mCustomHeaders));
        }

        @NonNull
        public String getVin() {
            return mVin;
        }

        @NonNull
        public String getServerEndpoint() {
            return mServerEndpoint;
        }

        public long getConnectTimeoutMs() {
            return mConnectTimeoutMs;
        }

        public int getMaxRetries() {
            return mMaxRetries;
        }

        public boolean isSslEnabled() {
            return mSslEnabled;
        }

        @NonNull
        public Map<String, String> getCustomHeaders() {
            return mCustomHeaders; // Safely unmodifiable
        }

        /**
         * Fluent Builder class for {@link VehicleTelemetryConfig}.
         */
        public static final class Builder {
            // Mandatory fields
            private String mVin;
            private String mServerEndpoint;

            // Optional fields initialized with safe named constants
            private long mConnectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
            private int mMaxRetries = DEFAULT_MAX_RETRIES;
            private boolean mSslEnabled = true;
            private final Map<String, String> mCustomHeaders = new HashMap<>();

            /**
             * Default constructor for Builder.
             */
            public Builder() {
            }

            /**
             * Sets mandatory vehicle VIN.
             */
            @NonNull
            public Builder setVin(@NonNull final String vin) {
                mVin = Objects.requireNonNull(vin, "VIN must not be null");
                return this;
            }

            /**
             * Sets mandatory server endpoint URL.
             */
            @NonNull
            public Builder setServerEndpoint(@NonNull final String serverEndpoint) {
                mServerEndpoint = Objects.requireNonNull(serverEndpoint, "ServerEndpoint must not be null");
                return this;
            }

            /**
             * Sets connection timeout with range verification.
             */
            @NonNull
            public Builder setConnectTimeoutMs(final long timeoutMs) {
                if (timeoutMs < MIN_TIMEOUT_MS || timeoutMs > MAX_TIMEOUT_MS) {
                    throw new IllegalArgumentException("Timeout out of valid bounds ["
                            + MIN_TIMEOUT_MS + "ms - " + MAX_TIMEOUT_MS + "ms]: " + timeoutMs);
                }
                mConnectTimeoutMs = timeoutMs;
                return this;
            }

            /**
             * Sets max retry count with boundary verification.
             */
            @NonNull
            public Builder setMaxRetries(final int maxRetries) {
                if (maxRetries < 0 || maxRetries > MAX_ALLOWED_RETRIES) {
                    throw new IllegalArgumentException("Max retries out of range [0 - "
                            + MAX_ALLOWED_RETRIES + "]: " + maxRetries);
                }
                mMaxRetries = maxRetries;
                return this;
            }

            /**
             * Toggles SSL encryption flag.
             */
            @NonNull
            public Builder setSslEnabled(final boolean sslEnabled) {
                mSslEnabled = sslEnabled;
                return this;
            }

            /**
             * Appends a custom HTTP header.
             */
            @NonNull
            public Builder addHeader(@NonNull final String key, @NonNull final String value) {
                Objects.requireNonNull(key, "Header key must not be null");
                Objects.requireNonNull(value, "Header value must not be null");
                mCustomHeaders.put(key, value);
                return this;
            }

            /**
             * Builds and validates the immutable {@link VehicleTelemetryConfig}.
             *
             * @return fully validated, immutable configuration object.
             * @throws IllegalStateException if any mandatory parameters are missing or invalid.
             */
            @NonNull
            public VehicleTelemetryConfig build() {
                // Rule 1.6: Boundary fail-fast verification
                if (mVin == null || mVin.isBlank()) {
                    throw new IllegalStateException("Cannot build VehicleTelemetryConfig: VIN must not be empty.");
                }
                if (mServerEndpoint == null || mServerEndpoint.isBlank()) {
                    throw new IllegalStateException("Cannot build VehicleTelemetryConfig: ServerEndpoint must not be empty.");
                }

                return new VehicleTelemetryConfig(this);
            }
        }
    }
}
