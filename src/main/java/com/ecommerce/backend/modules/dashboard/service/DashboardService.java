package com.ecommerce.backend.modules.dashboard.service;

import com.ecommerce.backend.modules.dashboard.dto.request.DashboardFilterRequest;
import com.ecommerce.backend.modules.dashboard.dto.response.DashboardOverviewResponse;

public interface DashboardService {

    DashboardOverviewResponse getDashboardOverview(DashboardFilterRequest request);
}
