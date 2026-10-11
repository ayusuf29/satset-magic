package com.agentic.platform.domain.repository;

import com.agentic.platform.domain.model.ApprovalAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalAuditLogRepository extends JpaRepository<ApprovalAuditLog, Long> {
    List<ApprovalAuditLog> findByTransactionIdOrderByDecidedAtAsc(String transactionId);
    List<ApprovalAuditLog> findAllByOrderByDecidedAtDesc();
}
