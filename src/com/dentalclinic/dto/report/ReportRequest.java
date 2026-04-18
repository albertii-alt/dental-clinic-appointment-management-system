package com.dentalclinic.dto.report;

import java.util.Date;

public class ReportRequest {
    private final String reportType;
    private final Date startDate;
    private final Date endDate;

    public ReportRequest(String reportType, Date startDate, Date endDate) {
        this.reportType = reportType;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getReportType() {
        return reportType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
}
