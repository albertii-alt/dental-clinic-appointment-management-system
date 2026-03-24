package com.dentalclinic.service;

import com.dentalclinic.dao.DashboardDAO;
import java.util.Map;
import java.util.List;

public class DashboardService {
    private DashboardDAO dashboardDAO;

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
    }

    public Map<String, Integer> fetchDashboardStats() {
        try {
            return dashboardDAO.getQuickStats();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Object[]> fetchTodaySchedule() {
        try {
            return dashboardDAO.getTodayAppointments();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    public List<String[]> fetchRecentActivity() {
        try {
            return dashboardDAO.getRecentActivityWithDetails();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    // Add this method to your DashboardService class
    public Map<String, Integer> fetchAppointmentTrends() {
        try {
            return dashboardDAO.getAppointmentTrends();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.HashMap<>();
        }
    }
}