package com.minitrello.integration.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test through the real HTTP layer against a real MySQL
 * instance: register two users, one creates a workspace + project,
 * invites the second as CONTRIBUTOR, and we verify the permission
 * boundary (a CONTRIBUTOR cannot update/delete the project) end to end —
 * not just at the unit level.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectFlowIntegrationTest {

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

    private String registerAndGetAccessToken(String email, String fullName) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "SecurePass1", "fullName", fullName));
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("data").get("accessToken").asText();
    }

    @Test
    void fullProjectFlow_createInviteAndEnforcePermissions() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner@example.com", "Project Owner");
        String contributorToken = registerAndGetAccessToken("contributor@example.com", "Project Contributor");

        // 1. Owner creates a workspace
        String workspacePayload = objectMapper.writeValueAsString(Map.of("name", "Acme Corp"));
        String workspaceBody = mockMvc.perform(post("/api/workspaces")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(workspacePayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String workspaceId = objectMapper.readTree(workspaceBody).get("data").get("id").asText();

        // 2. Owner creates a project in that workspace
        String projectPayload = objectMapper.writeValueAsString(Map.of(
                "name", "Website Redesign", "description", "Q3 redesign initiative"));
        String projectBody = mockMvc.perform(post("/api/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(projectPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.callerRole").value("OWNER"))
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(projectBody).get("data").get("id").asText();

        // 3. Contributor cannot see the project yet (no membership -> 403)
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + contributorToken))
                .andExpect(status().isForbidden());

        // 4. Owner invites the contributor
        String invitePayload = objectMapper.writeValueAsString(Map.of(
                "email", "contributor@example.com", "role", "CONTRIBUTOR"));
        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(invitePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("CONTRIBUTOR"));

        // 5. Contributor can now view the project
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + contributorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.callerRole").value("CONTRIBUTOR"));

        // 6. But cannot update it — role boundary enforced server-side, not just hidden in the UI
        String updatePayload = objectMapper.writeValueAsString(Map.of(
                "name", "Hijacked Name", "description", "nope"));
        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + contributorToken)
                        .contentType("application/json").content(updatePayload))
                .andExpect(status().isForbidden());

        // 7. Owner CAN update it
        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json").content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Hijacked Name"));

        // 8. The owner cannot be removed as the last OWNER (business rule guard)
        String ownerUserId = extractOwnerUserId(ownerToken);
        mockMvc.perform(delete("/api/projects/" + projectId + "/members/" + ownerUserId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict());

        // 9. Contributor cannot delete the project
        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + contributorToken))
                .andExpect(status().isForbidden());

        // 10. Owner can delete (soft-delete) the project
        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // 11. Soft-deleted project is no longer retrievable (excluded by @SQLRestriction)
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    private String extractOwnerUserId(String ownerToken) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "owner@example.com", "password", "SecurePass1"))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("user").get("id").asText();
    }
}
