package com.dentalclinic.service;

import com.dentalclinic.dao.DashboardDAO;
import java.util.Map;
import java.util.List;

public class DashboardService {
    private DashboardDAO dashboardDAO = new DashboardDAO();
    
    // Cache to reduce database calls
    private Map<String, Integer> cachedStats = null;
    private List<String[]> cachedActivity = null;
    private Map<String, Integer> cachedTrends = null;
    private long lastRefreshTime = 0;
    private static final long CACHE_DURATION = 30000; // 30 seconds cache

    public Map<String, Integer> fetchDashboardStats() {
        long now = System.currentTimeMillis();
        if (cachedStats == null || (now - lastRefreshTime) > CACHE_DURATION) {
            try {
                cachedStats = dashboardDAO.getQuickStats();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cachedStats;
    }

    public List<String[]> fetchRecentActivity() {
        long now = System.currentTimeMillis();
        if (cachedActivity == null || (now - lastRefreshTime) > CACHE_DURATION) {
            try {
                cachedActivity = dashboardDAO.getRecentActivityWithDetails();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cachedActivity;
    }

    public Map<String, Integer> fetchAppointmentTrends() {
        long now = System.currentTimeMillis();
        if (cachedTrends == null || (now - lastRefreshTime) > CACHE_DURATION) {
            try {
                cachedTrends = dashboardDAO.getAppointmentTrends();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cachedTrends;
    }
    
    public void refreshAll() {
        lastRefreshTime = 0; // Force refresh on next call
        fetchDashboardStats();
        fetchRecentActivity();
        fetchAppointmentTrends();
        lastRefreshTime = System.currentTimeMillis();
    }
}