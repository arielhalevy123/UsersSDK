package com.example.userssdk;

import com.example.userssdk.config.JwtUtil;
import com.example.userssdk.entities.User;
import com.example.userssdk.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User listings must not leak to anonymous callers. Relies on the demo seed data
 * (admin@example.com, mike@barber.com as admins; alice@example.com as a user).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserListingAccessTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository users;

    private String bearer(String email) {
        User u = users.findByEmail(email).orElseThrow();
        return "Bearer " + jwtUtil.generateToken(u);
    }

    @Test
    void allUsersWithoutTokenIsRejectedAndLeaksNothing() throws Exception {
        MvcResult r = mvc.perform(get("/api/auth/all"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        assertThat(body).doesNotContain("@example.com").doesNotContain("Appointment");
    }

    @Test
    void allUsersIsForbiddenForPlainUsers() throws Exception {
        mvc.perform(get("/api/auth/all").header("Authorization", bearer("alice@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allUsersStillWorksForAdmins() throws Exception {
        mvc.perform(get("/api/auth/all").header("Authorization", bearer("admin@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    void publicAdminListExposesOnlyIdAndName() throws Exception {
        MvcResult r = mvc.perform(get("/api/auth/admins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        assertThat(body).contains("Admin").contains("Mike Barber")
                .doesNotContain("email").doesNotContain("@")
                .doesNotContain("customFields").doesNotContain("password")
                .doesNotContain("Alice");
    }

    @Test
    void usersOfAnAdminRequireThatAdmin() throws Exception {
        Long adminId = users.findByEmail("admin@example.com").orElseThrow().getId();
        mvc.perform(get("/api/auth/admin/" + adminId + "/users"))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/auth/admin/" + adminId + "/users")
                        .header("Authorization", bearer("mike@barber.com")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/admin/" + adminId + "/users")
                        .header("Authorization", bearer("admin@example.com")))
                .andExpect(status().isOk());
    }
}
