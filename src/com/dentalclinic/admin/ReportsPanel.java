package com.dentalclinic.admin;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import com.dentalclinic.util.DBConnection;
import com.dentalclinic.service.LogService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class ReportsPanel extends JPanel {
    
    private JComboBox<String> reportTypeCombo;
    private JDateChooser startDateChooser, endDateChooser;
    private JButton generateBtn, exportBtn;
    private JTable previewTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    
    private int currentAdminId;
    private boolean isSuperAdmin;
    private LogService logService = new LogService();
    private List<Object[]> currentData = new ArrayList<>();
    private String currentReportName = "";
    private String currentDateRange = "";
    private int currentTotalRecords = 0;
    
    private final java.awt.Color PRIMARY_BLUE = new java.awt.Color(41, 128, 185);
    private final java.awt.Color SUCCESS_GREEN = new java.awt.Color(39, 174, 96);
    private final java.awt.Color BG_LIGHT = new java.awt.Color(245, 247, 250);
    private final java.awt.Color TEXT_DARK = new java.awt.Color(44, 62, 80);
    
    // Report title mapping
    private final Map<String, String> reportTitles = new HashMap<>();
    private final Map<String, String> reportDescriptions = new HashMap<>();
    
    public ReportsPanel(int adminId, boolean isSuper) {
        this.currentAdminId = adminId;
        this.isSuperAdmin = isSuper;

        initReportMetadata();

        // Main layout uses BorderLayout for the high-level structure
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_LIGHT);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header Panel stays at NORTH
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // FIX: Create a wrapper to hold the Control Panel and Table Panel together
        // This ensures they stack vertically and don't push each other off-screen
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setOpaque(false);

        JPanel controlPanel = createControlPanel();
        // Cap the control panel height so it doesn't take up the whole screen
        controlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        JPanel tablePanel = createTablePanel();

        // Add them to the wrapper with a small gap
        contentWrapper.add(controlPanel);
        contentWrapper.add(Box.createVerticalStrut(15)); 
        contentWrapper.add(tablePanel);

        // Add the wrapper to the CENTER so it takes up all available remaining space
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
        subtitle.setForeground(java.awt.Color.GRAY);
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        
        panel.add(titlePanel, BorderLayout.WEST);
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(java.awt.Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new java.awt.Color(218, 226, 234), 1, true),
            new EmptyBorder(20, 25, 20, 25)
        ));
        
        // Report Type Selection
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        typePanel.setOpaque(false);
        
        JLabel typeLabel = new JLabel("Report Type:");
        typeLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        
        String[] reportTypes = {
            "Patient Report", 
            "Appointment Report", 
            "Pending Approvals Report",
            "Completed Treatments Report",
            "Cancelled Appointments Report",
            "Service Popularity Report"
        };
        reportTypeCombo = new JComboBox<>(reportTypes);
        reportTypeCombo.setPreferredSize(new Dimension(220, 35));
        reportTypeCombo.addActionListener(e -> loadReport((String) reportTypeCombo.getSelectedItem()));
        
        typePanel.add(typeLabel);
        typePanel.add(reportTypeCombo);
        
        // Date Range Panel
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        datePanel.setOpaque(false);
        
        JLabel dateLabel = new JLabel("Date Range:");
        dateLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        
        startDateChooser = new JDateChooser();
        startDateChooser.setPreferredSize(new Dimension(130, 35));
        startDateChooser.setDateFormatString("yyyy-MM-dd");
        
        JLabel toLabel = new JLabel("to");
        
        endDateChooser = new JDateChooser();
        endDateChooser.setPreferredSize(new Dimension(130, 35));
        endDateChooser.setDateFormatString("yyyy-MM-dd");
        
        // Set default dates (last 30 days)
        Calendar cal = Calendar.getInstance();
        endDateChooser.setDate(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, -30);
        startDateChooser.setDate(cal.getTime());
        
        datePanel.add(dateLabel);
        datePanel.add(startDateChooser);
        datePanel.add(toLabel);
        datePanel.add(endDateChooser);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        buttonPanel.setOpaque(false);
        
        generateBtn = new JButton("Generate Report");
        styleButton(generateBtn, PRIMARY_BLUE);
        generateBtn.addActionListener(e -> loadReport((String) reportTypeCombo.getSelectedItem()));
        
        exportBtn = new JButton("Export to Excel");
        styleButton(exportBtn, SUCCESS_GREEN);
        exportBtn.addActionListener(e -> exportToExcel());
        
        buttonPanel.add(generateBtn);
        buttonPanel.add(exportBtn);
        
        // Combine panels
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        topPanel.setOpaque(false);
        topPanel.add(typePanel);
        topPanel.add(datePanel);
        topPanel.add(buttonPanel);
        
        card.add(topPanel, BorderLayout.NORTH);
        
        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.ITALIC, 11));
        statusLabel.setForeground(java.awt.Color.GRAY);
        card.add(statusLabel, BorderLayout.SOUTH);
        
        return card;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(java.awt.Color.WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(new java.awt.Color(218, 226, 234), 1, true),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        tableModel = new DefaultTableModel();
        previewTable = new JTable(tableModel);
        previewTable.setRowHeight(35);
        previewTable.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        previewTable.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        
        JScrollPane scrollPane = new JScrollPane(previewTable);
        scrollPane.setPreferredSize(new Dimension(0, 350));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private void styleButton(JButton btn, java.awt.Color bg) {
        btn.setBackground(bg);
        btn.setForeground(java.awt.Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    private void loadReport(String reportType) {
        statusLabel.setText("Loading " + reportType + "...");
        statusLabel.setForeground(PRIMARY_BLUE);
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    switch (reportType) {
                        case "Patient Report":
                            loadPatientReport();
                            break;
                        case "Appointment Report":
                            loadAppointmentReport();
                            break;
                        case "Pending Approvals Report":
                            loadPendingApprovalsReport();
                            break;
                        case "Completed Treatments Report":
                            loadCompletedTreatmentsReport();
                            break;
                        case "Cancelled Appointments Report":
                            loadCancelledAppointmentsReport();
                            break;
                        case "Service Popularity Report":
                            loadServiceReport();
                            break;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void done() {
                statusLabel.setText("Ready - " + currentData.size() + " records");
                statusLabel.setForeground(java.awt.Color.GRAY);
                System.out.println("Report loaded: " + currentReportName + " - " + currentData.size() + " records");
            }
        };
        worker.execute();
    }
    
    private void loadPatientReport() throws SQLException {
        java.util.Date startDateObj = startDateChooser.getDate();
        java.util.Date endDateObj = endDateChooser.getDate();
        
        String startDate = startDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(startDateObj) : "1900-01-01";
        String endDate = endDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(endDateObj) : "2099-12-31";
        
        currentDateRange = startDateObj != null && endDateObj != null ? 
            new SimpleDateFormat("MMM dd, yyyy").format(startDateObj) + " - " + new SimpleDateFormat("MMM dd, yyyy").format(endDateObj) : "All Time";
        
        String query = "SELECT patient_id, first_name, last_name, email, contact_number, registration_date " +
                       "FROM patients WHERE registration_date BETWEEN ? AND ? ORDER BY registration_date DESC";
        
        List<Object[]> data = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            
            String[] columns = {"ID", "First Name", "Last Name", "Email", "Contact", "Registration Date"};
            tableModel.setColumnIdentifiers(columns);
            tableModel.setRowCount(0);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("patient_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("contact_number"),
                    rs.getTimestamp("registration_date")
                };
                data.add(row);
                tableModel.addRow(row);
            }
        }
        currentData = data;
        currentReportName = "Patient Report";
        currentTotalRecords = data.size();
    }
    
    private void loadAppointmentReport() throws SQLException {
        java.util.Date startDateObj = startDateChooser.getDate();
        java.util.Date endDateObj = endDateChooser.getDate();
        
        String startDate = startDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(startDateObj) : "1900-01-01";
        String endDate = endDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(endDateObj) : "2099-12-31";
        
        currentDateRange = startDateObj != null && endDateObj != null ? 
            new SimpleDateFormat("MMM dd, yyyy").format(startDateObj) + " - " + new SimpleDateFormat("MMM dd, yyyy").format(endDateObj) : "All Time";
        
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, s.service_name AS service_type, " +
                   "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.status " +
                   "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                   "LEFT JOIN services s ON a.service_id = s.service_id " +
                       "WHERE a.appointment_date BETWEEN ? AND ? ORDER BY a.appointment_date DESC";
        
        List<Object[]> data = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            
            String[] columns = {"Appt ID", "Patient Name", "Service", "Date", "Time", "Status"};
            tableModel.setColumnIdentifiers(columns);
            tableModel.setRowCount(0);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("appointment_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("service_type"),
                    rs.getDate("appointment_date"),
                    rs.getString("appointment_time"),
                    rs.getString("status")
                };
                data.add(row);
                tableModel.addRow(row);
            }
        }
        currentData = data;
        currentReportName = "Appointment Report";
        currentTotalRecords = data.size();
    }
    
    private void loadPendingApprovalsReport() throws SQLException {
        java.util.Date startDateObj = startDateChooser.getDate();
        java.util.Date endDateObj = endDateChooser.getDate();
        
        String startDate = startDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(startDateObj) : "1900-01-01";
        String endDate = endDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(endDateObj) : "2099-12-31";
        
        currentDateRange = startDateObj != null && endDateObj != null ? 
            new SimpleDateFormat("MMM dd, yyyy").format(startDateObj) + " - " + new SimpleDateFormat("MMM dd, yyyy").format(endDateObj) : "All Time";
        
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, s.service_name AS service_type, " +
                   "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.request_date " +
                   "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                   "LEFT JOIN services s ON a.service_id = s.service_id " +
                       "WHERE a.status = 'Pending' AND a.appointment_date BETWEEN ? AND ? ORDER BY a.request_date ASC";
        
        List<Object[]> data = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            
            String[] columns = {"Appt ID", "Patient Name", "Service", "Requested Date", "Requested Time", "Requested On"};
            tableModel.setColumnIdentifiers(columns);
            tableModel.setRowCount(0);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("appointment_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("service_type"),
                    rs.getDate("appointment_date"),
                    rs.getString("appointment_time"),
                    rs.getTimestamp("request_date")
                };
                data.add(row);
                tableModel.addRow(row);
            }
        }
        currentData = data;
        currentReportName = "Pending Approvals Report";
        currentTotalRecords = data.size();
    }
    
    private void loadCompletedTreatmentsReport() throws SQLException {
        java.util.Date startDateObj = startDateChooser.getDate();
        java.util.Date endDateObj = endDateChooser.getDate();
        
        String startDate = startDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(startDateObj) : "1900-01-01";
        String endDate = endDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(endDateObj) : "2099-12-31";
        
        currentDateRange = startDateObj != null && endDateObj != null ? 
            new SimpleDateFormat("MMM dd, yyyy").format(startDateObj) + " - " + new SimpleDateFormat("MMM dd, yyyy").format(endDateObj) : "All Time";
        
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, s.service_name AS service_type, " +
                   "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.clinical_notes " +
                   "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                   "LEFT JOIN services s ON a.service_id = s.service_id " +
                       "WHERE a.status = 'Completed' AND a.appointment_date BETWEEN ? AND ? ORDER BY a.appointment_date DESC";
        
        List<Object[]> data = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            
            String[] columns = {"Appt ID", "Patient Name", "Service", "Date", "Time", "Clinical Notes"};
            tableModel.setColumnIdentifiers(columns);
            tableModel.setRowCount(0);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("appointment_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("service_type"),
                    rs.getDate("appointment_date"),
                    rs.getString("appointment_time"),
                    rs.getString("clinical_notes") != null ? rs.getString("clinical_notes") : "No notes"
                };
                data.add(row);
                tableModel.addRow(row);
            }
        }
        currentData = data;
        currentReportName = "Completed Treatments Report";
        currentTotalRecords = data.size();
    }
    
    private void loadCancelledAppointmentsReport() throws SQLException {
        java.util.Date startDateObj = startDateChooser.getDate();
        java.util.Date endDateObj = endDateChooser.getDate();
        
        String startDate = startDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(startDateObj) : "1900-01-01";
        String endDate = endDateObj != null ? new SimpleDateFormat("yyyy-MM-dd").format(endDateObj) : "2099-12-31";
        
        currentDateRange = startDateObj != null && endDateObj != null ? 
            new SimpleDateFormat("MMM dd, yyyy").format(startDateObj) + " - " + new SimpleDateFormat("MMM dd, yyyy").format(endDateObj) : "All Time";
        
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, s.service_name AS service_type, " +
                   "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.status " +
                   "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                   "LEFT JOIN services s ON a.service_id = s.service_id " +
                       "WHERE a.status IN ('Cancelled', 'Declined') AND a.appointment_date BETWEEN ? AND ? ORDER BY a.appointment_date DESC";
        
        List<Object[]> data = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            
            String[] columns = {"Appt ID", "Patient Name", "Service", "Date", "Time", "Status"};
            tableModel.setColumnIdentifiers(columns);
            tableModel.setRowCount(0);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("appointment_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("service_type"),
                    rs.getDate("appointment_date"),
                    rs.getString("appointment_time"),
                    rs.getString("status")
                };
                data.add(row);
                tableModel.addRow(row);
            }
        }
        currentData = data;
        currentReportName = "Cancelled Appointments Report";
        currentTotalRecords = data.size();
    }
    
    private void loadServiceReport() throws SQLException {
        currentDateRange = "All Time (No date filter for this report)";
        
        String query = "SELECT s.service_name AS service_type, COUNT(*) as total, " +
                   "SUM(CASE WHEN a.status = 'Completed' THEN 1 ELSE 0 END) as completed, " +
                   "SUM(CASE WHEN a.status = 'Approved' THEN 1 ELSE 0 END) as approved, " +
                   "SUM(CASE WHEN a.status = 'Pending' THEN 1 ELSE 0 END) as pending, " +
                   "SUM(CASE WHEN a.status IN ('Cancelled', 'Declined') THEN 1 ELSE 0 END) as cancelled " +
                   "FROM appointments a LEFT JOIN services s ON a.service_id = s.service_id " +
                   "GROUP BY s.service_name ORDER BY total DESC";
        
        List<Object[]> data = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            String[] columns = {"Service Type", "Total Bookings", "Completed", "Approved", "Pending", "Cancelled"};
            tableModel.setColumnIdentifiers(columns);
            tableModel.setRowCount(0);
            
            while (rs.next()) {
                Object[] row = {
                    rs.getString("service_type"),
                    rs.getInt("total"),
                    rs.getInt("completed"),
                    rs.getInt("approved"),
                    rs.getInt("pending"),
                    rs.getInt("cancelled")
                };
                data.add(row);
                tableModel.addRow(row);
            }
        }
        currentData = data;
        currentReportName = "Service Popularity Report";
        currentTotalRecords = data.size();
    }
    
    private void exportToExcel() {
        if (currentData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data to export. Generate a report first.");
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        String fileName = currentReportName.replace(" ", "_") + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".xlsx";
        fileChooser.setSelectedFile(new java.io.File(fileName));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".xlsx")) {
                file = new java.io.File(file.getAbsolutePath() + ".xlsx");
            }
            
            try (Workbook workbook = new XSSFWorkbook()) {
                String sheetName = currentReportName.length() > 31 ? currentReportName.substring(0, 31) : currentReportName;
                Sheet sheet = workbook.createSheet(sheetName);
                
                // ==========================================================
                // REPORT HEADER SECTION
                // ==========================================================
                
                // Title Font
                org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
                titleFont.setBold(true);
                titleFont.setFontHeightInPoints((short) 16);
                titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
                
                CellStyle titleStyle = workbook.createCellStyle();
                titleStyle.setFont(titleFont);
                titleStyle.setAlignment(HorizontalAlignment.CENTER);
                
                // Subtitle Font
                org.apache.poi.ss.usermodel.Font subtitleFont = workbook.createFont();
                subtitleFont.setItalic(true);
                subtitleFont.setFontHeightInPoints((short) 10);
                subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
                
                CellStyle subtitleStyle = workbook.createCellStyle();
                subtitleStyle.setFont(subtitleFont);
                subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
                
                // Create title rows
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
                
                // Generation info row
                Row infoRow = sheet.createRow(2);
                Cell infoCell = infoRow.createCell(0);
                String generationInfo = "Generated on: " + new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a").format(new java.util.Date()) + 
                                        " | Date Range: " + currentDateRange +
                                        " | Total Records: " + currentTotalRecords;
                infoCell.setCellValue(generationInfo);
                sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, tableModel.getColumnCount() - 1));
                
                // Blank row
                sheet.createRow(3);
                
                // ==========================================================
                // HEADER ROW
                // ==========================================================
                
                Row headerRow = sheet.createRow(4);
                CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(tableModel.getColumnName(i));
                    cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(i, 4000);
                }
                
                // ==========================================================
                // DATA ROWS
                // ==========================================================
                
                CellStyle evenRowStyle = workbook.createCellStyle();
                evenRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                evenRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                evenRowStyle.setBorderTop(BorderStyle.THIN);
                evenRowStyle.setBorderBottom(BorderStyle.THIN);
                evenRowStyle.setBorderLeft(BorderStyle.THIN);
                evenRowStyle.setBorderRight(BorderStyle.THIN);
                
                CellStyle oddRowStyle = workbook.createCellStyle();
                oddRowStyle.setBorderTop(BorderStyle.THIN);
                oddRowStyle.setBorderBottom(BorderStyle.THIN);
                oddRowStyle.setBorderLeft(BorderStyle.THIN);
                oddRowStyle.setBorderRight(BorderStyle.THIN);
                
                for (int row = 0; row < currentData.size(); row++) {
                    Row dataRow = sheet.createRow(row + 5);
                    Object[] rowData = currentData.get(row);
                    CellStyle rowStyle = (row % 2 == 1) ? evenRowStyle : oddRowStyle;
                    
                    for (int col = 0; col < rowData.length; col++) {
                        Cell cell = dataRow.createCell(col);
                        Object value = rowData[col];
                        
                        if (value instanceof java.sql.Timestamp) {
                            cell.setCellValue(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((java.sql.Timestamp) value));
                        } else if (value instanceof java.sql.Date) {
                            cell.setCellValue(new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date) value));
                        } else {
                            cell.setCellValue(value != null ? value.toString() : "");
                        }
                        cell.setCellStyle(rowStyle);
                    }
                }
                
                // Auto-size columns
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    sheet.autoSizeColumn(i);
                    sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 500, 15000));
                }
                
                // Add footer with clinic info
                Row footerRow = sheet.createRow(currentData.size() + 6);
                Cell footerCell = footerRow.createCell(0);
                footerCell.setCellValue("Vantage Dental Clinic - Confidential Report");
                sheet.addMergedRegion(new CellRangeAddress(currentData.size() + 6, currentData.size() + 6, 0, tableModel.getColumnCount() - 1));
                
                // Write to file
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }
                
                // Log the export
                logService.record(currentAdminId, isSuperAdmin ? "Super Admin" : "Admin", 
                    "Export Report", "Exported " + currentReportName + " to Excel (" + currentTotalRecords + " records)");
                
                JOptionPane.showMessageDialog(this, 
                    "✅ Report exported successfully!\n\n" +
                    "Report: " + currentReportName + "\n" +
                    "File: " + file.getName() + "\n" +
                    "Location: " + file.getParent() + "\n\n" +
                    "Records exported: " + currentTotalRecords,
                    "Export Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
}