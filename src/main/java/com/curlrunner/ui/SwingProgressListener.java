package com.curlrunner.ui;

import javax.swing.SwingUtilities;

import com.curlrunner.engine.ExecutionProgressListener;
import com.curlrunner.model.ExecutionRun;
import com.curlrunner.model.RequestResult;

/**
 * Progress listener that delegates UI updates to the EDT via SwingUtilities.invokeLater().
 * This class bridges between the execution engine (running on a background thread)
 * and the Swing UI (which must be updated on the Event Dispatch Thread).
 *
 * <p>This is NOT a Spring Component because it's created manually when an execution starts
 * (it needs a reference to the specific MainFrame instance). It's a simple adapter class.</p>
 */
public class SwingProgressListener implements ExecutionProgressListener {

    private final SwingMainFrame mainFrame;

    public SwingProgressListener(SwingMainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void onProgress(int completed, int total) {
        SwingUtilities.invokeLater(() -> mainFrame.updateProgress(completed, total));
    }

    @Override
    public void onIterationComplete(RequestResult result) {
        // Could add per-iteration UI updates here if needed
    }

    @Override
    public void onExecutionComplete(ExecutionRun run) {
        SwingUtilities.invokeLater(() -> mainFrame.setExecutionState(ExecutionState.COMPLETED));
    }

    @Override
    public void onError(String message) {
        SwingUtilities.invokeLater(() -> {
            mainFrame.showError(message);
            mainFrame.setExecutionState(ExecutionState.IDLE);
        });
    }
}
