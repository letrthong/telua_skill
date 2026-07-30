# Interface Integration & Dependency Registry Rules (interface_integration_registry_rule.md)

This document defines mandatory guidelines for recording, updating, and referencing third-party SDKs, library interfaces, and system API integrations in a persistent `docs/` directory. This ensures both human engineers and AI agents maintain a shared knowledge registry without performing redundant full-codebase scans in future development cycles.

---

## 1. Core Rules

### Rule 1.1: Mandatory Documentation in `docs/` Directory
Whenever integrating, wrapping, or modifying an external library, SDK interface, system service, or hardware binding:
The AI **MUST** create or update a dedicated integration document inside the **`docs/`** directory (e.g., `docs/integrations/` or `docs/<sdk_name>_integration.md`). The note **MUST** contain the following 4 mandatory sections:
1. **Required Imports & Packages:** Exact Java package imports (`import com.example.sdk.ApiManager;`).
2. **Build System Dependencies:** Exact lines added to build manifests (`build.gradle`, `Android.bp`, `Android.mk`, or `BUILD.gn`).
3. **Canonical Usage Pattern:** Minimal, self-contained, copy-pasteable usage snippet showing proper initialization and teardown.
4. **Known Risks, Pitfalls & Defensive Guards:** Potential ANR/thread risks, required Android permissions, timeout configurations, and memory leak prevention measures.

### Rule 1.2: Proactive Documentation Reference (Prevent Re-Scanning)
Before integrating or consuming an existing third-party library or system interface in subsequent turns:
* The AI **MUST** first consult the `docs/` directory to retrieve import paths, build dependencies, and safety guardrails.
* The AI **MUST NOT** perform redundant full-project file scans to re-discover already documented interface signatures.

---

## 2. Standard Integration Registry Template (`docs/<sdk_name>_integration.md`)

Whenever a new interface or SDK is integrated, create a markdown file in `docs/` using the following standard template format:

```markdown
# SDK / Interface Integration Registry: [Example: ThirdPartyPaymentSdk]

## 1. Required Java Imports
```java
import com.thirdparty.payment.PaymentClient;
import com.thirdparty.payment.PaymentRequest;
import com.thirdparty.payment.PaymentCallback;
import com.thirdparty.payment.PaymentException;
```

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

## 3. Canonical Usage Pattern
```java
public class PaymentManager {
    private PaymentClient mPaymentClient;

    public void init(Context context) {
        // Safe ApplicationContext initialization
        mPaymentClient = new PaymentClient(context.getApplicationContext());
    }

    public void processPayment(String orderId, int amount, PaymentCallback callback) {
        PaymentRequest request = new PaymentRequest(orderId, amount);
        // Always execute on background thread with 5-second timeout boundary
        mPaymentClient.executeAsync(request, 5, TimeUnit.SECONDS, callback);
    }
}
```

## 4. Known Risks, Pitfalls & Defensive Guards
* ⚠️ **ANR Risk:** `mPaymentClient.executeSync()` is a blocking call. Never invoke on the Main UI Thread.
* ⚠️ **Permissions Required:** Must declare `<uses-permission android:name="android.permission.INTERNET" />` in `AndroidManifest.xml`.
* ⚠️ **Timeout Requirement:** Must pass explicit 5-second timeout to avoid infinite loading state.
```

---

## 3. AI Self-Correction & Verification Checklist

Before completing any third-party SDK or interface integration task:
1. [ ] Was a dedicated integration note created/updated inside the `docs/` directory? -> **Must be Yes**.
2. [ ] Are all required Java `import` statements explicitly documented? -> **Must be Yes**.
3. [ ] Are exact build dependencies (`build.gradle` or `Android.bp`) recorded? -> **Must be Yes**.
4. [ ] Is a canonical, safe usage code snippet provided? -> **Must be Yes**.
5. [ ] Are known risks (ANR, timeouts, permissions, leaks) clearly documented? -> **Must be Yes**.
