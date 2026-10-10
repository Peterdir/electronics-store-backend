package com.ecommerce.backend.modules.returnrequest.service;

import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestCreateRequest;
import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestProcessRequest;
import com.ecommerce.backend.modules.returnrequest.dto.response.ReturnRequestResponse;
import com.ecommerce.backend.modules.returnrequest.enums.ReturnStatus;
import org.springframework.data.domain.Page;

public interface ReturnRequestService {

    ReturnRequestResponse createReturnRequest(Long userId, ReturnRequestCreateRequest request);

    Page<ReturnRequestResponse> getReturnRequestsByStatus(ReturnStatus status, int page, int size);

    ReturnRequestResponse processReturnRequest(Long id, ReturnRequestProcessRequest request);
}
