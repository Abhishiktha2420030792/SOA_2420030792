package com.carebridge.auth.dto;

public class AuthResponse {

    private String userId;
    private String name;
    private String email;
    private String role;
    private String accessToken;
    private String refreshToken;

    public AuthResponse() {}

    public AuthResponse(String userId, String name, String email, String role,
                         String accessToken, String refreshToken) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
