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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressServiceImpl Unit Test Suite")
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User testUser;
    private User otherUser;
    private Address defaultAddress;
    private Address secondAddress;
    private AddressRequest addressRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("user10@example.com");

        otherUser = new User();
        otherUser.setId(20L);
        otherUser.setEmail("user20@example.com");

        defaultAddress = Address.builder()
                .id(101L)
                .fullName("Nguyen Van A")
                .phone("0987654321")
                .province("Hanoi")
                .district("Cau Giay")
                .ward("Dich Vong")
                .specificAddress("123 Xuan Thuy")
                .isDefault(true)
                .user(testUser)
                .build();

        secondAddress = Address.builder()
                .id(102L)
                .fullName("Nguyen Van A")
                .phone("0987654321")
                .province("Danang")
                .district("Hai Chau")
                .ward("Thach Thang")
                .specificAddress("456 Le Duan")
                .isDefault(false)
                .user(testUser)
                .build();

        addressRequest = new AddressRequest();
        addressRequest.setFullName("Nguyen Van A");
        addressRequest.setPhone("0987654321");
        addressRequest.setProvince("HCMC");
        addressRequest.setDistrict("District 1");
        addressRequest.setWard("Ben Nghe");
        addressRequest.setSpecificAddress("789 Nguyen Hue");
        addressRequest.setIsDefault(false);
    }

    // =========================================================================
    // AC-ADDR-01: Thêm địa chỉ mới (addAddress)
    // =========================================================================
    @Nested
    @DisplayName("AC-ADDR-01: Add Address Tests")
    class AddAddressTests {

        @Test
        @DisplayName("TC-ADDR-01 [Boundary]: First address added for user should automatically be marked as default")
        void addAddress_FirstAddress_AutoMarkedDefault() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.existsByUserId(10L)).thenReturn(false);

            MessageResponse response = addressService.addAddress(10L, addressRequest);

            assertNotNull(response);
            assertEquals("Address added successfully.", response.getMessage());
            assertTrue(addressRequest.getIsDefault());
            verify(addressRepository, times(1)).save(any(Address.class));
        }

        @Test
        @DisplayName("TC-ADDR-02 [Positive]: Adding subsequent address marked as default should unset existing default")
        void addAddress_SubsequentAddressAsDefault_UnsetsOldDefault() {
            addressRequest.setIsDefault(true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.existsByUserId(10L)).thenReturn(true);
            when(addressRepository.findByUserId(10L)).thenReturn(List.of(defaultAddress, secondAddress));

            MessageResponse response = addressService.addAddress(10L, addressRequest);

            assertNotNull(response);
            assertFalse(defaultAddress.getIsDefault());
            verify(addressRepository, times(1)).save(defaultAddress);
            verify(addressRepository, times(2)).save(any(Address.class));
        }

        @Test
        @DisplayName("TC-ADDR-03 [Negative]: Throw ResourceNotFoundException when user does not exist")
        void addAddress_UserNotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    addressService.addAddress(999L, addressRequest)
            );
            verify(addressRepository, never()).save(any(Address.class));
        }
    }

    // =========================================================================
    // AC-ADDR-02: Cập nhật địa chỉ (updateAddress)
    // =========================================================================
    @Nested
    @DisplayName("AC-ADDR-02: Update Address Tests")
    class UpdateAddressTests {

        @Test
        @DisplayName("TC-ADDR-04 [Positive]: Update non-default address successfully")
        void updateAddress_NonDefault_Success() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(102L)).thenReturn(Optional.of(secondAddress));

            addressRequest.setSpecificAddress("New Street 100");
            MessageResponse response = addressService.updateAddress(10L, 102L, addressRequest);

            assertNotNull(response);
            assertEquals("Address updated successfully.", response.getMessage());
            assertEquals("New Street 100", secondAddress.getSpecificAddress());
            verify(addressRepository, times(1)).save(secondAddress);
        }

        @Test
        @DisplayName("TC-ADDR-05 [Positive]: Update address to default unsets previous default")
        void updateAddress_SetToDefault_UnsetsPreviousDefault() {
            addressRequest.setIsDefault(true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(102L)).thenReturn(Optional.of(secondAddress));
            when(addressRepository.findByUserId(10L)).thenReturn(List.of(defaultAddress, secondAddress));

            MessageResponse response = addressService.updateAddress(10L, 102L, addressRequest);

            assertNotNull(response);
            assertFalse(defaultAddress.getIsDefault());
            assertTrue(secondAddress.getIsDefault());
            verify(addressRepository, times(1)).save(defaultAddress);
            verify(addressRepository, times(1)).save(secondAddress);
        }

        @Test
        @DisplayName("TC-ADDR-06 [Negative]: Throw BadRequestException when updating address belonging to another user")
        void updateAddress_ForbiddenUser_ThrowsBadRequest() {
            when(userRepository.findById(20L)).thenReturn(Optional.of(otherUser));
            when(addressRepository.findById(101L)).thenReturn(Optional.of(defaultAddress)); // Owned by 10L

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    addressService.updateAddress(20L, 101L, addressRequest)
            );
            assertThat(ex.getMessage()).contains("You don't have permission");
            verify(addressRepository, never()).save(any(Address.class));
        }

        @Test
        @DisplayName("TC-ADDR-07 [Negative]: Throw ResourceNotFoundException when address ID not found")
        void updateAddress_AddressNotFound_ThrowsException() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    addressService.updateAddress(10L, 999L, addressRequest)
            );
        }
    }

    // =========================================================================
    // AC-ADDR-03: Xóa địa chỉ (deleteAddress)
    // =========================================================================
    @Nested
    @DisplayName("AC-ADDR-03: Delete Address Tests")
    class DeleteAddressTests {

        @Test
        @DisplayName("TC-ADDR-08 [Positive]: Delete non-default address successfully")
        void deleteAddress_NonDefault_Success() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(102L)).thenReturn(Optional.of(secondAddress));

            MessageResponse response = addressService.deleteAddress(10L, 102L);

            assertNotNull(response);
            assertEquals("Address deleted successfully.", response.getMessage());
            verify(addressRepository, times(1)).delete(secondAddress);
        }

        @Test
        @DisplayName("TC-ADDR-09 [Negative/Boundary]: Throw BadRequestException when attempting to delete default address")
        void deleteAddress_DefaultAddress_ThrowsBadRequest() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(101L)).thenReturn(Optional.of(defaultAddress));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    addressService.deleteAddress(10L, 101L)
            );
            assertThat(ex.getMessage()).contains("You cannot delete your default address");
            verify(addressRepository, never()).delete(any(Address.class));
        }

        @Test
        @DisplayName("TC-ADDR-10 [Negative]: Throw BadRequestException when deleting address owned by someone else")
        void deleteAddress_ForbiddenUser_ThrowsBadRequest() {
            when(userRepository.findById(20L)).thenReturn(Optional.of(otherUser));
            when(addressRepository.findById(102L)).thenReturn(Optional.of(secondAddress)); // Owned by 10L

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    addressService.deleteAddress(20L, 102L)
            );
            assertThat(ex.getMessage()).contains("You don't have permission");
            verify(addressRepository, never()).delete(any(Address.class));
        }
    }

    // =========================================================================
    // AC-ADDR-04: Đặt địa chỉ mặc định (setDefaultAddress)
    // =========================================================================
    @Nested
    @DisplayName("AC-ADDR-04: Set Default Address Tests")
    class SetDefaultAddressTests {

        @Test
        @DisplayName("TC-ADDR-11 [Positive]: Set second address as default successfully unsets old default")
        void setDefaultAddress_Success() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(102L)).thenReturn(Optional.of(secondAddress));
            when(addressRepository.findByUserId(10L)).thenReturn(List.of(defaultAddress, secondAddress));

            MessageResponse response = addressService.setDefaultAddress(10L, 102L);

            assertNotNull(response);
            assertEquals("Default address updated.", response.getMessage());
            assertFalse(defaultAddress.getIsDefault());
            assertTrue(secondAddress.getIsDefault());
            verify(addressRepository, times(1)).save(defaultAddress);
            verify(addressRepository, times(1)).save(secondAddress);
        }

        @Test
        @DisplayName("TC-ADDR-12 [Boundary]: Setting an already default address returns informational message without modifying others")
        void setDefaultAddress_AlreadyDefault_ReturnsMessage() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.findById(101L)).thenReturn(Optional.of(defaultAddress));

            MessageResponse response = addressService.setDefaultAddress(10L, 101L);

            assertNotNull(response);
            assertEquals("This address is already the default.", response.getMessage());
            verify(addressRepository, never()).findByUserId(anyLong());
        }

        @Test
        @DisplayName("TC-ADDR-13 [Negative]: Throw BadRequestException when setting default on address owned by another user")
        void setDefaultAddress_ForbiddenUser_ThrowsBadRequest() {
            when(userRepository.findById(20L)).thenReturn(Optional.of(otherUser));
            when(addressRepository.findById(101L)).thenReturn(Optional.of(defaultAddress));

            assertThrows(BadRequestException.class, () ->
                    addressService.setDefaultAddress(20L, 101L)
            );
        }
    }

    // =========================================================================
    // AC-ADDR-05: Lấy danh sách địa chỉ người dùng (getMyAddresses)
    // =========================================================================
    @Nested
    @DisplayName("AC-ADDR-05: Get My Addresses Tests")
    class GetMyAddressesTests {

        @Test
        @DisplayName("TC-ADDR-14 [Positive]: Return all addresses for valid user")
        void getMyAddresses_Success() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(addressRepository.findByUserId(10L)).thenReturn(List.of(defaultAddress, secondAddress));

            List<AddressResponse> responses = addressService.getMyAddresses(10L);

            assertNotNull(responses);
            assertEquals(2, responses.size());
            assertEquals(101L, responses.get(0).getId());
            assertTrue(responses.get(0).getIsDefault());
            assertEquals(102L, responses.get(1).getId());
            assertFalse(responses.get(1).getIsDefault());
            verify(addressRepository, times(1)).findByUserId(10L);
        }

        @Test
        @DisplayName("TC-ADDR-15 [Negative]: Throw ResourceNotFoundException when user not found")
        void getMyAddresses_UserNotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    addressService.getMyAddresses(999L)
            );
            verify(addressRepository, never()).findByUserId(anyLong());
        }
    }
}
