package com.dentalclinic.backend.controller;

import com.dentalclinic.backend.service.DashboardSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardSummaryService dashboardSummaryService;

    public DashboardController(DashboardSummaryService dashboardSummaryService) {
        this.dashboardSummaryService = dashboardSummaryService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_STAFF','ROLE_DENTIST')")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(dashboardSummaryService.getSummary());
    }
}
