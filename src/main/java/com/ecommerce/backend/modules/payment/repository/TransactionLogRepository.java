package com.ecommerce.backend.modules.payment.repository;

import com.ecommerce.backend.modules.payment.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    List<TransactionLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByVnpTxnRefAndSuccessTrue(String vnpTxnRef);
}
