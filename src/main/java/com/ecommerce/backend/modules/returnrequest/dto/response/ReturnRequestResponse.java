package com.ecommerce.backend.modules.returnrequest.dto.response;

import com.ecommerce.backend.modules.returnrequest.enums.ReturnStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestResponse {
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderItemId;

    private Integer quantity;
    
    private String reason;
    
    private String description;
    
    private String adminNote;
    
    private ReturnStatus status;
    
    private List<String> imageUrls;
    
    private Instant createdAt;
    
    private Instant updatedAt;
}
