package com.agentic.platform.service;

import com.agentic.platform.domain.model.AgentEvent;
import com.agentic.platform.domain.model.WorkflowInstance;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentExecutionServiceImpl implements AgentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionServiceImpl.class);

    private final ChatLanguageModel chatLanguageModel;
    private final WorkflowEventPublisher eventPublisher;

    public AgentExecutionServiceImpl(ChatLanguageModel chatLanguageModel, WorkflowEventPublisher eventPublisher) {
        this.chatLanguageModel = chatLanguageModel;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String executePhaseAgent(WorkflowInstance workflow, String promptInstruction) {
        String phaseName = workflow.getCurrentState().name();
        log.info("Executing Agent AI service for workflow: {} | Phase: {}", workflow.getId(), phaseName);

        eventPublisher.publishEvent(AgentEvent.of(
                workflow.getId(),
                workflow.getCurrentState(),
                "AGENT_START",
                "Agent mulai mengeksekusi instruksi pada fase: " + phaseName
        ));

        try {
            SystemMessage systemMessage = SystemMessage.from(
                    "You are an enterprise autonomous software engineering agent in the Satset-Magic agentic platform. " +
                    "Follow clean architecture, RFC standards, and provide crisp, professional outputs."
            );

            UserMessage userMessage = UserMessage.from(
                    String.format(
                            "Task: %s\nDescription: %s\nCurrent Phase: %s\nContext: %s\nInstruction: %s",
                            workflow.getTaskName(),
                            workflow.getDescription(),
                            phaseName,
                            workflow.getPayloadJson() != null ? workflow.getPayloadJson() : "None",
                            promptInstruction
                    )
            );

            Response<AiMessage> response = chatLanguageModel.generate(List.of(systemMessage, userMessage));
            String output = response.content().text();

            eventPublisher.publishEvent(AgentEvent.of(
                    workflow.getId(),
                    workflow.getCurrentState(),
                    "AGENT_OUTPUT",
                    "Agent menyelesaikan analisis fase: " + phaseName
            ));

            return output;
        } catch (Exception ex) {
            log.error("Error executing Agent AI for workflow {}: {}", workflow.getId(), ex.getMessage(), ex);
            eventPublisher.publishEvent(AgentEvent.of(
                    workflow.getId(),
                    workflow.getCurrentState(),
                    "AGENT_ERROR",
                    "Agent menemui kendala: " + ex.getMessage()
            ));
            return "Execution failed: " + ex.getMessage();
        }
    }
}
