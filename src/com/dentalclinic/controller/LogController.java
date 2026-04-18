package com.dentalclinic.controller;

import com.dentalclinic.service.LogService;
import java.util.List;

public class LogController {
    private final LogService logService = new LogService();

    public List<Object[]> getSystemLogs() throws Exception {
        return logService.getSystemLogs();
    }

    public boolean verifySuperAdminPassword(int adminId, String password) {
        return logService.verifySuperAdminPassword(adminId, password);
    }

    public boolean clearAllSystemLogs(int staffId, String role) {
        return logService.clearAllSystemLogs(staffId, role);
    }

    public void record(int actorId, String actorRole, String action, String details) {
        logService.record(actorId, actorRole, action, details);
    }
}
