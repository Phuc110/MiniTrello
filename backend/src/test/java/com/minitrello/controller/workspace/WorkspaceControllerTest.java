package com.minitrello.controller.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minitrello.application.workspace.WorkspaceService;
import com.minitrello.application.workspace.dto.WorkspaceResponse;
import com.minitrello.domain.shared.exception.ResourceNotFoundException;
import com.minitrello.domain.user.SystemRole;
import com.minitrello.domain.user.User;
import com.minitrello.infrastructure.security.CustomUserPrincipal;
import com.minitrello.presentation.advice.GlobalExceptionHandler;
import com.minitrello.presentation.controller.WorkspaceController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A true controller-layer test: no Spring context, no database, no
 * security filter chain — just the controller wired to a mocked service,
 * exercising request validation, status codes, and the ApiResponse
 * envelope. This is deliberately a different test category from the
 * Testcontainers-backed integration tests (which exercise the real
 * HTTP -> security -> service -> DB path end to end); this one is fast
 * and isolates "did the controller layer do its job" specifically.
 *
 * @AuthenticationPrincipal is resolved via Spring Security's own
 * AuthenticationPrincipalArgumentResolver reading a manually-populated
 * SecurityContext — this avoids needing the full JwtAuthenticationFilter
 * chain just to exercise the controller.
 */
class WorkspaceControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CustomUserPrincipal principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        User user = User.builder().id(UUID.randomUUID()).email("test@example.com")
                .fullName("Test User").systemRole(SystemRole.MEMBER).build();
        principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        mockMvc = MockMvcBuilders.standaloneSetup(new WorkspaceController(workspaceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_withBlankName_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("name", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void create_withValidName_returns201WithEnvelope() throws Exception {
        WorkspaceResponse response = new WorkspaceResponse(
                UUID.randomUUID(), "Acme", "acme", principal.getId(), null, true);
        when(workspaceService.createWorkspace(eq(principal.getId()), any())).thenReturn(response);

        mockMvc.perform(post("/api/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("name", "Acme"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("acme"));
    }

    @Test
    void getById_whenNotFound_returns404ViaGlobalExceptionHandler() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(workspaceService.getWorkspace(eq(workspaceId), eq(principal.getId())))
                .thenThrow(new ResourceNotFoundException("Workspace", workspaceId));

        mockMvc.perform(get("/api/workspaces/" + workspaceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
