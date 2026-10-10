package com.ecommerce.backend.modules.returnrequest.service;

import com.ecommerce.backend.modules.returnrequest.dto.request.ReturnRequestCreateRequest;
import com.ecommerce.backend.modules.returnrequest.dto.response.ReturnRequestResponse;

public interface ReturnRequestService {

    ReturnRequestResponse createReturnRequest(Long userId, ReturnRequestCreateRequest request);
    
}
