package com.ecommerce.backend.modules.dashboard.controller;

import com.ecommerce.backend.modules.dashboard.dto.request.DashboardFilterRequest;
import com.ecommerce.backend.modules.dashboard.dto.response.DashboardOverviewResponse;
import com.ecommerce.backend.modules.dashboard.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardOverviewResponse> getDashboardOverview(
            @Valid @ModelAttribute DashboardFilterRequest request
    ) {
        return ResponseEntity.ok(dashboardService.getDashboardOverview(request));
    }
}
