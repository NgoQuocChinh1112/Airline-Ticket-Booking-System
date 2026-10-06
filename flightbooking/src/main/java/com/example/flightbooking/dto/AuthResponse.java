package com.example.flightbooking.dto;

public class AuthResponse {
    private String accessToken;
    private String userId;
    private String email;

    public AuthResponse(String accessToken, String userId, String email) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.email = email;
    }

    public String getAccessToken() { return accessToken; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
}
