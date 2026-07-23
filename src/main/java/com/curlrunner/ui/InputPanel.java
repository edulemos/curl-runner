package com.curlrunner.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import org.springframework.stereotype.Component;

/**
 * Input panel containing the cURL command text area and execution controls.
 * Provides fields for:
 * - cURL command (multi-line text area)
 * - Number of iterations (spinner)
 * - Delay between requests in ms (spinner)
 * - Parallel execution toggle (checkbox)
 */
@Component
public class InputPanel extends JPanel {

    private final JTextArea curlTextArea;
    private final JSpinner iterationsSpinner;
    private final JSpinner delaySpinner;
    private final JCheckBox parallelCheckBox;

    public InputPanel() {
        setLayout(new BorderLayout());

        // cURL command text area
        curlTextArea = new JTextArea(5, 60);
        curlTextArea.setLineWrap(true);
        curlTextArea.setWrapStyleWord(true);
        curlTextArea.setToolTipText("Cole seu comando cURL aqui...");
        JScrollPane scrollPane = new JScrollPane(curlTextArea);

        // Iterations spinner: default 1, min 1, max 10000, step 1
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));

        // Delay spinner: default 0, min 0, max 60000, step 100
        delaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60000, 100));

        // Parallel checkbox: default unchecked
        parallelCheckBox = new JCheckBox("Paralelo", false);

        // Controls panel with FlowLayout
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlsPanel.add(new JLabel("Iterações:"));
        controlsPanel.add(iterationsSpinner);
        controlsPanel.add(new JLabel("Delay (ms):"));
        controlsPanel.add(delaySpinner);
        controlsPanel.add(parallelCheckBox);

        add(scrollPane, BorderLayout.CENTER);
        add(controlsPanel, BorderLayout.SOUTH);
    }

    /**
     * Returns the cURL command text entered by the user.
     *
     * @return the cURL command string
     */
    public String getCurlCommand() {
        return curlTextArea.getText();
    }

    /**
     * Returns the number of iterations configured.
     *
     * @return iteration count (1 to 10000)
     */
    public int getIterations() {
        return (int) iterationsSpinner.getValue();
    }

    /**
     * Returns the delay in milliseconds between requests.
     *
     * @return delay in ms (0 to 60000)
     */
    public int getDelayMs() {
        return (int) delaySpinner.getValue();
    }

    /**
     * Returns whether parallel execution is enabled.
     *
     * @return true if parallel, false if sequential
     */
    public boolean isParallel() {
        return parallelCheckBox.isSelected();
    }

    /**
     * Enables or disables all input controls in this panel.
     * Used to prevent user modifications during execution.
     *
     * @param enabled true to enable controls, false to disable them
     */
    public void setInputEnabled(boolean enabled) {
        curlTextArea.setEnabled(enabled);
        curlTextArea.setEditable(enabled);
        iterationsSpinner.setEnabled(enabled);
        delaySpinner.setEnabled(enabled);
        parallelCheckBox.setEnabled(enabled);
    }
}
