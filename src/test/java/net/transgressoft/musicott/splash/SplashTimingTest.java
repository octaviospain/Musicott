package net.transgressoft.musicott.splash;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SplashTiming")
class SplashTimingTest {

    @Test
    @DisplayName("returns full minimum display time when elapsed is zero")
    void returnsFullMinimumDisplayTimeWhenElapsedIsZero() {
        assertThat(SplashOrchestrator.computeRemainingMillis(0)).isEqualTo(800);
    }

    @Test
    @DisplayName("returns the difference when elapsed is below the minimum")
    void returnsTheDifferenceWhenElapsedIsBelowTheMinimum() {
        assertThat(SplashOrchestrator.computeRemainingMillis(500)).isEqualTo(300);
    }

    @Test
    @DisplayName("returns zero when elapsed already exceeded the minimum")
    void returnsZeroWhenElapsedAlreadyExceededTheMinimum() {
        assertThat(SplashOrchestrator.computeRemainingMillis(1500)).isEqualTo(0);
    }

    @Test
    @DisplayName("clamps to zero exactly when elapsed equals the minimum")
    void clampsToZeroExactlyWhenElapsedEqualsTheMinimum() {
        assertThat(SplashOrchestrator.computeRemainingMillis(800)).isEqualTo(0);
    }
}
