package com.agentic.platform.domain.repository;

import com.agentic.platform.domain.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, String> {
    List<StockTransaction> findByStatusOrderByCreatedAtDesc(StockTransaction.Status status);
    List<StockTransaction> findAllByOrderByCreatedAtDesc();
}
