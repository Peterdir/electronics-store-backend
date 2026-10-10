package com.ecommerce.backend.modules.returnrequest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ReturnRequestCreateRequest {

    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String description;

    private List<MultipartFile> images;
}
