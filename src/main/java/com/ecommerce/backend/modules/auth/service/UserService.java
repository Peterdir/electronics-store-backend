package com.ecommerce.backend.modules.auth.service;

import com.ecommerce.backend.modules.auth.dto.request.UpdateProfileRequest;
import com.ecommerce.backend.modules.auth.dto.response.UserAdminResponse;
import com.ecommerce.backend.modules.auth.dto.response.UserProfileResponse;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    // Lấy danh sách use có phân trang
    Page<UserAdminResponse> getAdminUsers(String keyword, Pageable pageable);

    // Thay đổi trạng thái của một user
    void changeUserStatus(Long id, UserStatus status);

    // Có thể tạo thêm UserDetailResponse
    UserAdminResponse getUserDetails(Long id);

    // Lấy thông tin cá nhân
    UserProfileResponse getUserProfile(Long id);

    // Cập nhật thông tin cá nhân
    void updateUserProfile(Long id, UpdateProfileRequest request);
}
