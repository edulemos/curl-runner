package com.curlrunner.engine;

import java.util.HashMap;
import java.util.Map;

public class AggregatedResults {

    private Long runId;
    private int totalRequests;
    private int successCount;
    private int failureCount;
    private long avgResponseTimeMs;
    private long minResponseTimeMs;
    private long maxResponseTimeMs;
    private long p50ResponseTimeMs;
    private long p95ResponseTimeMs;
    private long p99ResponseTimeMs;
    private Map<Integer, Integer> statusCodeDistribution;
    private double requestsPerSecond;
    private long totalDurationMs;

    public AggregatedResults() {
        this.statusCodeDistribution = new HashMap<>();
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public long getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }

    public void setAvgResponseTimeMs(long avgResponseTimeMs) {
        this.avgResponseTimeMs = avgResponseTimeMs;
    }

    public long getMinResponseTimeMs() {
        return minResponseTimeMs;
    }

    public void setMinResponseTimeMs(long minResponseTimeMs) {
        this.minResponseTimeMs = minResponseTimeMs;
    }

    public long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    public void setMaxResponseTimeMs(long maxResponseTimeMs) {
        this.maxResponseTimeMs = maxResponseTimeMs;
    }

    public long getP50ResponseTimeMs() {
        return p50ResponseTimeMs;
    }

    public void setP50ResponseTimeMs(long p50ResponseTimeMs) {
        this.p50ResponseTimeMs = p50ResponseTimeMs;
    }

    public long getP95ResponseTimeMs() {
        return p95ResponseTimeMs;
    }

    public void setP95ResponseTimeMs(long p95ResponseTimeMs) {
        this.p95ResponseTimeMs = p95ResponseTimeMs;
    }

    public long getP99ResponseTimeMs() {
        return p99ResponseTimeMs;
    }

    public void setP99ResponseTimeMs(long p99ResponseTimeMs) {
        this.p99ResponseTimeMs = p99ResponseTimeMs;
    }

    public Map<Integer, Integer> getStatusCodeDistribution() {
        return statusCodeDistribution;
    }

    public void setStatusCodeDistribution(Map<Integer, Integer> statusCodeDistribution) {
        this.statusCodeDistribution = statusCodeDistribution;
    }

    public double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(double requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }
}
