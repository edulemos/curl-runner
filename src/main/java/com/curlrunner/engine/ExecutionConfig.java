package com.curlrunner.engine;

public class ExecutionConfig {
    private int iterations;       // 1..10000
    private int delayMs;          // 0..60000
    private boolean parallel;     // false = sequential (default)

    /**
     * Default constructor with safe defaults.
     */
    public ExecutionConfig() {
        this.iterations = 1;
        this.delayMs = 0;
        this.parallel = false;
    }

    /**
     * Constructor with all fields.
     */
    public ExecutionConfig(int iterations, int delayMs, boolean parallel) {
        this.iterations = iterations;
        this.delayMs = delayMs;
        this.parallel = parallel;
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

    /**
     * Validates the execution configuration.
     *
     * @throws IllegalArgumentException if iterations is not between 1 and 10000,
     *                                  or if delayMs is not between 0 and 60000.
     */
    public void validate() throws IllegalArgumentException {
        if (iterations < 1 || iterations > 10000) {
            throw new IllegalArgumentException(
                "Número de iterações deve ser entre 1 e 10000. Valor: " + iterations);
        }
        if (delayMs < 0 || delayMs > 60000) {
            throw new IllegalArgumentException(
                "Delay deve ser entre 0 e 60000 milissegundos. Valor: " + delayMs);
        }
    }
}
