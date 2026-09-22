package com.nira.finance.dto;

public class AuthResponse {
    private String token;
    private String displayName;

    public AuthResponse(String token, String displayName) {
        this.token = token;
        this.displayName = displayName;
    }
    public String getToken() { return token; }
    public String getDisplayName() { return displayName; }
}
