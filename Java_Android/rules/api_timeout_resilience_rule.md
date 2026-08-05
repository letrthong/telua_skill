<!--
  Copyright (C) 2026 letrthong@gmail.com
  Created & Maintained by: letrthong@gmail.com
  Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
  Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
-->

# Third-Party Library API Latency & Timeout Resilience Rules (api_timeout_resilience_rule.md)

This document defines mandatory standards for evaluating execution latency of third-party library APIs/SDKs, configuring explicit timeouts, and wrapping potentially blocking 3-5 second calls in background threads to prevent UI hangs and ANR crashes.

---

## 1. Core Rules

### Rule 1.1: Mandatory Execution Latency Evaluation
Whenever integrating or calling a method/interface from an external library, SDK, database, hardware driver, or IPC service:
* **Evaluate Latency:** AI MUST assess if the call can potentially block or take noticeable time (e.g., 3–5 seconds under poor network/hardware conditions).
* **Prohibit Main Thread Execution:** Any call with potential blocking latency **MUST NOT** be invoked on the Main UI Thread.

### Rule 1.2: Mandatory Explicit Timeout Configuration
Never rely on default infinite timeouts provided by libraries. If a third-party API or client (e.g., OkHttpClient, Retrofit, Socket, gRPC) supports timeout settings, you **MUST** pass explicit timeout configurations (e.g., 3 to 5 seconds).

### Rule 1.3: Background Thread Wrapper with Timeout Handling
If a library method is synchronous/blocking and lacks native timeout parameters:
1. Execute the method on a background thread pool (`ExecutorService`).
2. Wrap the execution using `Future.get(timeout, TimeUnit.SECONDS)` or `CompletableFuture`.
3. Catch `TimeoutException` explicitly and trigger fallback logic to ensure the application never hangs.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Strictly Banned):

```java
// Bad 1: Calling blocking 3-5 second library method on Main Thread without timeout!
public class WeatherManager {
    public WeatherData fetchWeather() {
        ThirdPartyWeatherSdk sdk = new ThirdPartyWeatherSdk();
        // Danger: Blocking call! If server takes 5 seconds, UI hangs and triggers ANR!
        return sdk.blockingGetWeather(); 
    }
}
```

### ✅ REQUIRED BEST PRACTICE:

#### Option A: Native Library Timeout Configuration
```java
public class NetworkClientManager {
    private final OkHttpClient mOkHttpClient;

    public NetworkClientManager() {
        // Correct: Passing explicit 5-second timeouts to prevent hanging
        this.mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
    }
}
```

#### Option B: Background Thread Wrapper with Future Timeout Handling
```java
public class WeatherManager {
    private static final String TAG = WeatherManager.class.getSimpleName();
    private static final long API_TIMEOUT_SECONDS = 5;

    private ExecutorService mExecutor;

    public void init() {
        if (mExecutor == null || mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }
    }

    public Optional<WeatherData> fetchWeatherWithTimeout() {
        if (mExecutor == null || mExecutor.isShutdown()) {
            init();
        }

        ThirdPartyWeatherSdk sdk = new ThirdPartyWeatherSdk();
        Future<WeatherData> future = mExecutor.submit(sdk::blockingGetWeather);

        try {
            // Correct: Wait at most 5 seconds for library response off UI thread
            WeatherData data = future.get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return Optional.ofNullable(data);
        } catch (TimeoutException e) {
            // Correct: Catch timeout explicitly, cancel task, and log warning
            Log.w(TAG, "Third-party SDK weather call timed out after 5 seconds.", e);
            future.cancel(true); // Interrupt background thread
            return Optional.empty(); // Fallback return
        } catch (Exception e) {
            Log.e(TAG, "Error fetching weather data from SDK", e);
            return Optional.empty();
        }
    }

    public void release() {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before calling any third-party library API or SDK method:
1. [ ] Was potential latency evaluated to prevent Main UI Thread hangs? -> **Must be Yes**.
2. [ ] If native timeout parameters exist, are explicit timeouts (e.g., 3-5 seconds) passed? -> **Must be Yes**.
3. [ ] If the library call is blocking, is it wrapped in a background thread using `Future.get(timeout, ...)`? -> **Must be Yes**.
4. [ ] Is `TimeoutException` caught with a graceful fallback response? -> **Must be Yes**.
