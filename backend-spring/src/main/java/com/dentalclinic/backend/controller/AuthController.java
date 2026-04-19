package com.dentalclinic.backend.controller;

import com.dentalclinic.backend.dto.LoginRequestDto;
import com.dentalclinic.backend.service.AuthLoginResult;
import com.dentalclinic.backend.service.AuthLoginService;
import com.dentalclinic.backend.service.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        String token = jwtTokenService.extractTokenFromHeader(authorizationHeader);
        if (token == null || !jwtTokenService.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of(
                    "valid", false,
                    "message", "Invalid or missing bearer token"
            ));
        }

        Map<String, Object> claims = jwtTokenService.parseTokenClaims(token);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("valid", true);
        out.put("claims", claims);
        return ResponseEntity.ok(out);
    }
}
