package com.dentalclinic.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardSummaryService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardSummaryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getSummary() {
        String query = "SELECT " +
                "(SELECT COUNT(*) FROM patients) AS total_patients, " +
                "(SELECT COUNT(*) FROM appointments WHERE status = 'Pending') AS pending_appointments, " +
                "(SELECT COUNT(*) FROM appointments WHERE appointment_date = CURDATE() AND status = 'Approved') AS today_appointments, " +
                "(SELECT COUNT(*) FROM staff WHERE is_active = 1) AS active_staff";

        Map<String, Object> row = jdbcTemplate.queryForMap(query);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalPatients", toInt(row.get("total_patients")));
        summary.put("pendingAppointments", toInt(row.get("pending_appointments")));
        summary.put("todayAppointments", toInt(row.get("today_appointments")));
        summary.put("activeStaff", toInt(row.get("active_staff")));
        summary.put("generatedAt", Instant.now().toString());
        return summary;
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
