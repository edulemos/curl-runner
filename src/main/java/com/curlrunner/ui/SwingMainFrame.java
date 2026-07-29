package com.curlrunner.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.springframework.stereotype.Component;

import com.curlrunner.engine.AggregatedResults;
import com.curlrunner.engine.ExecutionConfig;
import com.curlrunner.engine.ExecutionEngineService;
import com.curlrunner.engine.ResultsAggregatorService;
import com.curlrunner.model.ExecutionRun;
import com.curlrunner.model.RequestResult;
import com.curlrunner.parser.CurlParseException;
import com.curlrunner.parser.CurlParserService;
import com.curlrunner.parser.ParsedRequest;
import com.curlrunner.repository.RequestResultRepository;

/**
 * Main application window for the Curl Runner.
 * Uses BorderLayout with:
 * - NORTH: Input panel (cURL command + controls)
 * - CENTER: Results panel (table + summary)
 * - SOUTH: Status panel (buttons + progress bar)
 */
@Component
public class SwingMainFrame extends JFrame {

    private JProgressBar progressBar;
    private JButton executeButton;
    private JButton cancelButton;
    private Long currentRunId;
    private final InputPanel inputPanel;
    private final CurlParserService curlParserService;
    private final ExecutionEngineService executionEngineService;
    private final ResultsAggregatorService resultsAggregatorService;
    private final ResultsPanel resultsPanel;
    private final RequestResultRepository requestResultRepository;

    public SwingMainFrame(InputPanel inputPanel,
                          CurlParserService curlParserService,
                          ExecutionEngineService executionEngineService,
                          ResultsAggregatorService resultsAggregatorService,
                          ResultsPanel resultsPanel,
                          RequestResultRepository requestResultRepository) {
        super("Curl Runner");
        this.inputPanel = inputPanel;
        this.curlParserService = curlParserService;
        this.executionEngineService = executionEngineService;
        this.resultsAggregatorService = resultsAggregatorService;
        this.resultsPanel = resultsPanel;
        this.requestResultRepository = requestResultRepository;
    }

    /**
     * Initializes the frame: sets size, layout, location, and makes it visible.
     * Should be called on the EDT after Spring context is ready.
     */
    public void initialize() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLayout(new BorderLayout());

        // Top section: Input panel (includes buttons)
        add(inputPanel, BorderLayout.NORTH);

        // Results panel in the center
        add(resultsPanel, BorderLayout.CENTER);

        // Progress bar at the bottom
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("0 / 0");
        progressBar.setVisible(false);
        add(progressBar, BorderLayout.SOUTH);

        // Get button references from InputPanel
        executeButton = inputPanel.getExecuteButton();
        cancelButton = inputPanel.getCancelButton();

        // Register Execute button ActionListener
        executeButton.addActionListener(e -> onExecuteClicked());

