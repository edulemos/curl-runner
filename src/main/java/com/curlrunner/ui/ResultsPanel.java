package com.curlrunner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;

import org.springframework.stereotype.Component;

import com.curlrunner.engine.AggregatedResults;
import com.curlrunner.model.RequestResult;

/**
 * Panel that displays execution results in two sections:
 * - Summary panel (top): aggregated metrics
 * - Results table + response viewer (center): split pane
 */
@Component
public class ResultsPanel extends JPanel {

    private static final String[] COLUMN_NAMES = {
        "#", "Status Code", "Response Time (ms)", "Request", "Response"
    };

    // Summary labels
    private final JLabel totalRequestsLabel;
    private final JLabel successCountLabel;
    private final JLabel failureCountLabel;
    private final JLabel avgResponseTimeLabel;
    private final JLabel minResponseTimeLabel;
    private final JLabel maxResponseTimeLabel;
    // Results table
    private final DefaultTableModel tableModel;
    private final JTable resultsTable;

    // Response body viewer
    private final JTextArea responseViewer;

    // Store results for response body access
    private List<RequestResult> currentResults;

    public ResultsPanel() {
        setLayout(new BorderLayout());

        // Summary panel
        JPanel summaryPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Summary"));

        totalRequestsLabel = new JLabel("Total: -");
        successCountLabel = new JLabel("Success: -");
        failureCountLabel = new JLabel("Failure: -");
        avgResponseTimeLabel = new JLabel("Avg: - ms");
        minResponseTimeLabel = new JLabel("Min: - ms");
        maxResponseTimeLabel = new JLabel("Max: - ms");

        summaryPanel.add(totalRequestsLabel);
        summaryPanel.add(successCountLabel);
        summaryPanel.add(failureCountLabel);
        summaryPanel.add(avgResponseTimeLabel);
        summaryPanel.add(minResponseTimeLabel);
        summaryPanel.add(maxResponseTimeLabel);

        // Results table
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setColumnSelectionAllowed(true);
        resultsTable.getSelectionModel().addListSelectionListener(this::onRowSelected);
        resultsTable.getColumnModel().getSelectionModel().addListSelectionListener(this::onRowSelected);
        JScrollPane tableScrollPane = new JScrollPane(resultsTable);


        // Response body viewer — dark theme, monospaced, read-only
        responseViewer = new JTextArea();
        responseViewer.setEditable(false);
        responseViewer.setFont(new Font("Consolas", Font.PLAIN, 12));
        responseViewer.setBackground(new Color(40, 40, 40));
        responseViewer.setForeground(new Color(200, 220, 200));
        responseViewer.setCaretColor(Color.WHITE);
        responseViewer.setLineWrap(true);
        responseViewer.setWrapStyleWord(true);
        responseViewer.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        responseViewer.setText("Select a request or response to view its full content");
        JScrollPane responseScrollPane = new JScrollPane(responseViewer);
        responseScrollPane.setBorder(BorderFactory.createTitledBorder("Request / Response Body"));

        // Split pane: table on top, response viewer on bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, responseScrollPane);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(200);

        add(summaryPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void onRowSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        int selectedRow = resultsTable.getSelectedRow();
        int selectedColumn = resultsTable.getSelectedColumn();
        if (selectedRow >= 0 && currentResults != null && selectedRow < currentResults.size()) {
            RequestResult result = currentResults.get(selectedRow);
            if (selectedColumn == 3) {
                responseViewer.setText(formatRequest(result));
                responseViewer.setCaretPosition(0);
                return;
            }
            String body = result.getResponseBody();
            if (body != null && !body.isEmpty()) {
                responseViewer.setText(formatJson(body));
                responseViewer.setCaretPosition(0);
            } else if (result.getErrorMessage() != null) {
                responseViewer.setText("ERROR: " + result.getErrorMessage());
            } else {
                responseViewer.setText("(no content)");
            }
        }
    }

    private String formatRequest(RequestResult result) {
        StringBuilder request = new StringBuilder();
        request.append(result.getRequestMethod() != null ? result.getRequestMethod() : "REQUEST");
        if (result.getRequestUrl() != null) {
            request.append(" ").append(result.getRequestUrl());
        }
        if (result.getRequestHeaders() != null && !result.getRequestHeaders().isBlank()) {
            request.append("\n\nHeaders:\n").append(formatJson(result.getRequestHeaders()));
        }
        if (result.getRequestBody() != null && !result.getRequestBody().isBlank()) {
            request.append("\n\nBody:\n").append(formatJson(result.getRequestBody()));
        }
        return request.toString();
    }

    /**
     * Attempts to pretty-print JSON content. If it fails, returns as-is.
     */
    private String formatJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Object parsed = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            // Not JSON or malformed — return raw content
            return json;
        }
    }

    /**
     * Populates both summary labels and table rows with execution results.
     */
    public void displayResults(AggregatedResults results, List<RequestResult> individualResults) {
        this.currentResults = individualResults;

        // Update summary labels
        totalRequestsLabel.setText("Total: " + results.getTotalRequests());
        successCountLabel.setText("Success: " + results.getSuccessCount());
        failureCountLabel.setText("Failure: " + results.getFailureCount());
        avgResponseTimeLabel.setText("Avg: " + results.getAvgResponseTimeMs() + " ms");
        minResponseTimeLabel.setText("Min: " + results.getMinResponseTimeMs() + " ms");
        maxResponseTimeLabel.setText("Max: " + results.getMaxResponseTimeMs() + " ms");

        // Populate table
        tableModel.setRowCount(0);
        for (RequestResult result : individualResults) {
            String requestPreview = buildRequestPreview(result);
            String responsePreview = "";
            if (result.getResponseBody() != null) {
                responsePreview = result.getResponseBody().length() > 80
                    ? result.getResponseBody().substring(0, 80) + "..."
                    : result.getResponseBody();
            }

            Object[] row = {
                result.getIterationNumber(),
                result.getStatusCode() != null ? result.getStatusCode() : "-",
                result.getResponseTimeMs(),
                requestPreview,
                responsePreview
            };
            tableModel.addRow(row);
        }

        responseViewer.setText("Select a request or response to view its full content");
    }

    private String buildRequestPreview(RequestResult result) {
        String preview = result.getRequestBody();
        if (preview == null || preview.isBlank()) {
            preview = (result.getRequestMethod() != null ? result.getRequestMethod() + " " : "")
                + (result.getRequestUrl() != null ? result.getRequestUrl() : "");
        }
        return preview.length() > 80 ? preview.substring(0, 80) + "..." : preview;
    }

    /**
     * Clears the table and resets summary labels to default values.
     */
    public void clear() {
        currentResults = null;
        tableModel.setRowCount(0);
        responseViewer.setText("Select a request or response to view its full content");
        totalRequestsLabel.setText("Total: -");
        successCountLabel.setText("Success: -");
        failureCountLabel.setText("Failure: -");
        avgResponseTimeLabel.setText("Avg: - ms");
        minResponseTimeLabel.setText("Min: - ms");
        maxResponseTimeLabel.setText("Max: - ms");
    }

}
