package com.xodud1202.springbackend.domain;

public class JwtAuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    
    public JwtAuthenticationResponse(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public JwtAuthenticationResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    
    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}