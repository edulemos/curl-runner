package com.curlrunner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

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
        "#", "Status Code", "Response Time (ms)", "Response"
    };

    // Summary labels
    private final JLabel totalRequestsLabel;
    private final JLabel successCountLabel;
    private final JLabel failureCountLabel;
    private final JLabel avgResponseTimeLabel;
    private final JLabel minResponseTimeLabel;
    private final JLabel maxResponseTimeLabel;
    private final JLabel p50Label;
    private final JLabel p95Label;
    private final JLabel p99Label;
    private final JLabel requestsPerSecondLabel;
    private final JLabel statusCodeDistributionLabel;

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
        JPanel summaryPanel = new JPanel(new GridLayout(4, 3, 10, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Summary"));

        totalRequestsLabel = new JLabel("Total: -");
        successCountLabel = new JLabel("Success: -");
        failureCountLabel = new JLabel("Failure: -");
        avgResponseTimeLabel = new JLabel("Avg: - ms");
        minResponseTimeLabel = new JLabel("Min: - ms");
        maxResponseTimeLabel = new JLabel("Max: - ms");
        p50Label = new JLabel("P50: - ms");
        p95Label = new JLabel("P95: - ms");
        p99Label = new JLabel("P99: - ms");
        requestsPerSecondLabel = new JLabel("Req/s: -");
        statusCodeDistributionLabel = new JLabel("Status Codes: -");
        JLabel emptyLabel = new JLabel("");

        summaryPanel.add(totalRequestsLabel);
        summaryPanel.add(successCountLabel);
        summaryPanel.add(failureCountLabel);
        summaryPanel.add(avgResponseTimeLabel);
        summaryPanel.add(minResponseTimeLabel);
        summaryPanel.add(maxResponseTimeLabel);
        summaryPanel.add(p50Label);
        summaryPanel.add(p95Label);
        summaryPanel.add(p99Label);
        summaryPanel.add(requestsPerSecondLabel);
        summaryPanel.add(statusCodeDistributionLabel);
        summaryPanel.add(emptyLabel);

        // Results table
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.getSelectionModel().addListSelectionListener(this::onRowSelected);
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
        responseViewer.setText("Select a row to view the full response");
        JScrollPane responseScrollPane = new JScrollPane(responseViewer);
        responseScrollPane.setBorder(BorderFactory.createTitledBorder("Response Body"));

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
        if (selectedRow >= 0 && currentResults != null && selectedRow < currentResults.size()) {
            RequestResult result = currentResults.get(selectedRow);
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
        p50Label.setText("P50: " + results.getP50ResponseTimeMs() + " ms");
        p95Label.setText("P95: " + results.getP95ResponseTimeMs() + " ms");
        p99Label.setText("P99: " + results.getP99ResponseTimeMs() + " ms");
        requestsPerSecondLabel.setText(String.format("Req/s: %.2f", results.getRequestsPerSecond()));
        statusCodeDistributionLabel.setText("Status Codes: " + formatStatusCodeDistribution(results.getStatusCodeDistribution()));

        // Populate table
        tableModel.setRowCount(0);
        for (RequestResult result : individualResults) {
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
                responsePreview
            };
            tableModel.addRow(row);
        }

        responseViewer.setText("Select a row to view the full response");
    }

    /**
     * Clears the table and resets summary labels to default values.
     */
    public void clear() {
        currentResults = null;
        tableModel.setRowCount(0);
        responseViewer.setText("Select a row to view the full response");
        totalRequestsLabel.setText("Total: -");
        successCountLabel.setText("Success: -");
        failureCountLabel.setText("Failure: -");
        avgResponseTimeLabel.setText("Avg: - ms");
        minResponseTimeLabel.setText("Min: - ms");
        maxResponseTimeLabel.setText("Max: - ms");
        p50Label.setText("P50: - ms");
        p95Label.setText("P95: - ms");
        p99Label.setText("P99: - ms");
        requestsPerSecondLabel.setText("Req/s: -");
        statusCodeDistributionLabel.setText("Status Codes: -");
    }

    private String formatStatusCodeDistribution(Map<Integer, Integer> distribution) {
        if (distribution == null || distribution.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        distribution.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            });
        return sb.toString();
    }
}
