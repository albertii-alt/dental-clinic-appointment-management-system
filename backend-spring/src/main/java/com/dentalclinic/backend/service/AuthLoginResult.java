package com.dentalclinic.backend.service;

import java.util.Map;

public record AuthLoginResult(int statusCode, Map<String, Object> body) {
}
