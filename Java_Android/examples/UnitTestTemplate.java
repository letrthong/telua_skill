package com.example.app.examples;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Standard Unit Test Benchmark Reference Template.
 * Complies strictly with unit_testability_rule.md standards:
 *
 * 1. Constructor Dependency Injection: Production class accepts interface abstractions for easy mocking.
 * 2. AAA Pattern: Clear Arrange-Act-Assert structure in every test case.
 * 3. Mockito Integration: Uses @RunWith(MockitoJUnitRunner.class), @Mock, stubbing (when), and verification (verify).
 * 4. Exception & Boundary Testing: Validates both happy paths and edge/error conditions.
 */
@RunWith(MockitoJUnitRunner.class)
public class UnitTestTemplate {

    // --- 1. ABSTRACTIONS & DEPENDENCIES FOR DI ---

    public interface VhalGateway {
        float getVehicleSpeed();
        boolean isVhalConnected();
    }

    public interface TimeProvider {
        long getCurrentTimeMillis();
    }

    // --- 2. PRODUCTION CLASS UNDER TEST ---

    public static class VehicleTelemetryService {
        private final VhalGateway mGateway;
        private final TimeProvider mTimeProvider;

        public VehicleTelemetryService(VhalGateway gateway, TimeProvider timeProvider) {
            this.mGateway = Objects.requireNonNull(gateway, "VhalGateway cannot be null");
            this.mTimeProvider = Objects.requireNonNull(timeProvider, "TimeProvider cannot be null");
        }

        public Optional<Float> fetchCurrentSpeed() {
            if (!mGateway.isVhalConnected()) {
                return Optional.empty();
            }
            float speed = mGateway.getVehicleSpeed();
            if (speed < 0.0f) {
                throw new IllegalStateException("Invalid negative speed received from VHAL");
            }
            return Optional.of(speed);
        }

        public TimeProvider getTimeProvider() {
            return mTimeProvider;
        }
    }

    // --- 3. UNIT TEST SUITE (AAA Pattern) ---

    @Mock
    private VhalGateway mMockGateway;

    @Mock
    private TimeProvider mMockTimeProvider;

    private VehicleTelemetryService mService;

    @Before
    public void setUp() {
        mService = new VehicleTelemetryService(mMockGateway, mMockTimeProvider);
    }

    @Test
    public void fetchCurrentSpeed_whenVhalConnected_shouldReturnSpeed() {
        // 1. ARRANGE
        when(mMockGateway.isVhalConnected()).thenReturn(true);
        when(mMockGateway.getVehicleSpeed()).thenReturn(85.5f);

        // 2. ACT
        Optional<Float> speedOpt = mService.fetchCurrentSpeed();

        // 3. ASSERT
        assertTrue(speedOpt.isPresent());
        assertEquals(85.5f, speedOpt.get(), 0.001f);
        verify(mMockGateway, times(1)).isVhalConnected();
        verify(mMockGateway, times(1)).getVehicleSpeed();
        verifyNoMoreInteractions(mMockGateway);
    }

    @Test
    public void fetchCurrentSpeed_whenVhalDisconnected_shouldReturnEmptyOptional() {
        // 1. ARRANGE
        when(mMockGateway.isVhalConnected()).thenReturn(false);

        // 2. ACT
        Optional<Float> speedOpt = mService.fetchCurrentSpeed();

        // 3. ASSERT
        assertFalse(speedOpt.isPresent());
        verify(mMockGateway, times(1)).isVhalConnected();
        verify(mMockGateway, times(0)).getVehicleSpeed();
    }

    @Test
    public void fetchCurrentSpeed_whenSpeedNegative_shouldThrowIllegalStateException() {
        // 1. ARRANGE
        when(mMockGateway.isVhalConnected()).thenReturn(true);
        when(mMockGateway.getVehicleSpeed()).thenReturn(-10.0f);

        // 2. ACT & ASSERT
        assertThrows(IllegalStateException.class, () -> mService.fetchCurrentSpeed());
        verify(mMockGateway, times(1)).isVhalConnected();
        verify(mMockGateway, times(1)).getVehicleSpeed();
    }

    @Test
    public void constructor_givenNullGateway_shouldThrowNullPointerException() {
        // 1. ARRANGE & 2. ACT & 3. ASSERT
        assertThrows(NullPointerException.class, () -> new VehicleTelemetryService(null, mMockTimeProvider));
    }
}
