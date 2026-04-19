package com.dentalclinic.backend.controller;

import com.dentalclinic.backend.dto.LoginRequestDto;
import com.dentalclinic.backend.service.AuthLoginResult;
import com.dentalclinic.backend.service.AuthLoginService;
import com.dentalclinic.backend.service.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthLoginService authLoginService;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthLoginService authLoginService, JwtTokenService jwtTokenService) {
        this.authLoginService = authLoginService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDto requestDto) {
        AuthLoginResult result = authLoginService.login(requestDto);

        Map<String, Object> body = new LinkedHashMap<>(result.body());
        if (result.statusCode() == 200 && "success".equals(String.valueOf(body.get("status")))) {
            body.put("tokenType", "Bearer");
            body.put("accessToken", jwtTokenService.createAccessToken(body));
            body.put("expiresInMinutes", jwtTokenService.expiresMinutes());
        }

        return ResponseEntity.status(result.statusCode()).body(body);
    }
}
