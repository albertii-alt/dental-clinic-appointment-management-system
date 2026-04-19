package com.dentalclinic.backend.controller;

import com.dentalclinic.backend.service.AuthLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final AuthLoginService authLoginService;

    public HealthController(AuthLoginService authLoginService) {
        this.authLoginService = authLoginService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        if (authLoginService.isDatabaseAvailable()) {
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "spring backend and database are ready"
            ));
        }

        return ResponseEntity.status(503).body(Map.of(
                "status", "error",
                "message", "database unavailable"
        ));
    }
}
