package com.dentalclinic.controller;

import com.dentalclinic.dto.report.ReportData;
import com.dentalclinic.dto.report.ReportRequest;
import com.dentalclinic.service.ReportsService;
import java.sql.SQLException;

public class ReportsController {
    private final ReportsService reportsService = new ReportsService();

    public ReportData loadReport(ReportRequest request) throws SQLException {
        return reportsService.generateReport(request);
    }
}
