package com.minitrello.integration.task;

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

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end: register a user, build the full Workspace -> Project ->
 * Board -> BoardList -> Task hierarchy through real HTTP calls, then
 * exercise drag-and-drop reordering (within a list) and moving a task
 * across lists, asserting the resulting position ordering is correct —
 * this is the sharpest edge in the whole Task Module (Phase 2's
 * write-only-the-moved-row design), so it gets the most thorough
 * integration coverage.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskFlowIntegrationTest {

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
    void fullKanbanFlow_createHierarchyAndDragTasks() throws Exception {
        String token = registerAndGetAccessToken("kanban.user@example.com", "Kanban User");

        String workspaceId = postAndExtract("/api/workspaces", Map.of("name", "Acme"), token, "id");
        String projectId = postAndExtract("/api/workspaces/" + workspaceId + "/projects",
                Map.of("name", "Redesign", "description", "d"), token, "id");
        String boardId = postAndExtract("/api/projects/" + projectId + "/boards", Map.of("name", "Main Board"), token, "id");
        String todoListId = postAndExtract("/api/boards/" + boardId + "/lists", Map.of("name", "To Do"), token, "id");
        String doneListId = postAndExtract("/api/boards/" + boardId + "/lists", Map.of("name", "Done"), token, "id");

        // Create three tasks in the To Do list — they should append in order.
        String taskA = postAndExtract("/api/board-lists/" + todoListId + "/tasks",
                Map.of("title", "Task A", "priority", "HIGH"), token, "id");
        String taskB = postAndExtract("/api/board-lists/" + todoListId + "/tasks",
                Map.of("title", "Task B", "priority", "MEDIUM"), token, "id");
        String taskC = postAndExtract("/api/board-lists/" + todoListId + "/tasks",
                Map.of("title", "Task C", "priority", "LOW"), token, "id");

        assertOrder(todoListId, token, "Task A", "Task B", "Task C");

        // Drag Task C to the front (before Task A).
        Map<String, Object> moveToFront = new HashMap<>();
        moveToFront.put("targetBoardListId", todoListId);
        moveToFront.put("prevTaskId", null);
        moveToFront.put("nextTaskId", taskA);
        mockMvc.perform(patch("/api/tasks/" + taskC + "/position")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(moveToFront)))
                .andExpect(status().isOk());

        assertOrder(todoListId, token, "Task C", "Task A", "Task B");

        // Move Task B to the (empty) Done list.
        Map<String, Object> moveAcrossLists = new HashMap<>();
        moveAcrossLists.put("targetBoardListId", doneListId);
        moveAcrossLists.put("prevTaskId", null);
        moveAcrossLists.put("nextTaskId", null);
        mockMvc.perform(patch("/api/tasks/" + taskB + "/position")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(moveAcrossLists)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.boardListId").value(doneListId));

        assertOrder(todoListId, token, "Task C", "Task A");
        assertOrder(doneListId, token, "Task B");
    }

    private void assertOrder(String boardListId, String token, String... expectedTitlesInOrder) throws Exception {
        String body = mockMvc.perform(get("/api/board-lists/" + boardListId + "/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(body).get("data");
        org.assertj.core.api.Assertions.assertThat(data).hasSize(expectedTitlesInOrder.length);
        for (int i = 0; i < expectedTitlesInOrder.length; i++) {
            org.assertj.core.api.Assertions.assertThat(data.get(i).get("title").asText())
                    .isEqualTo(expectedTitlesInOrder[i]);
        }
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
