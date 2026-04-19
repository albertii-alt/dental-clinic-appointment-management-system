package com.dentalclinic.backend.controller;

import com.dentalclinic.backend.dto.LoginRequestDto;
import com.dentalclinic.backend.service.AuthLoginResult;
import com.dentalclinic.backend.service.AuthLoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthLoginService authLoginService;

    public AuthController(AuthLoginService authLoginService) {
        this.authLoginService = authLoginService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDto requestDto) {
        AuthLoginResult result = authLoginService.login(requestDto);
        return ResponseEntity.status(result.statusCode()).body(result.body());
    }
}
