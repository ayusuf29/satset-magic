package com.agentic.platform.domain.repository;

import com.agentic.platform.domain.model.WorkflowTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WorkflowTransitionLogRepository extends JpaRepository<WorkflowTransitionLog, Long> {
    List<WorkflowTransitionLog> findByWorkflowIdOrderByCreatedAtAsc(String workflowId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WorkflowTransitionLog l WHERE l.workflowId = :workflowId")
    void deleteByWorkflowId(@Param("workflowId") String workflowId);
}

