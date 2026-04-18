package com.dentalclinic.controller;

import com.dentalclinic.service.DashboardService;
import java.util.List;
import java.util.Map;

public class DashboardController {
    private final DashboardService dashboardService = new DashboardService();

    public Map<String, Integer> fetchDashboardStats() {
        return dashboardService.fetchDashboardStats();
    }

    public List<String[]> fetchRecentActivity() {
        return dashboardService.fetchRecentActivity();
    }

    public Map<String, Integer> fetchAppointmentTrends() {
        return dashboardService.fetchAppointmentTrends();
    }
}
