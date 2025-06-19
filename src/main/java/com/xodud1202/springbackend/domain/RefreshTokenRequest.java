package com.xodud1202.springbackend.domain;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}