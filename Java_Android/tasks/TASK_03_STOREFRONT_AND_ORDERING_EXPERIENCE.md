# Task Execution: Storefront & Ordering Experience Engine (`TASK_03_STOREFRONT_AND_ORDERING_EXPERIENCE.md`)

* **Task ID:** `TASK-03`
* **Feature Name:** Storefront & In-Vehicle Ordering Experience Engine
* **Status:** 🔄 `IN_PROGRESS`
* **Target Package:** `com.example.app.storefront`
* **Requirements Spec:** [requirements/template.md](file:///d:/code/telua_skill/Java_Android/requirements/template.md)
* **Architecture Blueprint:** [design/template.md](file:///d:/code/telua_skill/Java_Android/design/template.md)

---

## 1. Feature Objective & Scope
Build a robust, thread-safe Storefront and In-Vehicle Digital Ordering Experience engine. Enables vehicle occupants to browse in-car service upgrades (e.g. Navigation Plus, Cabin Ambient Packs), manage a persistent Cart state with immutable snapshots, and process order submission with automatic network retry resilience.

---

## 2. Step-by-Step Execution Checklist

### Phase 1: Requirements & Architecture Alignment
* [x] **Subtask 1.1:** Parse functional requirements (FR-101 catalog browsing, FR-102 cart management, FR-103 checkout) from [requirements/template.md](file:///d:/code/telua_skill/Java_Android/requirements/template.md).
* [x] **Subtask 1.2:** Inspect Clean Architecture sequence diagram in [design/template.md](file:///d:/code/telua_skill/Java_Android/design/template.md) (Repository + Reactive Observer pattern).
* [x] **Subtask 1.3:** Architectural Sign-off: Chosen **Repository Pattern + Volatile Local Copy State** over bloated singletons.

### Phase 2: Domain Data Structures & Contracts
* [x] **Subtask 2.1:** Implement immutable DTO Java Records separating data from behavior ([objects_and_data_structures_rule.md](file:///d:/code/telua_skill/Java_Android/rules/objects_and_data_structures_rule.md)):
  - `StoreItemRecord(String itemId, String title, long priceCents, String category)`
  - `CartSnapshotRecord(List<StoreItemRecord> items, long totalAmountCents, long timestamp)`
* [x] **Subtask 2.2:** Define clean public interface `StorefrontRepository`:
  - `List<StoreItemRecord> getAvailableItems()`
  - `CartSnapshotRecord getCartSnapshot()`
  - `void addItemToCart(StoreItemRecord item)`
  - `void clearCart()`

### Phase 3: Core Implementation & Thread Safety (Current Focus)
* [x] **Subtask 3.1:** Implement `StorefrontRepositoryImpl` with `private volatile CartSnapshotRecord mCartSnapshot` implementing **Volatile Local Copy Pattern** ([thread_safety_concurrency_rule.md](file:///d:/code/telua_skill/Java_Android/rules/thread_safety_concurrency_rule.md)).
* [x] **Subtask 3.2:** Enforce immutable safe publication: Cart updates construct a new `CartSnapshotRecord` rather than mutating internal list in-place.
* [ ] **Subtask 3.3:** Implement asynchronous order submission with 5-second timeout wrapper ([api_timeout_resilience_rule.md](file:///d:/code/telua_skill/Java_Android/rules/api_timeout_resilience_rule.md)).
* [ ] **Subtask 3.4:** Implement idempotent `init()` and `release()` lifecycle teardown ([lifecycle_init_rule.md](file:///d:/code/telua_skill/Java_Android/rules/lifecycle_init_rule.md)).
* [ ] **Subtask 3.5:** Replace all hardcoded strings with class-level constants ([magic_number_immutability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/magic_number_immutability_rule.md)).

### Phase 4: Unit Testing & Verification
* [ ] **Subtask 4.1:** Generate `StorefrontRepositoryTest.java` implementing Arrange-Act-Assert (AAA) JUnit 4 + Mockito test suite ([unit_testability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/unit_testability_rule.md)).
* [ ] **Subtask 4.2:** Verify cart snapshot consistency across concurrent reader/writer threads (Zero TOCTOU NPE).
* [ ] **Subtask 4.3:** Run automated verification: `./gradlew testDebugUnitTest`.
* [ ] **Subtask 4.4:** Run static analysis verification: `./gradlew lintDebug`.

### Phase 5: Documentation & Sign-Off
* [ ] **Subtask 5.1:** Register payment gateway dependencies and network permissions in `docs/`.
* [ ] **Subtask 5.2:** Update [tasks/CHECKLIST.md](file:///d:/code/telua_skill/Java_Android/tasks/CHECKLIST.md) to mark `TASK-03` as `COMPLETED`.

---

## 3. Engineering Rules Enforced
* [x] [naming_rule.md](file:///d:/code/telua_skill/Java_Android/rules/naming_rule.md) (Fields prefixed with `m`/`s`, constants in `UPPER_SNAKE_CASE`)
* [x] [objects_and_data_structures_rule.md](file:///d:/code/telua_skill/Java_Android/rules/objects_and_data_structures_rule.md) (Immutable Records for cart items)
* [x] [thread_safety_concurrency_rule.md](file:///d:/code/telua_skill/Java_Android/rules/thread_safety_concurrency_rule.md) (Local Copy snapshots, safe publication)
* [ ] [api_timeout_resilience_rule.md](file:///d:/code/telua_skill/Java_Android/rules/api_timeout_resilience_rule.md) (5-second timeout on payment checkout)
* [ ] [log_rule.md](file:///d:/code/telua_skill/Java_Android/rules/log_rule.md) (Logging via `AppLogger`, zero PII payment data logged)

---

## 4. Deliverables & Progress Tracker

| Component | Target File | Status | Notes |
| :--- | :--- | :---: | :--- |
| **Data Record DTOs** | `com.example.app.storefront.StoreItemRecord` | ✅ DONE | Immutable Java Record |
| **Cart Snapshot** | `com.example.app.storefront.CartSnapshotRecord` | ✅ DONE | Defensive unmodifiable list |
| **Repository Interface**| `com.example.app.storefront.StorefrontRepository` | ✅ DONE | Clean abstraction |
| **Repository Implementation** | `com.example.app.storefront.StorefrontRepositoryImpl` | 🔄 CODING | Implementing checkout timeout |
| **Unit Test Suite** | `com.example.app.storefront.StorefrontRepositoryTest` | ⏳ PENDING | AAA JUnit 4 + Mockito |
