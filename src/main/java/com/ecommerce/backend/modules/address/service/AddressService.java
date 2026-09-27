package com.ecommerce.backend.modules.address.service;

import com.ecommerce.backend.modules.address.dto.request.AddressRequest;
import com.ecommerce.backend.modules.address.dto.response.AddressResponse;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;

import java.util.List;

public interface AddressService {
    MessageResponse addAddress(Long userId, AddressRequest request);

    MessageResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    MessageResponse deleteAddress(Long userId, Long addressId);

    MessageResponse setDefaultAddress(Long userId, Long addressId);

    List<AddressResponse> getMyAddresses(Long userId);
}
