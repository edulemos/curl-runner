package com.curlrunner.engine;

import com.curlrunner.model.ExecutionRun;
import com.curlrunner.model.ExecutionStatus;
import com.curlrunner.parser.ParsedRequest;

public interface ExecutionEngineService {

    /**
     * Starts execution of HTTP requests based on the parsed request and configuration.
     * The execution runs asynchronously and notifies progress via the listener.
     */
    ExecutionRun execute(ParsedRequest request, ExecutionConfig config, ExecutionProgressListener listener);

    /**
     * Cancels an ongoing execution run.
     */
    void cancel(Long runId);

    /**
     * Returns the current status of an execution run.
     */
    ExecutionStatus getStatus(Long runId);
}
