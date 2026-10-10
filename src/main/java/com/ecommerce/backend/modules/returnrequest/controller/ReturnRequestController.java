package com.ecommerce.backend.modules.returnrequest.controller;

import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestCreateRequest;
import com.ecommerce.backend.modules.returnrequest.dto.response.ReturnRequestResponse;
import com.ecommerce.backend.modules.returnrequest.service.ReturnRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/return-requests")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    private Long extractUserId(Jwt jwt) {
        return Long.valueOf(Objects.requireNonNull(jwt.getSubject(), "JWT subject must not be null"));
    }

    /**
     * Khách hàng tạo yêu cầu hoàn hàng cho 1 sản phẩm
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReturnRequestResponse> createReturnRequest(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute ReturnRequestCreateRequest request) {
        Long userId = extractUserId(jwt);
        ReturnRequestResponse response = returnRequestService.createReturnRequest(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
