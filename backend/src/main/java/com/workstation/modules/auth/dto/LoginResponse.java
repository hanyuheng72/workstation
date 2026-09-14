package com.workstation.modules.auth.dto;

public record LoginResponse(String token, long expiresAt) {
}
