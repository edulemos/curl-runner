package com.curlrunner.ui;

/**
 * Represents the UI state of the application.
 * Used to control which buttons and panels are enabled/visible.
 */
public enum ExecutionState {
    IDLE,       // Ready to execute: Execute button enabled, Cancel disabled
    RUNNING,    // Execution in progress: Execute disabled, Cancel enabled, progress visible
    COMPLETED   // Execution finished: Execute enabled, Cancel disabled, results visible
}
