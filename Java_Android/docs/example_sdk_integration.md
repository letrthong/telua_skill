# SDK Integration Note: ThirdPartyPaymentSdk

This document records the exact integration details for `ThirdPartyPaymentSdk` to prevent redundant codebase scanning in future development tasks.

---

## 1. Required Java Imports
```java
import com.thirdparty.payment.PaymentClient;
import com.thirdparty.payment.PaymentRequest;
import com.thirdparty.payment.PaymentCallback;
import com.thirdparty.payment.PaymentException;
```

---

## 2. Build System Dependencies
* **Gradle (`app/build.gradle`):**
  ```groovy
  dependencies {
      implementation 'com.thirdparty.sdk:payment:2.4.0'
  }
  ```
* **AOSP Build (`Android.bp`):**
  ```blueprint
  java_libs: [
      "thirdparty-payment-sdk",
  ],
  ```

---

## 3. Canonical Usage Pattern
```java
public class PaymentManager {
    private PaymentClient mPaymentClient;

    public void init(Context context) {
        // Safe ApplicationContext initialization to prevent Activity memory leaks
        this.mPaymentClient = new PaymentClient(context.getApplicationContext());
    }

    public void processPayment(String orderId, int amount, PaymentCallback callback) {
        PaymentRequest request = new PaymentRequest(orderId, amount);
        // Execute off Main Thread with explicit 5-second timeout boundary
        mPaymentClient.executeAsync(request, 5, TimeUnit.SECONDS, callback);
    }
}
```

---

## 4. Known Risks, Pitfalls & Defensive Guards
* ⚠️ **ANR Risk:** `mPaymentClient.executeSync()` is a blocking call. Never invoke on the Main UI Thread.
* ⚠️ **Permissions Required:** Must declare `<uses-permission android:name="android.permission.INTERNET" />` in `AndroidManifest.xml`.
* ⚠️ **Timeout Requirement:** Must pass explicit 5-second timeout to avoid infinite loading state.
