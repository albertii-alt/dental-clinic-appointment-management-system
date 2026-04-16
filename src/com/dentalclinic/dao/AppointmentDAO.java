package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import com.dentalclinic.model.Appointment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {
    private static final String APPOINTMENT_SELECT =
        "SELECT a.appointment_id, a.patient_id, COALESCE(s.service_name, 'Unknown') AS service_type, " +
        "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, " +
        "a.age_at_visit, a.contact_at_visit, a.status, a.clinical_notes, a.is_read, a.is_archived ";

    private static final String APPOINTMENT_FROM =
        "FROM appointments a LEFT JOIN services s ON a.service_id = s.service_id ";

    private Appointment mapAppointment(ResultSet rs) throws SQLException {
        Appointment app = new Appointment(
            rs.getInt("appointment_id"), rs.getInt("patient_id"),
            rs.getString("service_type"), rs.getDate("appointment_date"),
            rs.getString("appointment_time"), rs.getInt("age_at_visit"),
            rs.getString("contact_at_visit"), rs.getString("status"),
            rs.getString("clinical_notes"), rs.getBoolean("is_read")
        );
        app.setArchived(rs.getBoolean("is_archived"));
        return app;
    }
    

    public boolean registerPatient(String fName, String mName, String lName, java.sql.Date dob, int age, String addr, String phone, String email, String user, String pass) throws SQLException {
        String query = "INSERT INTO patients (first_name, middle_name, last_name, birth_date, age, address, contact_number, email, username, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fName);
            pstmt.setString(2, mName);
            pstmt.setString(3, lName);
            pstmt.setDate(4, dob);
            pstmt.setInt(5, age);
            pstmt.setString(6, addr);
            pstmt.setString(7, phone);
            pstmt.setString(8, email);
            pstmt.setString(9, user);
            pstmt.setString(10, pass);
            return pstmt.executeUpdate() > 0;
        }
    }

    public String[] getDynamicServices() throws SQLException {
        List<String> services = new ArrayList<>();
        String query = "SELECT service_name FROM services WHERE is_active = 1 ORDER BY service_name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                services.add(rs.getString("service_name"));
            }
        }
        return services.isEmpty() ? new String[]{"General Consultation"} : services.toArray(new String[0]);
    }

    public String[] getDynamicTimeSlots() throws SQLException {
        List<String> slots = new ArrayList<>();
        String query = "SELECT time_slot FROM clinic_hours WHERE is_active = 1 ORDER BY STR_TO_DATE(time_slot, '%h:%i %p') ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                slots.add(rs.getString("time_slot"));
            }
        }
        return slots.isEmpty() ? new String[]{"09:00 AM"} : slots.toArray(new String[0]);
    }

    public List<String> getClosedDaysFromDB() throws SQLException {
        List<String> closedDays = new ArrayList<>();
        String query = "SELECT day_name FROM clinic_schedule WHERE is_open = 0";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                closedDays.add(rs.getString("day_name"));
            }
        }
        return closedDays;
    }

    public int getLeadTime() throws SQLException {
        String query = "SELECT setting_value FROM clinic_settings WHERE setting_name = 'min_lead_days'";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) return rs.getInt("setting_value");
        }
        return 0;
    }

    public int save(Appointment app) throws SQLException {
        String query = "INSERT INTO appointments (patient_id, service_id, appointment_date, appointment_time_new, age_at_visit, contact_at_visit, status, is_read) " +
                       "VALUES (?, (SELECT service_id FROM services WHERE service_name = ?), ?, STR_TO_DATE(?, '%h:%i %p'), ?, ?, 'Pending', FALSE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, app.getPatientId());
            pstmt.setString(2, app.getServiceType());
            pstmt.setDate(3, app.getAppointmentDate());
            pstmt.setString(4, app.getAppointmentTime());
            pstmt.setInt(5, app.getAgeAtVisit());
            pstmt.setString(6, app.getContactAtVisit());
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            return -1;
        }
    }
    
    public List<Appointment> getAppointmentsByPatient(int pId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(int appId, String newStatus) throws SQLException {
        String query = "UPDATE appointments SET status = ?, is_read = FALSE WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, appId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Appointment> getAppointmentsByStatus(String status) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.status = ? ORDER BY a.appointment_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        }
        return list;
    }

    public List<Appointment> getAllAppointments() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "ORDER BY a.appointment_date DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapAppointment(rs));
            }
        }
        return list;
    }

    public List<Appointment> getTodaysAppointments() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.appointment_date = CURDATE() AND a.status = 'Approved'";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapAppointment(rs));
            }
        }
        return list;
    }

    public List<Appointment> getTodaysAppointmentsByPatient(int pId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.patient_id = ? AND a.appointment_date = CURDATE() AND a.status = 'Approved'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        }
        return list;
    }

    public List<Appointment> getFutureUpcoming(int pId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.patient_id = ? AND a.appointment_date > CURDATE() AND a.status = 'Approved' ORDER BY a.appointment_date ASC LIMIT 3";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        }
        return list;
    }
    
    public List<Appointment> getUnreadNotifications(int pId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.patient_id = ? " +
                   "AND a.is_read = FALSE AND a.is_archived = FALSE " +
                   "AND a.status IN ('Cancelled', 'Declined', 'Approved')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        }
        return list;
    }

    public List<Appointment> getUpcomingScheduleByPatient(int pId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.patient_id = ? AND a.appointment_date >= CURDATE() AND a.status = 'Approved' ORDER BY a.appointment_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        }
        return list;
    }

    public boolean markAsRead(int appId) {
        String query = "UPDATE appointments SET is_read = TRUE WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, appId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getUnreadNotificationCount(int pId) throws SQLException {
        String query = "SELECT COUNT(*) FROM appointments WHERE patient_id = ? " +
                       "AND is_read = FALSE AND is_archived = FALSE " +
                       "AND status IN ('Cancelled', 'Declined', 'Approved')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // Helper methods for Tables (Object arrays)
    public List<Object[]> getPendingAppointmentsWithNames() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String query = "SELECT a.appointment_id, a.patient_id, p.first_name, p.middle_name, p.last_name, " +
                   "COALESCE(s.service_name, 'Unknown') AS service_type, a.appointment_date, " +
                   "DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.status " +
                   "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                   "LEFT JOIN services s ON a.service_id = s.service_id " +
                   "WHERE a.status = 'Pending' ORDER BY a.appointment_date ASC";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                String fullName = rs.getString("first_name") + " " + rs.getString("middle_name") + " " + rs.getString("last_name");
                list.add(new Object[]{ rs.getInt("appointment_id"), rs.getInt("patient_id"), fullName, rs.getString("service_type"), rs.getDate("appointment_date"), rs.getString("appointment_time"), rs.getString("status") });
            }
        }
        return list;
    }

    public List<String> getOccupiedSlots(java.sql.Date date) throws SQLException {
        List<String> occupied = new ArrayList<>();
        String query = "SELECT DATE_FORMAT(appointment_time_new, '%h:%i %p') AS appointment_time FROM appointments WHERE appointment_date = ? AND status != 'Cancelled'";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDate(1, date);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) occupied.add(rs.getString("appointment_time"));
            }
        }
        return occupied;
    }

    public boolean updateTreatmentRecord(int appId, String status, String notes) throws SQLException {
        String query = "UPDATE appointments SET status = ?, clinical_notes = ? WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setString(2, notes);
            pstmt.setInt(3, appId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // Additional slot blocking methods...
    public List<String> getBlockedSlotsByDate(java.sql.Date date) throws SQLException {
        List<String> blocked = new ArrayList<>();
        String sql = "SELECT time_slot FROM blocked_slots WHERE block_date = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, date);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) blocked.add(rs.getString("time_slot"));
            }
        }
        return blocked;
    }
    
    public boolean blockSlot(java.sql.Date date, String slot, String reason, int staffId, String role) throws SQLException {
        String sql = "INSERT INTO blocked_slots (block_date, time_slot, reason) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, date);
            pstmt.setString(2, slot);
            pstmt.setString(3, reason);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                new com.dentalclinic.service.LogService().record(staffId, role, "Block Time Slot", 
                    "Service: Slot Management | Details: Blocked " + slot + " on " + date + " Reason: " + reason);
            }
            return success;
        }
    }

    public boolean unblockSlot(java.sql.Date date, String slot, int staffId, String role) throws SQLException {
        String sql = "DELETE FROM blocked_slots WHERE block_date = ? AND time_slot = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, date);
            pstmt.setString(2, slot);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                new com.dentalclinic.service.LogService().record(staffId, role, "Unblock Time Slot", 
                    "Service: Slot Management | Details: Unblocked " + slot + " on " + date);
            }
            return success;
        }
    }
    
    public boolean hasPendingAppointment(int pId) throws SQLException {
        String query = "SELECT COUNT(*) FROM appointments WHERE patient_id = ? AND status = 'Pending'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean updateDateTime(int appId, java.sql.Date newDate, String newTime) throws SQLException {
        String query = "UPDATE appointments SET appointment_date = ?, appointment_time_new = STR_TO_DATE(?, '%h:%i %p') WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDate(1, newDate);
            pstmt.setString(2, newTime);
            pstmt.setInt(3, appId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Object[]> getCancelledAppointmentsWithNames() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, COALESCE(s.service_name, 'Unknown') AS service_type, a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.status " +
                   "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id LEFT JOIN services s ON a.service_id = s.service_id " +
                       "WHERE a.status IN ('Cancelled', 'Declined') ORDER BY a.appointment_date DESC";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Object[]{ rs.getInt(1), rs.getString(2) + " " + rs.getString(3), rs.getString(4), rs.getDate(5), rs.getString(6), rs.getString(7) });
            }
        }
        return list;
    }

    public List<Object[]> getTodaysAppointmentsWithNames() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, COALESCE(s.service_name, 'Unknown') AS service_type, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.status " +
                   "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id LEFT JOIN services s ON a.service_id = s.service_id " +
                       "WHERE a.appointment_date = CURDATE() AND a.status = 'Approved'";
        try (Connection conn = DBConnection.getConnection(); 
             Statement st = conn.createStatement(); 
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Object[]{ 
                    rs.getInt(1),
                    rs.getString(2) + " " + rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6)
                });
            }
        }
        return list;
    }

    public List<Object[]> getCompletedAppointmentsWithNames() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, COALESCE(s.service_name, 'Unknown') AS service_type, a.appointment_date, a.clinical_notes " +
                   "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id LEFT JOIN services s ON a.service_id = s.service_id " +
                       "WHERE a.status = 'Completed' ORDER BY a.appointment_date DESC";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Object[]{ rs.getInt(1), rs.getString(2) + " " + rs.getString(3), rs.getString(4), rs.getDate(5), rs.getString(6) });
            }
        }
        return list;
    }
    
    public List<Appointment> getAppointmentsByDateRange(boolean onlyToday) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = onlyToday ? APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.appointment_date = CURDATE() AND a.status = 'Approved'"
                     : APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.appointment_date >= CURDATE() AND a.status = 'Approved'";
        try (Connection conn = DBConnection.getConnection(); 
             Statement st = conn.createStatement(); 
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapAppointment(rs));
            }
        }
        return list;
    }

    public List<Object[]> getServiceDetails() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String query = "SELECT service_name, is_active FROM services ORDER BY service_name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("service_name"), 
                    rs.getInt("is_active")
                });
            }
        }
        return list;
    }

    public List<String> getAllTimeSlotsFromDB() throws SQLException {
        List<String> list = new ArrayList<>();
        String query = "SELECT time_slot FROM clinic_hours";
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }
    
    public void blockAllDay(java.sql.Date date, String[] allSlots, int staffId, String role) throws SQLException {
        String sql = "INSERT IGNORE INTO blocked_slots (block_date, time_slot, reason) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String slot : allSlots) {
                pstmt.setDate(1, date);
                pstmt.setString(2, slot);
                pstmt.setString(3, "Staff Manual Block (All Day)");
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            new com.dentalclinic.service.LogService().record(staffId, role, "Block Day", 
                "Service: Slot Management | Details: Blocked all slots for date: " + date);
        }
    }

    public boolean unblockAllDay(java.sql.Date date, int staffId, String role) throws SQLException {
        String query = "DELETE FROM blocked_slots WHERE block_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDate(1, date);
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                new com.dentalclinic.service.LogService().record(
                    staffId, 
                    role, 
                    "Clear All Blocks", 
                    "Service: Schedule | Details: Removed all blocks for " + date.toString()
                );
            }
            return affected > 0;
        }
    }
    
    public boolean archiveNotification(int appId) throws SQLException {
        String query = "UPDATE appointments SET is_archived = TRUE WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, appId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean archiveAllNotifications(int pId) throws SQLException {
        String query = "UPDATE appointments SET is_archived = TRUE WHERE patient_id = ? AND status != 'Pending'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public List<Appointment> getApprovedUpcomingAppointments() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.appointment_date >= CURDATE() AND a.status = 'Approved' ORDER BY a.appointment_date ASC";
        try (Connection conn = DBConnection.getConnection(); 
             Statement st = conn.createStatement(); 
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapAppointment(rs));
            }
        }
        return list;
    }
    
    public String getServiceNameByAppId(int appId) throws SQLException {
        String sql = "SELECT COALESCE(s.service_name, 'Unknown') AS service_type FROM appointments a " +
                 "LEFT JOIN services s ON a.service_id = s.service_id WHERE a.appointment_id = ?";
        try (Connection conn = com.dentalclinic.util.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("service_type");
            }
        }
        return "Unknown";
    }
    
    public List<Appointment> getRecentCancelledByPatient(int pId, int daysLimit) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.patient_id = ? " +
                   "AND a.status = 'Cancelled' " +
                   "AND a.is_archived = FALSE " +
                   "AND a.appointment_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                   "ORDER BY a.appointment_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, pId);
            pstmt.setInt(2, daysLimit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAppointment(rs));
                }
            }
        }
        return list;
    }
    
    public Appointment getAppointmentById(int appId) throws SQLException {
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, appId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
            }
        }
        return null;
    }

    // ==========================================================
    // EMAIL REMINDER METHODS (NEW)
    // ==========================================================

    public List<Appointment> getAppointmentsForTomorrow() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.appointment_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) " +
                   "AND a.status = 'Approved' AND a.reminder_sent = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapAppointment(rs));
            }
        }
        return list;
    }

    public boolean markReminderSent(int appId) throws SQLException {
        String query = "UPDATE appointments SET reminder_sent = 1 WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, appId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public Appointment getAppointmentByIdForReminder(int appId) throws SQLException {
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.appointment_id = ? AND a.status = 'Approved'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, appId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapAppointment(rs);
                }
            }
        }
        return null;
    }
    
    // ==========================================================
    // DAY-OF REMINDER METHODS
    // ==========================================================

    /**
     * Get appointments for TODAY that haven't received day-of reminder yet
     */
    public List<Appointment> getAppointmentsForToday() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String query = APPOINTMENT_SELECT + APPOINTMENT_FROM + "WHERE a.appointment_date = CURDATE() " +
                   "AND a.status = 'Approved' AND a.day_of_reminder_sent = 0";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapAppointment(rs));
            }
        }
        return list;
    }

    /**
     * Mark day-of reminder as sent for an appointment
     */
    public boolean markDayOfReminderSent(int appId) throws SQLException {
        String query = "UPDATE appointments SET day_of_reminder_sent = 1 WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, appId);
            return pstmt.executeUpdate() > 0;
        }
    }
}