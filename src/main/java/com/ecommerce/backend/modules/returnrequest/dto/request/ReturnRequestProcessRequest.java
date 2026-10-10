package com.ecommerce.backend.modules.returnrequest.dto.request;

import com.ecommerce.backend.modules.returnrequest.enums.ReturnStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnRequestProcessRequest {
    
    @NotNull(message = "Return status is required")
    private ReturnStatus status;
    
    private String rejectReason; 
}
