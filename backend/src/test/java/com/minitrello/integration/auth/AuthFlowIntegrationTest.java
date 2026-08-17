package com.minitrello.integration.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test against a REAL MySQL instance (via Testcontainers) —
 * not H2. Schema drift between an in-memory test DB and production MySQL
 * (different SQL dialects, different default collations, etc.) is exactly
 * the class of bug this is meant to catch, per the project's testing
 * requirements.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("mini_trello_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mysql.getJdbcUrl() + "?allowPublicKeyRetrieval=true&useSSL=false");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-key-must-be-long-enough-for-hmac-sha256-signing");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullAuthFlow_registerLoginRefresh_shouldSucceed() throws Exception {
        String email = "integration.test@example.com";
        String registerPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email,
                "password", "SecurePass1",
                "fullName", "Integration Test"
        ));

        // 1. Register
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        Cookie refreshCookie = registerResult.getResponse().getCookie("refreshToken");

        // 2. Login with the same credentials
        String loginPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email,
                "password", "SecurePass1"
        ));
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(email));

        // 3. Refresh using the cookie from registration — must succeed and
        //    rotate to a NEW refresh token (old one is now revoked).
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(cookie().exists("refreshToken"));

        // 4. Reusing the now-revoked original refresh token must be rejected.
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_shouldRejectDuplicateEmail() throws Exception {
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "email", "duplicate@example.com",
                "password", "SecurePass1",
                "fullName", "First User"
        ));

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_shouldRejectWeakPassword() throws Exception {
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "email", "weakpass@example.com",
                "password", "weak",
                "fullName", "Weak Password"
        ));

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isBadRequest());
    }
}
