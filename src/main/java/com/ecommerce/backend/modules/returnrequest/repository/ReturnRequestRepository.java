package com.ecommerce.backend.modules.returnrequest.repository;

import com.ecommerce.backend.modules.returnrequest.entity.ReturnRequest;
import com.ecommerce.backend.modules.returnrequest.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    List<ReturnRequest> findByUserId(Long userId);
    
    List<ReturnRequest> findByOrderId(Long orderId);

    Optional<ReturnRequest> findByOrderItemId(Long orderItemId);

    boolean existsByOrderItemId(Long orderItemId);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);
}
