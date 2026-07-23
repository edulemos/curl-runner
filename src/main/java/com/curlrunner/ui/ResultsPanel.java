package com.curlrunner.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.springframework.stereotype.Component;

import com.curlrunner.engine.AggregatedResults;
import com.curlrunner.model.RequestResult;

/**
 * Panel that displays execution results in two sections:
 * - Summary panel (top): aggregated metrics
 * - Results table (center): individual request results in a JTable
 */
@Component
public class ResultsPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String[] COLUMN_NAMES = {
        "#", "Status Code", "Response Time (ms)", "Success", "Error", "Size (bytes)", "Executed At"
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

    public ResultsPanel() {
        setLayout(new BorderLayout());

        // Summary panel
        JPanel summaryPanel = new JPanel(new GridLayout(4, 3, 10, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Resumo"));

        totalRequestsLabel = new JLabel("Total: -");
        successCountLabel = new JLabel("Sucesso: -");
        failureCountLabel = new JLabel("Falha: -");
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
        JScrollPane tableScrollPane = new JScrollPane(resultsTable);

        add(summaryPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    /**
     * Populates both summary labels and table rows with execution results.
     *
     * @param results the aggregated results to display in the summary
     * @param individualResults the list of individual request results for the table
     */
    public void displayResults(AggregatedResults results, List<RequestResult> individualResults) {
        // Update summary labels
        totalRequestsLabel.setText("Total: " + results.getTotalRequests());
        successCountLabel.setText("Sucesso: " + results.getSuccessCount());
        failureCountLabel.setText("Falha: " + results.getFailureCount());
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
            Object[] row = {
                result.getIterationNumber(),
                result.getStatusCode() != null ? result.getStatusCode() : "-",
                result.getResponseTimeMs(),
                result.getSuccess() != null && result.getSuccess() ? "Yes" : "No",
                result.getErrorMessage() != null ? result.getErrorMessage() : "",
                result.getResponseSizeBytes() != null ? result.getResponseSizeBytes() : "-",
                result.getExecutedAt() != null ? result.getExecutedAt().format(DATE_FORMATTER) : "-"
            };
            tableModel.addRow(row);
        }
    }

    /**
     * Clears the table and resets summary labels to default values.
     */
    public void clear() {
        tableModel.setRowCount(0);
        totalRequestsLabel.setText("Total: -");
        successCountLabel.setText("Sucesso: -");
        failureCountLabel.setText("Falha: -");
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
