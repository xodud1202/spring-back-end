package com.xodud1202.springbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoginControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginSuccess() throws Exception {
        mockMvc.perform(post("/backoffice/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"xodud1202\",\"password\":\"qwer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginFail() throws Exception {
        mockMvc.perform(post("/backoffice/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"bad\",\"password\":\"user\"}"))
                .andExpect(status().isUnauthorized());
    }
}