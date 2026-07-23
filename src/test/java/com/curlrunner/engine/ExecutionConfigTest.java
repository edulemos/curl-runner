package com.curlrunner.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionConfigTest {

    @Test
    void validConfig_minimalValues_noException() {
        ExecutionConfig config = new ExecutionConfig(1, 0, false);
        assertDoesNotThrow(config::validate);
    }

    @Test
    void validConfig_boundaryMaxValues_noException() {
        ExecutionConfig config = new ExecutionConfig(10000, 60000, false);
        assertDoesNotThrow(config::validate);
    }

    @Test
    void validConfig_parallelEnabled_noException() {
        ExecutionConfig config = new ExecutionConfig(5000, 500, true);
        assertDoesNotThrow(config::validate);
    }

    @Test
    void invalidIterations_zero_throwsIllegalArgumentException() {
        ExecutionConfig config = new ExecutionConfig(0, 0, false);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void invalidIterations_negative_throwsIllegalArgumentException() {
        ExecutionConfig config = new ExecutionConfig(-1, 0, false);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void invalidIterations_aboveMax_throwsIllegalArgumentException() {
        ExecutionConfig config = new ExecutionConfig(10001, 0, false);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void invalidDelayMs_negative_throwsIllegalArgumentException() {
        ExecutionConfig config = new ExecutionConfig(1, -1, false);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void invalidDelayMs_aboveMax_throwsIllegalArgumentException() {
        ExecutionConfig config = new ExecutionConfig(1, 60001, false);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    void defaultConstructor_hasValidDefaults() {
        ExecutionConfig config = new ExecutionConfig();
        assertDoesNotThrow(config::validate);
    }
}
