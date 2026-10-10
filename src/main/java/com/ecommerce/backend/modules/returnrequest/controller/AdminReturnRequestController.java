package com.ecommerce.backend.modules.returnrequest.controller;

import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestProcessRequest;
import com.ecommerce.backend.modules.returnrequest.dto.response.ReturnRequestResponse;
import com.ecommerce.backend.modules.returnrequest.enums.ReturnStatus;
import com.ecommerce.backend.modules.returnrequest.service.ReturnRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/return-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @GetMapping
    public ResponseEntity<Page<ReturnRequestResponse>> getReturnRequestsByStatus(
            @RequestParam(defaultValue = "PENDING") ReturnStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        Page<ReturnRequestResponse> response = returnRequestService.getReturnRequestsByStatus(status, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/process")
    public ResponseEntity<ReturnRequestResponse> processReturnRequest(
            @PathVariable Long id,
            @Valid @RequestBody ReturnRequestProcessRequest request) {
            
        ReturnRequestResponse response = returnRequestService.processReturnRequest(id, request);
        return ResponseEntity.ok(response);
    }
}
