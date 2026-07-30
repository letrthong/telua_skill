# Code Testability & Unit Testing Rules (unit_testability_rule.md)

This document defines mandatory guidelines for writing testable Java/Android code and structuring Unit Tests.

---

## 1. Core Testability Rules

### Rule 1.1: Constructor Dependency Injection
Always inject dependencies via interfaces into the constructor. Never instantiate concrete dependencies inside business methods using `new` (e.g., `new NetworkClient()`), as this prevents mock injection during testing.

### Rule 1.2: Avoid Hardcoded Static Calls & System Clocks
Never call static system utilities directly (e.g., `System.currentTimeMillis()`, `Calendar.getInstance()`, `Log.d()`) inside business logic. Wrap time, system, and Android framework calls behind interface abstractions (e.g., `TimeProvider`) so they can be deterministically mocked.

### Rule 1.3: Single Responsibility & Pure Logic Methods
Keep business methods focused on a single responsibility with clear inputs and outputs. Avoid hidden global state mutations.

### Rule 1.4: Arrange-Act-Assert (AAA) Test Pattern
Unit test methods **MUST** follow the **Arrange-Act-Assert (AAA)** structure and use clear `@Test` method naming: `methodName_givenState_shouldExpectedBehavior()`.

### Rule 1.5: Mandatory Accompanying Unit Test Generation
Whenever generating, modifying, or refactoring Java business logic, services, repositories, or controllers, the AI **MUST ALWAYS** generate a matching JUnit/Mockito unit test class (e.g., `MyServiceTest`) containing test cases that cover both successful scenarios and edge/error cases.

---

## 2. Code Transformation Examples

### ❌ ANTI-PATTERN (Untestable Code & Missing Unit Tests):

```java
// Bad: Hardcoded concrete dependencies, static time call -> Untestable!
// Bad: Generating business code without providing the corresponding Unit Test class!
public class OrderService {
    public boolean processOrder(Order order) {
        // Tightly coupled dependency - impossible to mock in unit tests!
        PaymentGateway gateway = new StripeGateway(); 
        
        // Static system call - impossible to control time in tests!
        long timestamp = System.currentTimeMillis(); 
        
        return gateway.charge(order.getAmount());
    }
}
```

### ✅ REQUIRED BEST PRACTICE (Testable Code & Accompanying Unit Test):

#### 1. Testable Class Design (`OrderService.java`)
```java
public class OrderService {
    private final PaymentGateway mGateway;
    private final TimeProvider mTimeProvider;

    // Correct: Constructor Dependency Injection via Interfaces
    public OrderService(PaymentGateway gateway, TimeProvider timeProvider) {
        this.mGateway = Objects.requireNonNull(gateway);
        this.mTimeProvider = Objects.requireNonNull(timeProvider);
    }

    public boolean processOrder(Order order) {
        long timestamp = mTimeProvider.getCurrentTimeMillis();
        return mGateway.charge(order.getAmount(), timestamp);
    }
}
```

#### 2. Accompanying Unit Test Class (`OrderServiceTest.java`)
```java
@RunWith(MockitoJUnitRunner.class)
public class OrderServiceTest {

    @Mock private PaymentGateway mMockGateway;
    @Mock private TimeProvider mMockTimeProvider;

    private OrderService mOrderService;

    @Before
    public void setUp() {
        mOrderService = new OrderService(mMockGateway, mMockTimeProvider);
    }

    @Test
    public void processOrder_givenValidAmount_shouldReturnTrue() {
        // 1. ARRANGE
        Order order = new Order("ORD-101", 100);
        when(mMockTimeProvider.getCurrentTimeMillis()).thenReturn(1000000L);
        when(mMockGateway.charge(100, 1000000L)).thenReturn(true);

        // 2. ACT
        boolean result = mOrderService.processOrder(order);

        // 3. ASSERT
        assertTrue(result);
        verify(mMockGateway).charge(100, 1000000L);
    }

    @Test(expected = NullPointerException.class)
    public void processOrder_givenNullOrder_shouldThrowException() {
        // Edge case testing
        mOrderService.processOrder(null);
    }
}
```

---

## 3. AI Self-Correction & Verification Checklist

Before emitting code to ensure unit testability:
1. [ ] Are all external dependencies passed through constructor interfaces instead of `new`? -> **Must be Yes**.
2. [ ] Are static system/time calls abstracted behind injectable interfaces? -> **Must be Yes**.
3. [ ] Did I generate an accompanying `@Test` unit test class alongside the implementation code? -> **Must be Yes**.
4. [ ] Are Unit Test classes formatted using the Arrange-Act-Assert (AAA) pattern? -> **Must be Yes**.
