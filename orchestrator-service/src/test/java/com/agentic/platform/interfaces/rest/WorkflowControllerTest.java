package com.agentic.platform.interfaces.rest;

import com.agentic.platform.domain.model.WorkflowInstance;
import com.agentic.platform.domain.model.WorkflowState;
import com.agentic.platform.service.WorkflowOrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WorkflowOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        WorkflowController controller = new WorkflowController(orchestratorService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/workflow/start harus membuat workflow baru")
    void shouldStartWorkflowEndpoint() throws Exception {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id("wf-12345")
                .projectId("PRJ-PAYMENT")
                .clientId("CLIENT-ALPHA")
                .taskName("E2E Test Feature")
                .description("Fitur baru")
                .currentState(WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY)
                .build();

        when(orchestratorService.createWorkflow(any(), any(), any(), any(), any())).thenReturn(instance);

        mockMvc.perform(post("/api/workflow/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "projectId": "PRJ-PAYMENT",
                            "clientId": "CLIENT-ALPHA",
                            "taskName": "E2E Test Feature",
                            "description": "Fitur baru",
                            "initialPayload": "{}"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("wf-12345"))
                .andExpect(jsonPath("$.projectId").value("PRJ-PAYMENT"))
                .andExpect(jsonPath("$.clientId").value("CLIENT-ALPHA"))
                .andExpect(jsonPath("$.taskName").value("E2E Test Feature"))
                .andExpect(jsonPath("$.currentState").value("PHASE_0_REQUIREMENT_DISCOVERY"));
    }

    @Test
    @DisplayName("GET /api/workflow harus mengembalikan daftar seluruh workflow")
    void shouldGetAllWorkflowsEndpoint() throws Exception {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id("wf-list-1")
                .taskName("Task 1")
                .currentState(WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN)
                .build();

        when(orchestratorService.getAllWorkflows()).thenReturn(List.of(instance));

        mockMvc.perform(get("/api/workflow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("wf-list-1"))
                .andExpect(jsonPath("$[0].currentState").value("PHASE_1_ARCHITECTURE_AND_DESIGN"));
    }

    @Test
    @DisplayName("POST /api/workflow/{id}/approve harus memproses persetujuan manusia")
    void shouldApproveWorkflowEndpoint() throws Exception {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id("wf-app-1")
                .taskName("Task Approval")
                .currentState(WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN)
                .lastDecision("APPROVED")
                .build();

        when(orchestratorService.handleHumanDecision(eq("wf-app-1"), eq("APPROVE"), anyString(), anyString()))
                .thenReturn(instance);

        mockMvc.perform(post("/api/workflow/wf-app-1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "comment": "Approved by Tech Lead",
                            "actor": "LEAD_ARCHITECT"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("wf-app-1"))
                .andExpect(jsonPath("$.lastDecision").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/workflow/{id}/council-review harus memproses consensus vote dari 3 pilar council")
    void shouldSubmitCouncilReviewEndpoint() throws Exception {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id("wf-council-1")
                .taskName("Council Review Task")
                .currentState(WorkflowState.PHASE_7_QA_VERIFICATION)
                .coveragePercentage(88.0)
                .architectApproved(true)
                .securityApproved(true)
                .performanceApproved(true)
                .build();

        when(orchestratorService.handleCouncilReviewResult(eq("wf-council-1"), eq(true), eq(true), eq(true), eq(88.0), anyString()))
                .thenReturn(instance);

        mockMvc.perform(post("/api/workflow/wf-council-1/council-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "architectApproved": true,
                            "securityApproved": true,
                            "performanceApproved": true,
                            "coveragePercentage": 88.0,
                            "feedback": "All council pillars signed off."
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("wf-council-1"))
                .andExpect(jsonPath("$.architectApproved").value(true))
                .andExpect(jsonPath("$.securityApproved").value(true))
                .andExpect(jsonPath("$.performanceApproved").value(true))
                .andExpect(jsonPath("$.coveragePercentage").value(88.0))
                .andExpect(jsonPath("$.currentState").value("PHASE_7_QA_VERIFICATION"));
    }

    @Test
    @DisplayName("DELETE /api/workflow harus menghapus seluruh workflow dan transition logs")
    void shouldDeleteAllWorkflowsEndpoint() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/workflow"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(orchestratorService).deleteAllWorkflows();
    }
}
