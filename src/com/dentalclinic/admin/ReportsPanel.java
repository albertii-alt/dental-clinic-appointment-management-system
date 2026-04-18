package com.dentalclinic.admin;

import com.dentalclinic.controller.ReportsController;
import com.dentalclinic.dto.report.ReportData;
import com.dentalclinic.dto.report.ReportRequest;
import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ReportsPanel extends JPanel {
    private final ReportsController reportsController = new ReportsController();
    private JComboBox<String> reportTypeCombo;
    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;
    private JTable previewTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private List<Object[]> currentData = new ArrayList<>();
    private String currentReportName = "";
    private String currentDateRange = "";
    private int currentTotalRecords = 0;

    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SUCCESS_GREEN = new Color(39, 174, 96);
    private final Color BG_LIGHT = new Color(245, 247, 250);
    private final Color TEXT_DARK = new Color(44, 62, 80);

    private final Map<String, String> reportTitles = new HashMap<>();
    private final Map<String, String> reportDescriptions = new HashMap<>();

    public ReportsPanel(int adminId, boolean isSuper) {
        initReportMetadata();
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_LIGHT);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setOpaque(false);

        JPanel controlPanel = createControlPanel();
        controlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        contentWrapper.add(controlPanel);
        contentWrapper.add(Box.createVerticalStrut(15));
        contentWrapper.add(createTablePanel());

        add(contentWrapper, BorderLayout.CENTER);
        loadReport("Patient Report");
    }

    private void initReportMetadata() {
        reportTitles.put("Patient Report", "PATIENT DIRECTORY REPORT");
        reportTitles.put("Appointment Report", "APPOINTMENT SCHEDULE REPORT");
        reportTitles.put("Pending Approvals Report", "PENDING APPROVALS REPORT");
        reportTitles.put("Completed Treatments Report", "COMPLETED TREATMENTS REPORT");
        reportTitles.put("Cancelled Appointments Report", "CANCELLED APPOINTMENTS REPORT");
        reportTitles.put("Service Popularity Report", "SERVICE POPULARITY REPORT");

        reportDescriptions.put("Patient Report", "Complete list of all registered patients with contact information");
        reportDescriptions.put("Appointment Report", "All appointments with patient details, service, date, time, and status");
        reportDescriptions.put("Pending Approvals Report", "Appointments waiting for staff approval");
        reportDescriptions.put("Completed Treatments Report", "History of all completed treatments and procedures");
        reportDescriptions.put("Cancelled Appointments Report", "Track cancelled and declined appointments");
        reportDescriptions.put("Service Popularity Report", "Analysis of service demand and completion rates");
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = new JLabel("Reports & Analytics");
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28));
        title.setForeground(TEXT_DARK);
        JLabel subtitle = new JLabel("Generate, preview, and export professional business reports");
        subtitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        panel.add(titlePanel, BorderLayout.WEST);
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(218, 226, 234), 1, true), new EmptyBorder(20, 25, 20, 25)));

        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        typePanel.setOpaque(false);
        JLabel typeLabel = new JLabel("Report Type:");
        typeLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        String[] reportTypes = {
            "Patient Report", "Appointment Report", "Pending Approvals Report",
            "Completed Treatments Report", "Cancelled Appointments Report", "Service Popularity Report"
        };
        reportTypeCombo = new JComboBox<>(reportTypes);
        reportTypeCombo.setPreferredSize(new Dimension(220, 35));
        reportTypeCombo.addActionListener(e -> loadReport((String) reportTypeCombo.getSelectedItem()));
        typePanel.add(typeLabel);
        typePanel.add(reportTypeCombo);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        datePanel.setOpaque(false);
        JLabel dateLabel = new JLabel("Date Range:");
        dateLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        startDateChooser = new JDateChooser();
        startDateChooser.setPreferredSize(new Dimension(130, 35));
        startDateChooser.setDateFormatString("yyyy-MM-dd");
        endDateChooser = new JDateChooser();
        endDateChooser.setPreferredSize(new Dimension(130, 35));
        endDateChooser.setDateFormatString("yyyy-MM-dd");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        endDateChooser.setDate(cal.getTime());
        cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
        startDateChooser.setDate(cal.getTime());
        datePanel.add(dateLabel);
        datePanel.add(startDateChooser);
        datePanel.add(new JLabel("to"));
        datePanel.add(endDateChooser);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        buttonPanel.setOpaque(false);
        JButton generateBtn = new JButton("Generate Report");
        styleButton(generateBtn, PRIMARY_BLUE);
        generateBtn.addActionListener(e -> loadReport((String) reportTypeCombo.getSelectedItem()));
        JButton exportBtn = new JButton("Export to Excel");
        styleButton(exportBtn, SUCCESS_GREEN);
        exportBtn.addActionListener(e -> exportToExcel());
        buttonPanel.add(generateBtn);
        buttonPanel.add(exportBtn);

        JPanel topPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        topPanel.setOpaque(false);
        topPanel.add(typePanel);
        topPanel.add(datePanel);
        topPanel.add(buttonPanel);
        card.add(topPanel, BorderLayout.NORTH);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.ITALIC, 11));
        statusLabel.setForeground(Color.GRAY);
        card.add(statusLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(new LineBorder(new Color(218, 226, 234), 1, true), new EmptyBorder(15, 15, 15, 15)));
        tableModel = new DefaultTableModel();
        previewTable = new JTable(tableModel);
        previewTable.setRowHeight(35);
        previewTable.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        previewTable.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        panel.add(new JScrollPane(previewTable), BorderLayout.CENTER);
        return panel;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void loadReport(String reportType) {
        statusLabel.setText("Loading " + reportType + "...");
        statusLabel.setForeground(PRIMARY_BLUE);

        SwingWorker<ReportData, Void> worker = new SwingWorker<ReportData, Void>() {
            @Override
            protected ReportData doInBackground() throws Exception {
                return reportsController.loadReport(new ReportRequest(reportType, startDateChooser.getDate(), endDateChooser.getDate()));
            }

            @Override
            protected void done() {
                try {
                    applyReportData(get());
                    statusLabel.setText("Ready - " + currentData.size() + " records");
                    statusLabel.setForeground(Color.GRAY);
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load report");
                    statusLabel.setForeground(new Color(192, 57, 43));
                    JOptionPane.showMessageDialog(ReportsPanel.this, "Unable to generate report: " + ex.getMessage(), "Report Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void applyReportData(ReportData reportData) {
        tableModel.setColumnIdentifiers(reportData.getColumns());
        tableModel.setRowCount(0);
        for (Object[] row : reportData.getRows()) {
            tableModel.addRow(row);
        }
        currentData = reportData.getRows();
        currentReportName = reportData.getReportName();
        currentDateRange = reportData.getDateRange();
        currentTotalRecords = reportData.getTotalRecords();
    }

    private void exportToExcel() {
        if (currentData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data to export. Generate a report first.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        String fileName = currentReportName.replace(" ", "_") + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".xlsx";
        fileChooser.setSelectedFile(new java.io.File(fileName));

        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File file = fileChooser.getSelectedFile();
        if (!file.getName().endsWith(".xlsx")) {
            file = new java.io.File(file.getAbsolutePath() + ".xlsx");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            String sheetName = currentReportName.length() > 31 ? currentReportName.substring(0, 31) : currentReportName;
            Sheet sheet = workbook.createSheet(sheetName);

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Font subtitleFont = workbook.createFont();
            subtitleFont.setItalic(true);
            subtitleFont.setFontHeightInPoints((short) 10);
            subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            CellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setFont(subtitleFont);
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(reportTitles.getOrDefault(currentReportName, currentReportName.toUpperCase()));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, tableModel.getColumnCount() - 1));

            Row descRow = sheet.createRow(1);
            Cell descCell = descRow.createCell(0);
            descCell.setCellValue(reportDescriptions.getOrDefault(currentReportName, "Dental Clinic Management Report"));
            descCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, tableModel.getColumnCount() - 1));

            Row infoRow = sheet.createRow(2);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(
                "Generated on: " + new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a").format(new java.util.Date()) +
                " | Date Range: " + currentDateRange + " | Total Records: " + currentTotalRecords
            );
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, tableModel.getColumnCount() - 1));

            sheet.createRow(3);
            Row headerRow = sheet.createRow(4);
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(tableModel.getColumnName(col));
                cell.setCellStyle(headerStyle);
            }

            int excelRow = 5;
            for (Object[] rowData : currentData) {
                Row row = sheet.createRow(excelRow++);
                for (int col = 0; col < rowData.length; col++) {
                    Cell cell = row.createCell(col);
                    Object value = rowData[col];
                    cell.setCellValue(value == null ? "" : value.toString());
                }
            }

            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                sheet.autoSizeColumn(col);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            JOptionPane.showMessageDialog(this, "Report exported successfully to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
