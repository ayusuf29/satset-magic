package com.agentic.platform.service.lifecycle;

import com.agentic.platform.domain.model.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhaseExecutionManagerTest {

    private final List<String> lifecycleEvents = new ArrayList<>();
    private PhaseExecutionManager executionManager;

    @BeforeEach
    void setUp() {
        lifecycleEvents.clear();
        PhaseLifecycleObserver testObserver = new PhaseLifecycleObserver() {
            @Override
            public void onPreTask(PhaseContext context) {
                lifecycleEvents.add("PRE-TASK:" + context.getPhase().name());
            }

            @Override
            public void onTaskProgress(PhaseContext context, int progressPercentage, String currentActivity) {
                lifecycleEvents.add("PROGRESS:" + progressPercentage + "%:" + currentActivity);
            }

            @Override
            public void onPostTask(PhaseContext context, PhaseResult result) {
                lifecycleEvents.add("POST-TASK:" + result.getStatus() + ":" + result.getProducedArtifacts().size());
            }

            @Override
            public void onTaskFailed(PhaseContext context, Throwable throwable) {
                lifecycleEvents.add("FAILED:" + throwable.getMessage());
            }
        };

        executionManager = new PhaseExecutionManager(List.of(testObserver));
    }

    @Test
    @DisplayName("Harus mengeksekusi lifecycle Pre-Task -> On-Task Progress -> Post-Task secara berurutan")
    void shouldExecuteLifecycleInOrder() {
        PhaseContext context = PhaseContext.of(
                "wf-test-lifecycle",
                "PRJ-TEST-01",
                "Lifecycle Test Task",
                "Testing Observer Flow",
                WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN,
                "Architect Agent",
                "c4-model-designer",
                "Hexagonal Ports & Adapters"
        );

        PhaseResult result = executionManager.executePhase(context, progress -> {
            progress.accept(new PhaseExecutionManager.ProgressUpdate(50, "Generating C4 Diagram..."));
            return PhaseResult.success(
                    WorkflowState.PHASE_1_ARCHITECTURE_AND_DESIGN,
                    "Rationale: Isolating domain core",
                    "C4 Model generated",
                    List.of("architecture/C4_MODEL.md"),
                    120L
            );
        });

        assertNotNull(result);
        assertEquals(PhaseResult.Status.SUCCESS, result.getStatus());
        assertEquals(3, lifecycleEvents.size());
        assertEquals("PRE-TASK:PHASE_1_ARCHITECTURE_AND_DESIGN", lifecycleEvents.get(0));
        assertEquals("PROGRESS:50%:Generating C4 Diagram...", lifecycleEvents.get(1));
        assertEquals("POST-TASK:SUCCESS:1", lifecycleEvents.get(2));
    }

    @Test
    @DisplayName("Harus memicu onTaskFailed saat terjadi exception di dalam task function")
    void shouldTriggerFailedHookOnException() {
        PhaseContext context = PhaseContext.of(
                "wf-test-fail",
                "PRJ-TEST-02",
                "Failing Task",
                "Testing Error Handling",
                WorkflowState.PHASE_4_TDD_PLANNING,
                "TDD Agent",
                "tdd-orchestrator",
                "TDD Specs"
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            executionManager.executePhase(context, progress -> {
                throw new IllegalStateException("Database unreachable");
            });
        });

        assertEquals("Database unreachable", ex.getMessage());
        assertTrue(lifecycleEvents.contains("PRE-TASK:PHASE_4_TDD_PLANNING"));
        assertTrue(lifecycleEvents.contains("FAILED:Database unreachable"));
    }
}
