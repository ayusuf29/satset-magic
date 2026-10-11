package com.agentic.platform.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowStateTest {

    @Test
    @DisplayName("Harus menandai state gerbang persetujuan manusia dengan benar")
    void shouldIdentifyHumanGateStates() {
        assertTrue(WorkflowState.PHASE_0_WAITING_HUMAN_SIGN_OFF.isHumanGate());
        assertTrue(WorkflowState.PHASE_6_DEADLOCK_WAITING_HUMAN.isHumanGate());
        assertTrue(WorkflowState.PHASE_8_READY_FOR_PRODUCTION_GATE.isHumanGate());

        assertFalse(WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY.isHumanGate());
        assertFalse(WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN.isHumanGate());
        assertFalse(WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION.isHumanGate());
        assertFalse(WorkflowState.PHASE_6_CODE_REVIEW.isHumanGate());
        assertFalse(WorkflowState.PHASE_9_RELEASED.isHumanGate());
    }

    @Test
    @DisplayName("Harus menghasilkan urutan transisi sekuensial yang konsisten")
    void shouldTransitionSequentially() {
        assertEquals(WorkflowState.PHASE_0_WAITING_HUMAN_SIGN_OFF,
                WorkflowState.PHASE_0_REQUIREMENT_DISCOVERY.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN,
                WorkflowState.PHASE_0_WAITING_HUMAN_SIGN_OFF.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_2_BACKLOG_GENERATION,
                WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_3_TASK_DISPATCHING,
                WorkflowState.PHASE_2_BACKLOG_GENERATION.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_4_TDD_PLANNING,
                WorkflowState.PHASE_3_TASK_DISPATCHING.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION,
                WorkflowState.PHASE_4_TDD_PLANNING.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_6_CODE_REVIEW,
                WorkflowState.PHASE_5_DEVELOPMENT_IMPLEMENTATION.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_7_QA_VERIFICATION,
                WorkflowState.PHASE_6_CODE_REVIEW.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_8_READY_FOR_PRODUCTION_GATE,
                WorkflowState.PHASE_7_QA_VERIFICATION.getNextDefaultState());
        assertEquals(WorkflowState.PHASE_9_RELEASED,
                WorkflowState.PHASE_8_READY_FOR_PRODUCTION_GATE.getNextDefaultState());
    }
}
