package com.curlrunner.engine;

import java.util.List;
import java.util.Map;

public class ExecutionConfig {
    private int iterations;       // 1..10000
    private int delayMs;          // 0..60000
    private boolean parallel;     // false = sequential (default)
    private List<Map<String, Object>> dataList;  // JSON data rows for variable substitution

    /**
     * Default constructor with safe defaults.
     */
    public ExecutionConfig() {
        this.iterations = 1;
        this.delayMs = 0;
        this.parallel = false;
        this.dataList = null;
    }

    /**
     * Constructor with all fields.
     */
    public ExecutionConfig(int iterations, int delayMs, boolean parallel) {
        this.iterations = iterations;
        this.delayMs = delayMs;
        this.parallel = parallel;
        this.dataList = null;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public int getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public List<Map<String, Object>> getDataList() {
        return dataList;
    }

    public void setDataList(List<Map<String, Object>> dataList) {
        this.dataList = dataList;
    }

    /**
     * Returns true if this config has data-driven execution enabled.
     */
    public boolean isDataDriven() {
        return dataList != null && !dataList.isEmpty();
    }

    /**
     * Returns the effective number of iterations.
     * If data-driven, returns the size of the data list.
     * Otherwise returns the configured iterations value.
     */
    public int getEffectiveIterations() {
        if (isDataDriven()) {
            return dataList.size();
        }
        return iterations;
    }

    /**
     * Validates the execution configuration.
     *
     * @throws IllegalArgumentException if iterations is not between 1 and 10000,
     *                                  or if delayMs is not between 0 and 60000.
     */
    public void validate() throws IllegalArgumentException {
        if (iterations < 1 || iterations > 10000) {
            throw new IllegalArgumentException(
                "Number of iterations must be between 1 and 10000. Value: " + iterations);
        }
        if (delayMs < 0 || delayMs > 60000) {
            throw new IllegalArgumentException(
                "Delay must be between 0 and 60000 milliseconds. Value: " + delayMs);
        }
    }
}
