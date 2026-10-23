package com.agentic.platform.service;

import com.agentic.platform.service.phase.Phase5DevImplementationAgent;
import com.agentic.platform.service.phase.ProjectScaffoldingService;
import com.agentic.platform.service.phase.SdlcJournalService;
import com.agentic.platform.service.phase.SkillLoaderService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Phase5DevImplementationAgentTest {

    private SdlcJournalService journalService;
    private ProjectScaffoldingService scaffoldingService;
    private SkillLoaderService skillLoaderService;
    private Phase5DevImplementationAgent agent;

    @BeforeEach
    void setUp() {
        journalService = new SdlcJournalService();
        scaffoldingService = new ProjectScaffoldingService(journalService);
        skillLoaderService = new SkillLoaderService();
        ChatLanguageModel chatLanguageModel = messages -> null;
        agent = new Phase5DevImplementationAgent(chatLanguageModel, journalService, scaffoldingService, skillLoaderService);
    }

    @Test
    @DisplayName("Should detect truncated, short, or code-less implementation summary outputs")
    void shouldDetectInvalidImplementationSummaryOutput() {
        // Too short
        assertThat(agent.isInvalidImplementationSummaryOutput("short text")).isTrue();

        // Missing Java code blocks (like the old faulty fallback)
        String noCode = """
                # Implementation Summary & Code Scaffold
                **Status:** IMPLEMENTATION_COMPLETED
                ## 1. Architecture Implementation Overview
                - Pure domain core.
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
                line 27
                line 28
                line 29
                line 30
                line 31
                line 32
                line 33
                line 34
                line 35
                line 36
                """;
        assertThat(agent.isInvalidImplementationSummaryOutput(noCode)).isTrue();

        // Unbalanced code fences
        String unbalanced = """
                # Implementation Summary & Code Scaffold
                **Status:** IMPLEMENTATION_COMPLETED
                ## 1. Architecture Implementation Overview
                ```java
                package com.agentic.test;
                public class Item {}
                """;
        assertThat(agent.isInvalidImplementationSummaryOutput(unbalanced)).isTrue();
    }

    @Test
    @DisplayName("Should validate complete implementation summary containing Java code blocks")
    void shouldValidateCompleteImplementationSummary() {
        String validSummary = """
                # Implementation Summary & Code Scaffold (Phase 5 Output)
                **Status:** IMPLEMENTATION_COMPLETED

                ## 1. Architecture Implementation Overview
                - Domain core decoupled from frameworks.

                ## 2. Implemented Clean Architecture Source Code
                ```java
                package com.agentic.test.domain;
                public class Item {
                    private String id;
                    public String getId() { return id; }
                }
                ```

                ## 3. Implemented REST Endpoints & Error Handling
                - Endpoints exposed under `/api/items`.

                ## 4. Persistence & Concurrency Strategy
                - In-memory concurrent hash map adapter.
                line 21
                line 22
                line 23
                line 24
                line 25
                line 26
                line 27
                line 28
                line 29
                line 30
                line 31
                line 32
                line 33
                line 34
                line 35
                line 36
                """;

        assertThat(agent.isInvalidImplementationSummaryOutput(validSummary)).isFalse();
    }
}
