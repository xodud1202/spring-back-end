package com.xodud1202.springbackend.domain;

import lombok.Data;

@Data
public class LoginRequest {
    private String loginId;
    private String pwd;
    private boolean rememberMe;
}
