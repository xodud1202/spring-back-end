package com.xodud1202.springbackend;

import com.xodud1202.springbackend.entity.UserBaseEntity;
import com.xodud1202.springbackend.repository.UserRepository;
import com.xodud1202.springbackend.security.JwtTokenProvider;
import com.xodud1202.springbackend.service.UserBaseService;
import com.xodud1202.springbackend.service.UserRefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
// 관리자 로그인 API를 검증합니다.
class LoginControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private UserBaseService userBaseService;

    @MockBean
    private UserRefreshTokenService userRefreshTokenService;

    @MockBean
    private UserRepository userRepository;

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
