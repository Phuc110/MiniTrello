package com.minitrello.integration.board;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression guard for the @SQLRestriction soft-delete bug: when
 * {@code @SQLRestriction("deleted_at IS NULL")} lived only on the
 * {@code @MappedSuperclass}, Hibernate silently ignored it, so every
 * SELECT leaked soft-deleted rows back into results (a deleted list
 * kept showing up in GET /boards/{id}/lists). These annotations must
 * stay on each concrete entity — if anyone removes them again, this
 * test fails.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BoardListSoftDeleteIntegrationTest {

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
    void deleteList_softDeletedListNeverReappearsInBoardLists() throws Exception {
        String token = registerAndGetAccessToken("softdelete.user@example.com", "Soft Delete User");

        String workspaceId = postAndExtract("/api/workspaces", Map.of("name", "Cleanup WS"), token, "id");
        String boardId = postAndExtract("/api/workspaces/" + workspaceId + "/boards",
                Map.of("name", "Cleanup Board"), token, "id");

        // A fresh board auto-generates exactly 3 lists.
        JsonNode before = getLists(boardId, token);
        assertThat(before).hasSize(3);
        String doneListId = before.get(2).get("id").asText();
        assertThat(before.get(2).get("name").asText()).isEqualTo("Done");

        // Give the list real content so the delete is not trivially empty.
        postAndExtract("/api/board-lists/" + doneListId + "/tasks",
                Map.of("title", "Doomed Task", "priority", "HIGH"), token, "id");

        mockMvc.perform(delete("/api/board-lists/" + doneListId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // The row is still physically present (soft delete), but every read
        // path must filter it out via the entity-level @SQLRestriction.
        JsonNode after = getLists(boardId, token);
        assertThat(after).hasSize(2);
        assertThat(after.get(0).get("name").asText()).isEqualTo("To Do");
        assertThat(after.get(1).get("name").asText()).isEqualTo("In Progress");
        for (JsonNode list : after) {
            assertThat(list.get("id").asText()).isNotEqualTo(doneListId);
        }

        // Resolving the deleted list directly (e.g. listing its tasks) must
        // 404 instead of resurrecting it from the leaked row.
        mockMvc.perform(get("/api/board-lists/" + doneListId + "/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private JsonNode getLists(String boardId, String token) throws Exception {
        String body = mockMvc.perform(get("/api/boards/" + boardId + "/lists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    private String registerAndGetAccessToken(String email, String fullName) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "SecurePass1", "fullName", fullName));
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("accessToken").asText();
    }

    /** Helper: POST a JSON body with auth, and pull one field out of the response's `data` object. */
    private String postAndExtract(String url, Map<String, Object> body, String token, String extractField) throws Exception {
        String responseBody = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("data").get(extractField).asText();
    }
}
