package com.ecommerce.backend.common.utils;

import com.ecommerce.backend.common.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Utility class để validate file upload.
 * Dùng chung cho Brand logo, Product image, v.v.
 */
public final class FileValidator {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".webp"
    );

    private FileValidator() {
        // Prevent instantiation
    }

    /**
     * Validate file ảnh: kiểm tra null, định dạng, và dung lượng.
     *
     * @param file file cần validate
     * @throws BadRequestException nếu file không hợp lệ
     */
    public static void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required.");
        }

        // Kiểm tra content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Invalid image format. Allowed formats: JPG, JPEG, PNG, WebP.");
        }

        // Kiểm tra extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BadRequestException(
                        "Invalid image format. Allowed formats: JPG, JPEG, PNG, WebP.");
            }
        }

        // Kiểm tra dung lượng
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    "Image size exceeds the maximum allowed size of 5MB.");
        }
    }
}
