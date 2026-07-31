# AI Mandatory Engineering Standards & Rules Manifest for Java/Android

This file serves as the primary system directive and master index manifest for AI code generation, refactoring, code review, and unit testing within the `Java_Android` workspace.

---

## 🚨 AI System Directive & Execution Workflow

Whenever generating, reviewing, or refactoring Java/Android code in this workspace, the AI **MUST** follow this 5-step execution workflow:

1. **Read Requirements (`requirements/`):** Understand business specifications, user stories, and acceptance criteria.
2. **Review System Design (`design/`):** Inspect class diagrams, sequence flows, and component architecture.
3. **Load Rules & Directives (`rules/`):** Load all 22 rule modules from the `rules/` directory (including non-blocking callback protocols).
4. **Benchmark against Templates (`examples/`):** Use the reference code templates in `examples/` as structural and stylistic benchmarks.
5. **Generate Code & Unit Tests:** Produce clean, defensive Java code along with a matching JUnit/Mockito unit test class (`MyClassTest.java`), pass Android Lint (`./gradlew lintDebug`), and update integration notes inside **`docs/`**.

---

## 📁 Project Architecture & Directory Map

* 📋 **[requirements/](file:///d:/code/telua_skill/Java_Android/requirements/README.md)**: Business logic, feature requirements, user stories, and acceptance criteria.
* 📐 **[design/](file:///d:/code/telua_skill/Java_Android/design/README.md)**: System architecture design, Mermaid class diagrams, sequence flows, and API specs.
* 📁 **[docs/](file:///d:/code/telua_skill/Java_Android/docs/example_sdk_integration.md)**: Shared knowledge registry for integrated SDKs, library dependencies, imports, and risks.
* 🛠️ **[tools/](file:///d:/code/telua_skill/Java_Android/tools/mcp_config_guide.md)**: Development tool configurations, GitHub MCP Server setup, and integration guides.
* 📁 **[examples/](file:///d:/code/telua_skill/Java_Android/examples/)**: 9 gold-standard benchmark reference templates.
* 📁 **[rules/](file:///d:/code/telua_skill/Java_Android/rules/)**: 22 mandatory engineering quality & safety rule modules.

---

## 📚 Master Index of Rule Modules (`rules/`)

### 1. Architectural & Structural Rules
* 📄 **[design_pattern_architecture_rule.md](file:///d:/code/telua_skill/Java_Android/rules/design_pattern_architecture_rule.md)**: MVC/MVVM separation, Strategy, Observer/Callback, Factory, Dependency Inversion, and `@CallbackExecutor` non-blocking callback protocol.
* 📄 **[objects_and_data_structures_rule.md](file:///d:/code/telua_skill/Java_Android/rules/objects_and_data_structures_rule.md)**: Strict Clean Code Chapter 6 separation of Data Structures (DTOs/Records) from Behavior Processors/Services and DAOs.
* 📄 **[encapsulation_rule.md](file:///d:/code/telua_skill/Java_Android/rules/encapsulation_rule.md)**: Strict private member field access, prohibition of public fields, defensive copying.
* 📄 **[lifecycle_init_rule.md](file:///d:/code/telua_skill/Java_Android/rules/lifecycle_init_rule.md)**: Lightweight constructors, prohibition of overridable methods in constructors, explicit & idempotent `init()` / `release()` lifecycle pattern.
* 📄 **[method_length_and_file_structure_rule.md](file:///d:/code/telua_skill/Java_Android/rules/method_length_and_file_structure_rule.md)**: Maximum 35-line method length limit, SRP helper method decomposition, and strict 1-to-1 class-to-file name matching.
* 📄 **[parameter_count_and_builder_rule.md](file:///d:/code/telua_skill/Java_Android/rules/parameter_count_and_builder_rule.md)**: Maximum 3-parameter limit per method, mandatory Parameter Object DTO and Builder Pattern for complex signatures.
* 📄 **[interface_integration_registry_rule.md](file:///d:/code/telua_skill/Java_Android/rules/interface_integration_registry_rule.md)**: Mandatory recording of integrated interfaces, imports, dependencies (`build.gradle`/`Android.bp`), usage, and risks inside the `docs/` folder.
* 📄 **[external_reference_rule.md](file:///d:/code/telua_skill/Java_Android/rules/external_reference_rule.md)**: Automated fetching and refactoring of external AOSP/GitHub/SDK reference URLs to workspace standards.

### 2. Safety, Threading & Resource Management Rules
* 📄 **[api_timeout_resilience_rule.md](file:///d:/code/telua_skill/Java_Android/rules/api_timeout_resilience_rule.md)**: Evaluating library API latency, explicit 3-5s timeout configuration, and background thread Future timeout wrappers.
* 📄 **[ui_thread_rule.md](file:///d:/code/telua_skill/Java_Android/rules/ui_thread_rule.md)**: Main UI thread safety, ANR prevention, background execution, and thread-safe View updates.
* 📄 **[executor_shutdown_rule.md](file:///d:/code/telua_skill/Java_Android/rules/executor_shutdown_rule.md)**: Mandatory shutdown of `Executor` / `ExecutorService` thread pools in lifecycle teardowns.
* 📄 **[resource_leak_rule.md](file:///d:/code/telua_skill/Java_Android/rules/resource_leak_rule.md)**: `try-with-resources` for `AutoCloseable`, SQLite Cursor closing, symmetric `BroadcastReceiver` unregistering.
* 📄 **[singleton_thread_safety_rule.md](file:///d:/code/telua_skill/Java_Android/rules/singleton_thread_safety_rule.md)**: Bill Pugh & volatile double-checked locking for thread-safe singletons, `ApplicationContext` usage.

### 3. Code Hygiene, Null Safety & Quality Rules
* 📄 **[naming_rule.md](file:///d:/code/telua_skill/Java_Android/rules/naming_rule.md)**: AOSP field prefixes (`m`/`s`), `UPPER_SNAKE_CASE` constants, JLS modifier ordering (`public static final`).
* 📄 **[magic_number_immutability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/magic_number_immutability_rule.md)**: Total prohibition of magic numbers/strings, mandatory extraction of constants, and defensive immutability.
* 📄 **[comment_and_documentation_rule.md](file:///d:/code/telua_skill/Java_Android/rules/comment_and_documentation_rule.md)**: Clean Code Chapter 4 standards: self-documenting code, Javadoc in English, elimination of noise comments.
* 📄 **[handler_rule.md](file:///d:/code/telua_skill/Java_Android/rules/handler_rule.md)**: Defensive null safety guards on chained getters, mandatory `removeCallbacksAndMessages(null)` cleanup.
* 📄 **[for_loop_rule.md](file:///d:/code/telua_skill/Java_Android/rules/for_loop_rule.md)**: Prohibition of manual `for(int i=0;...)` loops; mandatory use of enhanced `for(Item item : list)` or Java Streams.
* 📄 **[exception_handling_rule.md](file:///d:/code/telua_skill/Java_Android/rules/exception_handling_rule.md)**: Prohibition of empty catch blocks, catching specific exception types, preserving exception cause chaining.
* 📄 **[log_rule.md](file:///d:/code/telua_skill/Java_Android/rules/log_rule.md)**: Prohibition of `System.out.println()` / `e.printStackTrace()`, dynamic `TAG = MyClass.class.getSimpleName()`, PII data protection, `BuildConfig.DEBUG` guarding.
* 📄 **[unit_testability_rule.md](file:///d:/code/telua_skill/Java_Android/rules/unit_testability_rule.md)**: Constructor Dependency Injection, abstracting static/system calls, Arrange-Act-Assert (AAA) JUnit testing pattern, and mandatory test generation.
* 📄 **[checkstyle_lint_rule.md](file:///d:/code/telua_skill/Java_Android/rules/checkstyle_lint_rule.md)**: Checkstyle formatting, PMD quality gates, and Gradle Android Lint verification (`./gradlew lintDebug`).

---

## 💻 Reference Template Examples (`examples/`)

The following reference templates serve as gold-standard code benchmarks for AI code generation:
* ☕ **[ProducerConsumerTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/ProducerConsumerTemplate.java)**: Production-grade Producer-Consumer thread pattern with Bounded Blocking Queue backpressure control preventing thread hangs and OOM crashes.
* ☕ **[CarServiceConnectionTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/CarServiceConnectionTemplate.java)**: Non-blocking Android Automotive `Car.createCar` connection offloaded to a background thread executor with idempotent lifecycle teardown.
* ☕ **[DualThreadConnectionTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/DualThreadConnectionTemplate.java)**: Asynchronous connection thread signaling backend ready state to event receiver thread.
* ☕ **[ApiTimeoutResilienceTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/ApiTimeoutResilienceTemplate.java)**: Third-party SDK latency management, explicit 5-second timeout, and Future cancellation wrapper.
* ☕ **[ThreadingTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/ThreadingTemplate.java)**: Asynchronous task execution, UI thread dispatching, and idempotent lifecycle cleanup (`init()` / `release()`).
* ☕ **[ObserverStrategyFactoryTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/ObserverStrategyFactoryTemplate.java)**: Integrated Observer, Strategy, and Factory pattern system eliminating if-else branching.
* ☕ **[DesignPatternTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/DesignPatternTemplate.java)**: Strategy & Factory pattern implementation eliminating conditional branches.
* ☕ **[RepositoryPatternTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/RepositoryPatternTemplate.java)**: Clean Architecture repository pattern, Java Records, Optional null safety.
* ☕ **[SingletonTemplate.java](file:///d:/code/telua_skill/Java_Android/examples/SingletonTemplate.java)**: Thread-safe Bill Pugh Holder pattern and ApplicationContext leak prevention.

---

## 🛠️ Static Analysis Tools & Gradle Verification Commands

All generated Java code **MUST** pass static analysis checks before code delivery:

### 1. Run Android Lint via Gradle Terminal
```bash
# macOS / Linux
./gradlew lintDebug

# Windows PowerShell
.\gradlew lintDebug
```

### 2. Run Checkstyle & PMD Static Checks
```bash
# Checkstyle code formatting scan
./gradlew checkstyle

# PMD code quality scan
./gradlew pmd
```
