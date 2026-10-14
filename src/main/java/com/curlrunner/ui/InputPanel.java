package com.curlrunner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Input panel containing the cURL command text area and execution controls.
 */
@Component
public class InputPanel extends JPanel {

    private final JTextArea curlTextArea;
    private final JSpinner iterationsSpinner;
    private final JSpinner delaySpinner;
    private final JCheckBox parallelCheckBox;
    private final JButton loadJsonButton;
    private final JButton clearJsonButton;
    private final JLabel jsonStatusLabel;
    private final JButton exportButton;
    private final JButton executeButton;
    private final JButton cancelButton;
    private List<Map<String, Object>> loadedData;

    public InputPanel() {
        setLayout(new BorderLayout());

        // Label for the cURL field
        JLabel curlLabel = new JLabel("  cURL Command:");
        curlLabel.setFont(curlLabel.getFont().deriveFont(Font.BOLD, 12f));
        curlLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        // cURL command text area — larger, with monospaced font and dark background
        curlTextArea = new JTextArea(8, 60);
        curlTextArea.setLineWrap(true);
        curlTextArea.setWrapStyleWord(true);
        curlTextArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        curlTextArea.setBackground(new Color(45, 45, 45));
        curlTextArea.setForeground(new Color(220, 220, 220));
        curlTextArea.setCaretColor(Color.WHITE);
        curlTextArea.setToolTipText("Paste your cURL command here. Use {{variable}} for dynamic data.");
        curlTextArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane scrollPane = new JScrollPane(curlTextArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(2, 4, 2, 4),
            BorderFactory.createLineBorder(new Color(100, 100, 100))
        ));

        // Curl area panel with label on top
        JPanel curlPanel = new JPanel(new BorderLayout());
        curlPanel.add(curlLabel, BorderLayout.NORTH);
        curlPanel.add(scrollPane, BorderLayout.CENTER);

        // Iterations spinner: default 1, min 1, max 10000, step 1
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));

        // Delay spinner: default 0, min 0, max 60000, step 100
        delaySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60000, 100));

        // Parallel checkbox: default unchecked
        parallelCheckBox = new JCheckBox("Parallel", false);

        // JSON data upload controls
        loadJsonButton = new JButton("Load JSON");
        clearJsonButton = new JButton("Clear");
        clearJsonButton.setEnabled(false);
        jsonStatusLabel = new JLabel("No data loaded");

        loadJsonButton.addActionListener(e -> onLoadJson());
        clearJsonButton.addActionListener(e -> onClearJson());

        // Controls panel with FlowLayout
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlsPanel.add(new JLabel("Iterations:"));
        controlsPanel.add(iterationsSpinner);
        controlsPanel.add(new JLabel("Delay (ms):"));
        controlsPanel.add(delaySpinner);
        controlsPanel.add(parallelCheckBox);

        // JSON panel (left side) + Execute/Cancel buttons (right side)
        JPanel jsonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        jsonPanel.add(loadJsonButton);
        jsonPanel.add(clearJsonButton);
        jsonPanel.add(jsonStatusLabel);

        // Execute and Cancel buttons (right-aligned)
        exportButton = new JButton("Export results");
        exportButton.setEnabled(false);
        executeButton = new JButton("Run");
        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(false);
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actionButtonsPanel.add(exportButton);
        actionButtonsPanel.add(executeButton);
        actionButtonsPanel.add(cancelButton);

        // Combined row: JSON on left, action buttons on right
        JPanel jsonAndButtonsRow = new JPanel(new BorderLayout());
        jsonAndButtonsRow.add(jsonPanel, BorderLayout.WEST);
        jsonAndButtonsRow.add(actionButtonsPanel, BorderLayout.EAST);

        // Bottom section: controls + json/buttons row
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlsPanel, BorderLayout.NORTH);
        bottomPanel.add(jsonAndButtonsRow, BorderLayout.SOUTH);

        add(curlPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void onLoadJson() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        fileChooser.setDialogTitle("Select JSON data file");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String content = Files.readString(file.toPath());
                ObjectMapper mapper = new ObjectMapper();
                loadedData = mapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});

                if (loadedData.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "The JSON file is empty or does not contain an array of objects.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                    loadedData = null;
                    return;
                }

                jsonStatusLabel.setText("✓ " + loadedData.size() + " records loaded (" + file.getName() + ")");
                clearJsonButton.setEnabled(true);
                // When data is loaded, iterations are driven by data size
                iterationsSpinner.setValue(loadedData.size());
                iterationsSpinner.setEnabled(false);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error reading JSON file: " + ex.getMessage() +
                    "\n\nThe file must contain an array of objects, e.g.:\n" +
                    "[{\"name\": \"Carlos\"}, {\"name\": \"Daniel\"}]",
                    "Read Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onClearJson() {
        loadedData = null;
        jsonStatusLabel.setText("No data loaded");
        clearJsonButton.setEnabled(false);
        iterationsSpinner.setEnabled(true);
        iterationsSpinner.setValue(1);
    }

    public String getCurlCommand() {
        return curlTextArea.getText();
    }

    public int getIterations() {
        return (int) iterationsSpinner.getValue();
    }

    public int getDelayMs() {
        return (int) delaySpinner.getValue();
    }

    public boolean isParallel() {
        return parallelCheckBox.isSelected();
    }

    /**
     * Returns the loaded JSON data list, or null if no data is loaded.
     */
    public List<Map<String, Object>> getLoadedData() {
        return loadedData;
    }

    public void setInputEnabled(boolean enabled) {
        curlTextArea.setEnabled(enabled);
        curlTextArea.setEditable(enabled);
        iterationsSpinner.setEnabled(enabled && loadedData == null);
        delaySpinner.setEnabled(enabled);
        parallelCheckBox.setEnabled(enabled);
        loadJsonButton.setEnabled(enabled);
        clearJsonButton.setEnabled(enabled && loadedData != null);
        executeButton.setEnabled(enabled);
    }

    public JButton getExecuteButton() {
        return executeButton;
    }

    public JButton getExportButton() {
        return exportButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}
