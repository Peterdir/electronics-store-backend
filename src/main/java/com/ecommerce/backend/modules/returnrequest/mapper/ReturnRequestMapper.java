package com.ecommerce.backend.modules.returnrequest.mapper;

import com.ecommerce.backend.modules.returnrequest.dto.response.ReturnRequestResponse;
import com.ecommerce.backend.modules.returnrequest.entity.ReturnRequest;
import org.springframework.stereotype.Component;

@Component
public class ReturnRequestMapper {

    public ReturnRequestResponse toResponse(ReturnRequest returnRequest) {
        if (returnRequest == null) {
            return null;
        }

        return ReturnRequestResponse.builder()
                .id(returnRequest.getId())
                .orderId(returnRequest.getOrder() != null ? returnRequest.getOrder().getId() : null)
                .orderItemId(returnRequest.getOrderItem() != null ? returnRequest.getOrderItem().getId() : null)
                .quantity(returnRequest.getQuantity())
                .reason(returnRequest.getReason())
                .description(returnRequest.getDescription())
                .status(returnRequest.getStatus())
                .imageUrls(returnRequest.getImageUrls())
                .createdAt(returnRequest.getCreatedAt())
                .updatedAt(returnRequest.getUpdatedAt())
                .build();
    }

}
