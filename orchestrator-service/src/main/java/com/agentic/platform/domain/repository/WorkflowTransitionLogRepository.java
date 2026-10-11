package com.agentic.platform.domain.repository;

import com.agentic.platform.domain.model.WorkflowTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTransitionLogRepository extends JpaRepository<WorkflowTransitionLog, Long> {
    List<WorkflowTransitionLog> findByWorkflowIdOrderByCreatedAtAsc(String workflowId);
}
