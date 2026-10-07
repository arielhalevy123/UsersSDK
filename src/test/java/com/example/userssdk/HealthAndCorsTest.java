package com.example.userssdk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.cors.allowed-origins=https://portal.example.com")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthAndCorsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsPublicAndUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
    }

    @Test
    void preflightFromAllowedOriginSucceeds() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "https://portal.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://portal.example.com"));
    }

    @Test
    void preflightFromUnknownOriginIsRejected() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
