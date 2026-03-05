package com.xodud1202.springbackend;

import com.xodud1202.springbackend.controller.bo.AdminAuthController;
import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.repository.UserRepository;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.UserBaseService;
import com.xodud1202.springbackend.service.UserRefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
// 관리자 로그인 컨트롤러의 성공/실패 응답을 단위 테스트합니다.
class LoginControllerTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserBaseService userBaseService;

    @Mock
    private UserRefreshTokenService userRefreshTokenService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminAuthController adminAuthController;

    private MockMvc mockMvc;

    @BeforeEach
    // MockMvc standalone 환경과 @Value 필드를 초기화합니다.
    void setUp() {
        ReflectionTestUtils.setField(adminAuthController, "jwtRefreshTokenExpirationInMs", 2592000000L);
        ReflectionTestUtils.setField(adminAuthController, "jwtCookieSecure", false);
        mockMvc = MockMvcBuilders.standaloneSetup(adminAuthController).build();
    }

    @Test
    // 로그인 성공 시 accessToken이 반환되는지 확인합니다.
    void loginSuccess() throws Exception {
        UserBaseEntity user = new UserBaseEntity();
        user.setUsrNo(1L);
        user.setLoginId("xodud1202");
        user.setUserNm("테스트");

        when(userBaseService.loadUserByLoginId("xodud1202")).thenReturn(Optional.of(user));
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("test-access-token");

        mockMvc.perform(post("/api/backoffice/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"xodud1202\",\"pwd\":\"qwer\",\"rememberMe\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    // 로그인 실패 시 401을 반환하는지 확인합니다.
    void loginFail() throws Exception {
        when(userBaseService.loadUserByLoginId("bad")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/backoffice/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"bad\",\"pwd\":\"user\",\"rememberMe\":false}"))
                .andExpect(status().isUnauthorized());
    }
}
