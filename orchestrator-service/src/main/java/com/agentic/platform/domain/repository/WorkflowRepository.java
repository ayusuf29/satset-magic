package com.agentic.platform.domain.repository;

import com.agentic.platform.domain.model.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowInstance, String> {
    List<WorkflowInstance> findAllByOrderByCreatedAtDesc();
}