        // Register Cancel button ActionListener
        cancelButton.addActionListener(e -> {
            if (currentRunId != null) {
                executionEngineService.cancel(currentRunId);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Handles the Execute button click: validates input, parses cURL command,
     * then runs execution in a background thread to avoid blocking the EDT.
     */
    private void onExecuteClicked() {
        // 1. Get cURL command from InputPanel
        String curlCommand = inputPanel.getCurlCommand();
        if (curlCommand == null || curlCommand.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a cURL command.",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Get execution config from InputPanel
        ExecutionConfig config = new ExecutionConfig(
            inputPanel.getIterations(),
            inputPanel.getDelayMs(),
            inputPanel.isParallel()
        );

        // Set data list if loaded (data-driven execution)
        if (inputPanel.getLoadedData() != null) {
            config.setDataList(inputPanel.getLoadedData());
        }

        // 3. Validate the config
        try {
            config.validate();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                ex.getMessage(),
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Parse the cURL command
        ParsedRequest parsedRequest;
        try {
            parsedRequest = curlParserService.parse(curlCommand);
        } catch (CurlParseException ex) {
            JOptionPane.showMessageDialog(this,
                ex.getMessage(),
                "Parse Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 5. Set UI state to RUNNING
        setExecutionState(ExecutionState.RUNNING);
        resultsPanel.clear();

        // 6. Execute in a background thread (NOT the EDT)
        SwingProgressListener progressListener = new SwingProgressListener(this);

        Thread executionThread = new Thread(() -> {
            try {
                ExecutionRun run = executionEngineService.execute(parsedRequest, config, progressListener);
                currentRunId = run.getId();

                // 7. On completion, aggregate results and display them on the EDT
                Long runId = run.getId();
                AggregatedResults aggregatedResults = resultsAggregatorService.aggregate(runId);
                List<RequestResult> individualResults = requestResultRepository.findByRunIdOrderByIterationNumber(runId);

                SwingUtilities.invokeLater(() -> {
                    currentRunId = null;
                    resultsPanel.displayResults(aggregatedResults, individualResults);
                    setExecutionState(ExecutionState.COMPLETED);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    currentRunId = null;
                    JOptionPane.showMessageDialog(SwingMainFrame.this,
                        "Execution error: " + ex.getMessage(),
                        "Execution Error",
                        JOptionPane.ERROR_MESSAGE);
                    setExecutionState(ExecutionState.IDLE);
                });
            }
        }, "curl-execution-thread");
        executionThread.setDaemon(true);
        executionThread.start();
    }

    /**
     * Returns the Execute button for action listener registration.
     *
     * @return the execute button
     */
    public JButton getExecuteButton() {
        return executeButton;
    }

    /**
     * Returns the Cancel button for action listener registration.
     *
     * @return the cancel button
     */
    public JButton getCancelButton() {
        return cancelButton;
    }

    /**
     * Returns the InputPanel for accessing user input values.
     *
     * @return the input panel
     */
    public InputPanel getInputPanel() {
        return inputPanel;
    }

    /**
     * Displays aggregated execution results in the results panel.
     *
     * @param results the aggregated results to display
     * @param individualResults the individual request results for the table
     */
    public void displayResults(AggregatedResults results, List<RequestResult> individualResults) {
        resultsPanel.displayResults(results, individualResults);
    }

    /**
     * Updates the progress bar with current execution progress.
     *
     * @param completed number of completed iterations
     * @param total total number of iterations
     */
    public void updateProgress(int completed, int total) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setMaximum(total);
            progressBar.setValue(completed);
            progressBar.setString(completed + " / " + total);
            progressBar.setVisible(true);
        });
    }

    /**
     * Sets the execution state of the UI, enabling/disabling controls accordingly.
     * <p>
     * States:
     * <ul>
     *   <li>IDLE: Execute enabled, Cancel disabled, progress hidden, input editable</li>
     *   <li>RUNNING: Execute disabled, Cancel enabled, progress visible, input disabled</li>
     *   <li>COMPLETED: Execute enabled, Cancel disabled, progress visible (final state), input editable</li>
     * </ul>
     *
     * @param state the new execution state
     */
    public void setExecutionState(ExecutionState state) {
        SwingUtilities.invokeLater(() -> {
            switch (state) {
                case IDLE:
                    executeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    progressBar.setValue(0);
                    progressBar.setString("0 / 0");
                    progressBar.setVisible(false);
                    inputPanel.setInputEnabled(true);
                    break;
                case RUNNING:
                    executeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    progressBar.setVisible(true);
                    inputPanel.setInputEnabled(false);
                    break;
                case COMPLETED:
                    executeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    // Progress bar stays visible showing final state
                    inputPanel.setInputEnabled(true);
                    break;
            }
        });
    }

    /**
     * Shows an error dialog to the user. Thread-safe: can be called from any thread.
     * Used by SwingProgressListener to display execution errors on the EDT.
     *
     * @param message the error message to display
     */
    public void showError(String message) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }
}
