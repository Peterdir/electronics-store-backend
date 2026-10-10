package com.ecommerce.backend.modules.returnrequest.repository;

import com.ecommerce.backend.modules.returnrequest.entity.ReturnRequest;
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
}
