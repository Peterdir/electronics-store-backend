package com.ecommerce.backend.modules.address.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.address.dto.request.AddressRequest;
import com.ecommerce.backend.modules.address.dto.response.AddressResponse;
import com.ecommerce.backend.modules.address.entity.Address;
import com.ecommerce.backend.modules.address.repository.AddressRepository;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService{

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public MessageResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found user."));

        boolean hasAddress = addressRepository.existsByUserId(userId);

        if (!hasAddress) {
            request.setIsDefault(true);
        }
        else if (request.getIsDefault() != null && request.getIsDefault()) {
            List<Address> addresses = addressRepository.findByUserId(userId);

            for (Address address : addresses) {
                if (address.getIsDefault() != null && address.getIsDefault()) {
                    address.setIsDefault(false);
                    addressRepository.save(address);
                }
            }
        }

        Address address = Address.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .specificAddress(request.getSpecificAddress())
                .isDefault(request.getIsDefault())
                .user(user)
                .build();

        addressRepository.save(address);

        return MessageResponse.builder()
                .message("Address added successfully.")
                .build();
    }

    @Override
    public MessageResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found user."));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found address."));

        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException("You don't have permission");
        }

        if (request.getIsDefault() != null && request.getIsDefault() && (address.getIsDefault() == null || !address.getIsDefault())) {
            List<Address> addresses = addressRepository.findByUserId(userId);

            for (Address address1 : addresses) {
                if (address1.getIsDefault() != null && address1.getIsDefault()) {
                    address1.setIsDefault(false);
                    addressRepository.save(address1);
                }
            }
        }

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setSpecificAddress(request.getSpecificAddress());
        address.setIsDefault(request.getIsDefault());

        addressRepository.save(address);

        return MessageResponse.builder()
                .message("Address updated successfully.")
                .build();
    }

    @Override
    public MessageResponse deleteAddress(Long userId, Long addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found user."));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found address."));

        // Không nên so sánh bằng User
        // Do cơ chế lazy loading của Hibernate, userId đã có sẵn đó từ trước nên không cần phải xuống DB tìm dữ liệu
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException("You don't have permission");
        }

        if (address.getIsDefault() != null && address.getIsDefault()) {
            throw new BadRequestException("You cannot delete your default address. Please set another address as default first.");
        }

        addressRepository.delete(address);

        return MessageResponse.builder()
                .message("Address deleted successfully.")
                .build();
    }

    @Override
    public MessageResponse setDefaultAddress(Long userId, Long addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found user."));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found address."));

        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException("You don't have permission");
        }

        if (address.getIsDefault() != null && address.getIsDefault()) {
            return MessageResponse.builder()
                    .message("This address is already the default.")
                    .build();
        }

        List<Address> addresses = addressRepository.findByUserId(userId);

        for (Address address1 : addresses) {
            if (address1.getIsDefault() != null && address1.getIsDefault()) {
                address1.setIsDefault(false);
                addressRepository.save(address1);
            }
        }

        address.setIsDefault(true);
        addressRepository.save(address);

        return MessageResponse.builder()
                .message("Default address updated.")
                .build();
    }

    @Override
    public List<AddressResponse> getMyAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found user."));

        List<Address> addresses = addressRepository.findByUserId(userId);

        return addresses.stream().map(address -> {
            return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .specificAddress(address.getSpecificAddress())
                .isDefault(address.getIsDefault())
                .build();
        }).collect(Collectors.toList());
    }
}
