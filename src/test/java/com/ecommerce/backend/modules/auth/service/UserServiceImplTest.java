package com.ecommerce.backend.modules.auth.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.ecommerce.backend.modules.auth.dto.request.UpdateProfileRequest;
import com.ecommerce.backend.modules.auth.dto.response.UserAdminResponse;
import com.ecommerce.backend.modules.auth.dto.response.UserProfileResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.enums.Role;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Test Suite")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@gmail.com");
        mockUser.setFullName("Nguyen Van A");
        mockUser.setPhone("0987654321");
        mockUser.setRole(Role.CUSTOMER);
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setPassword("hashedOldPassword");

        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("oldPassword");
        changePasswordRequest.setNewPassword("NewPassword@123");
        changePasswordRequest.setConfirmPassword("NewPassword@123");
    }

    // =========================================================================
    // AC-USER-01: Tìm kiếm & phân trang người dùng (getAdminUsers)
    // =========================================================================
    @Nested
    @DisplayName("AC-USER-01: Get Admin Users Tests")
    class GetAdminUsersTests {

        @Test
        @DisplayName("TC-USER-01 [Positive]: Should return paginated users for admin")
        void getAdminUsers_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(mockUser));

            when(userRepository.searchUsers("test", pageable)).thenReturn(page);

            Page<UserAdminResponse> result = userService.getAdminUsers("test", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            UserAdminResponse item = result.getContent().get(0);
            assertEquals(1L, item.getId());
            assertEquals("Nguyen Van A", item.getFullName());
            assertEquals("test@gmail.com", item.getEmail());
            assertEquals(Role.CUSTOMER, item.getRole());
            assertEquals(UserStatus.ACTIVE, item.getStatus());
            verify(userRepository, times(1)).searchUsers("test", pageable);
        }
    }

    // =========================================================================
    // AC-USER-02: Thay đổi trạng thái tài khoản (changeUserStatus)
    // =========================================================================
    @Nested
    @DisplayName("AC-USER-02: Change User Status Tests")
    class ChangeUserStatusTests {

        @Test
        @DisplayName("TC-USER-02 [Positive]: Should change user status successfully")
        void changeUserStatus_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            userService.changeUserStatus(1L, UserStatus.INACTIVE);

            assertEquals(UserStatus.INACTIVE, mockUser.getStatus());
            verify(userRepository, times(1)).save(mockUser);
        }

        @Test
        @DisplayName("TC-USER-03 [Negative]: Should throw ResourceNotFoundException when user not found")
        void changeUserStatus_NotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.changeUserStatus(999L, UserStatus.INACTIVE)
            );
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // AC-USER-03: Xem chi tiết tài khoản (getUserDetails)
    // =========================================================================
    @Nested
    @DisplayName("AC-USER-03: Get User Details Tests")
    class GetUserDetailsTests {

        @Test
        @DisplayName("TC-USER-04 [Positive]: Should return user admin details")
        void getUserDetails_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            UserAdminResponse response = userService.getUserDetails(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("Nguyen Van A", response.getFullName());
        }

        @Test
        @DisplayName("TC-USER-05 [Negative]: Should throw ResourceNotFoundException when user not found")
        void getUserDetails_NotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.getUserDetails(999L)
            );
        }
    }

    // =========================================================================
    // AC-USER-04: Xem hồ sơ cá nhân (getUserProfile)
    // =========================================================================
    @Nested
    @DisplayName("AC-USER-04: Get User Profile Tests")
    class GetUserProfileTests {

        @Test
        @DisplayName("TC-USER-06 [Positive]: Should return user profile")
        void getUserProfile_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            UserProfileResponse response = userService.getUserProfile(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("Nguyen Van A", response.getFullName());
            assertEquals("test@gmail.com", response.getEmail());
            assertEquals("0987654321", response.getPhone());
        }

        @Test
        @DisplayName("TC-USER-07 [Negative]: Should throw ResourceNotFoundException when user not found")
        void getUserProfile_NotFound_ThrowsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.getUserProfile(999L)
            );
        }
    }

    // =========================================================================
    // AC-USER-05: Cập nhật thông tin cá nhân (updateUserProfile)
    // =========================================================================
    @Nested
    @DisplayName("AC-USER-05: Update User Profile Tests")
    class UpdateUserProfileTests {

        @Test
        @DisplayName("TC-USER-08 [Positive]: Should update profile successfully")
        void updateUserProfile_Success() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName("Nguyen Van B");
            request.setPhone("0912345678");

            when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

            userService.updateUserProfile(1L, request);

            assertEquals("Nguyen Van B", mockUser.getFullName());
            assertEquals("0912345678", mockUser.getPhone());
            verify(userRepository, times(1)).save(mockUser);
        }

        @Test
        @DisplayName("TC-USER-09 [Negative]: Should throw ResourceNotFoundException when user not found")
        void updateUserProfile_NotFound_ThrowsException() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.updateUserProfile(999L, request)
            );
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // AC-USER-06: Đổi mật khẩu (changePassword)
    // =========================================================================
    @Nested
    @DisplayName("AC-USER-06: Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("TC-USER-10 [Positive]: Should change password successfully")
        void changePassword_Success() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("oldPassword", "hashedOldPassword")).thenReturn(true);
            when(passwordEncoder.encode("NewPassword@123")).thenReturn("hashedNewPassword");

            userService.changePassword("test@gmail.com", changePasswordRequest);

            assertEquals("hashedNewPassword", mockUser.getPassword());
            verify(userRepository, times(1)).save(mockUser);
        }

        @Test
        @DisplayName("TC-USER-11 [Negative]: Throw BadRequestException when current password does not match")
        void changePassword_WrongCurrentPassword() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    userService.changePassword("test@gmail.com", changePasswordRequest)
            );
            assertThat(ex.getMessage()).isEqualTo("Incorrect current password.");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-USER-12 [Negative/Boundary]: Throw BadRequestException when new password does not match confirm password")
        void changePassword_PasswordsDoNotMatch() {
            changePasswordRequest.setConfirmPassword("DifferentPassword@123");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    userService.changePassword("test@gmail.com", changePasswordRequest)
            );
            assertThat(ex.getMessage()).isEqualTo("Passwords do not match.");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-USER-13 [Negative]: Throw ResourceNotFoundException when email not found")
        void changePassword_UserNotFound() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    userService.changePassword("unknown@gmail.com", changePasswordRequest)
            );
            verify(userRepository, never()).save(any());
        }
    }
}
