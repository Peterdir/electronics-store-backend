package com.ecommerce.backend.modules.auth.service;

import com.ecommerce.backend.modules.auth.dto.request.RegisterRequest;
import com.ecommerce.backend.modules.auth.dto.request.ResendVerificationRequest;
import com.ecommerce.backend.modules.auth.dto.response.AuthResponse;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;

public interface AuthService {
    MessageResponse register(RegisterRequest request);
    AuthResponse verifyEmail(String token);
    MessageResponse resendVerification(ResendVerificationRequest request);
}
