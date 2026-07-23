package com.curlrunner.engine;

import com.curlrunner.model.ExecutionRun;
import com.curlrunner.model.RequestResult;

/**
 * Listener interface for execution progress notifications.
 * Used to update the UI with real-time progress information.
 */
public interface ExecutionProgressListener {

    /**
     * Called when overall progress changes.
     * @param completed number of iterations completed
     * @param total total number of iterations
     */
    void onProgress(int completed, int total);

    /**
     * Called when a single iteration completes.
     * @param result the result of the completed iteration
     */
    void onIterationComplete(RequestResult result);

    /**
     * Called when the entire execution run completes.
     * @param run the completed ExecutionRun
     */
    void onExecutionComplete(ExecutionRun run);

    /**
     * Called when an error occurs during execution.
     * @param message the error message
     */
    void onError(String message);
}
