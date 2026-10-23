package com.agentic.platform.service;

import com.agentic.platform.service.phase.Phase4TddPlanningAgent;
import com.agentic.platform.service.phase.ProjectScaffoldingService;
import com.agentic.platform.service.phase.SdlcJournalService;
import com.agentic.platform.service.phase.SkillLoaderService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Phase4TddPlanningAgentTest {

    private SdlcJournalService journalService;
    private ProjectScaffoldingService scaffoldingService;
    private SkillLoaderService skillLoaderService;
    private Phase4TddPlanningAgent agent;

    @BeforeEach
    void setUp() {
        journalService = new SdlcJournalService();
        scaffoldingService = new ProjectScaffoldingService(journalService);
        skillLoaderService = new SkillLoaderService();
        ChatLanguageModel chatLanguageModel = messages -> null;
        agent = new Phase4TddPlanningAgent(chatLanguageModel, journalService, scaffoldingService, skillLoaderService);
    }

    @Test
    @DisplayName("Should detect truncated, short, or incomplete TDD plan outputs")
    void shouldDetectInvalidTddPlanOutput() {
        // Too few lines
        assertThat(agent.isInvalidTddPlanOutput("short text")).isTrue();

        // Missing required sections
        assertThat(agent.isInvalidTddPlanOutput("""
                line 1
                line 2
                line 3
                line 4
                line 5
                line 6
                line 7
                line 8
                line 9
                line 10
                line 11
                line 12
                line 13
                line 14
                line 15
                line 16
                line 17
                line 18
                line 19
                line 20
                line 21
                line 22
                line 23
                line 24
                line 25
                line 26
                """)).isTrue();

        // Unbalanced code fences
        String unbalanced = """
                # TDD Test Plan & Specifications
                ## 1. Test Suite Matrix
                Target Branch Coverage: >= 70%
                ```java
                package com.agentic.test;
                public class ItemTest {}
                """;
        assertThat(agent.isInvalidTddPlanOutput(unbalanced)).isTrue();

        // Abrupt cut-off ending
        String abruptEnding = """
                # TDD Test Plan & Specifications
                ## 1. Test Suite Matrix
                Target Branch Coverage: >= 70%
                ```java
                package com.agentic.test;
                public class ItemTest {}
                ```
                * Test usage values exactly at tier boundaries (e.g., 1
                """;
        assertThat(agent.isInvalidTddPlanOutput(abruptEnding)).isTrue();
    }

    @Test
    @DisplayName("Should validate complete TDD plan output containing JUnit 5 test class")
    void shouldValidateCompleteTddPlanOutput() {
        String validPlan = """
                # TDD Test Plan & Specifications (Phase 4 Output)
                **Target Branch Coverage:** >= 70%

                ## 1. Test Suite Matrix (Red-Green-Refactor)
                | Test Category | Target Component | Assertions | Target Coverage |
                | :--- | :--- | :--- | :--- |
                | Domain Unit Test | Item Aggregate | Valid invariants | 90% |

                ## 2. Invariant & Edge Case Boundaries
                - Null safety verification.

                ## 3. Concrete JUnit 5 Test Harness Code
                ```java
                package com.agentic.test.domain;
                import org.junit.jupiter.api.Test;
                public class ItemTest {
                    @Test
                    void verifyCreation() {}
                }
                ```

                ## 4. Test Execution Command
                `mvn test`
                line 21
                line 22
                line 23
                line 24
                line 25
                line 26
                """;

        assertThat(agent.isInvalidTddPlanOutput(validPlan)).isFalse();
    }
}
