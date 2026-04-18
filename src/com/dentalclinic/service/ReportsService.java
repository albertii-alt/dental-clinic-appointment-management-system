package com.dentalclinic.service;

import com.dentalclinic.dao.ReportsDAO;
import com.dentalclinic.dto.report.ReportData;
import com.dentalclinic.dto.report.ReportRequest;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReportsService {
    private final ReportsDAO reportsDAO = new ReportsDAO();
    private static final String MIN_DATE = "1900-01-01";
    private static final String MAX_DATE = "2099-12-31";
    private static final SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat UI_DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    public ReportData generateReport(ReportRequest request) throws SQLException {
        String reportType = request.getReportType();
        Date startDateObj = request.getStartDate();
        Date endDateObj = request.getEndDate();

        String startDate = startDateObj != null ? SQL_DATE_FORMAT.format(startDateObj) : MIN_DATE;
        String endDate = endDateObj != null ? SQL_DATE_FORMAT.format(endDateObj) : MAX_DATE;
        String dateRange = buildDateRange(reportType, startDateObj, endDateObj);

        String[] columns;
        List<Object[]> rows;

        switch (reportType) {
            case "Patient Report":
                columns = new String[]{"ID", "First Name", "Last Name", "Email", "Contact", "Registration Date"};
                rows = reportsDAO.fetchPatientReport(startDate, endDate);
                break;
            case "Appointment Report":
                columns = new String[]{"Appt ID", "Patient Name", "Service", "Date", "Time", "Status"};
                rows = reportsDAO.fetchAppointmentReport(startDate, endDate);
                break;
            case "Pending Approvals Report":
                columns = new String[]{"Appt ID", "Patient Name", "Service", "Requested Date", "Requested Time", "Requested On"};
                rows = reportsDAO.fetchPendingApprovalsReport(startDate, endDate);
                break;
            case "Completed Treatments Report":
                columns = new String[]{"Appt ID", "Patient Name", "Service", "Date", "Time", "Clinical Notes"};
                rows = reportsDAO.fetchCompletedTreatmentsReport(startDate, endDate);
                break;
            case "Cancelled Appointments Report":
                columns = new String[]{"Appt ID", "Patient Name", "Service", "Date", "Time", "Status"};
                rows = reportsDAO.fetchCancelledAppointmentsReport(startDate, endDate);
                break;
            case "Service Popularity Report":
                columns = new String[]{"Service Type", "Total Bookings", "Completed", "Approved", "Pending", "Cancelled"};
                rows = reportsDAO.fetchServicePopularityReport();
                break;
            default:
                throw new IllegalArgumentException("Unsupported report type: " + reportType);
        }

        return new ReportData(reportType, dateRange, rows.size(), columns, rows);
    }

    private String buildDateRange(String reportType, Date startDate, Date endDate) {
        if ("Service Popularity Report".equals(reportType)) {
            return "All Time (No date filter for this report)";
        }
        if (startDate != null && endDate != null) {
            return UI_DATE_FORMAT.format(startDate) + " - " + UI_DATE_FORMAT.format(endDate);
        }
        return "All Time";
    }
}
