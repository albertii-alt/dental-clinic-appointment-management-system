package com.dentalclinic.dto.report;

import java.util.List;

public class ReportData {
    private final String reportName;
    private final String dateRange;
    private final int totalRecords;
    private final String[] columns;
    private final List<Object[]> rows;

    public ReportData(String reportName, String dateRange, int totalRecords, String[] columns, List<Object[]> rows) {
        this.reportName = reportName;
        this.dateRange = dateRange;
        this.totalRecords = totalRecords;
        this.columns = columns;
        this.rows = rows;
    }

    public String getReportName() {
        return reportName;
    }

    public String getDateRange() {
        return dateRange;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public String[] getColumns() {
        return columns;
    }

    public List<Object[]> getRows() {
        return rows;
    }
}
