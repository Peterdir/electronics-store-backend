package com.ecommerce.backend.common.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Implementation tạm thời lưu file vào ổ đĩa local.
 * Khi tích hợp AWS S3, tạo class mới implement FileStorageService
 * và đánh @Primary hoặc xóa class này.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            Path uploadPath = Paths.get(uploadDir, folder);
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFilename = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath);

            return baseUrl + "/uploads/" + folder + "/" + storedFilename;
        } catch (IOException e) {
            throw new BadRequestException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isBlank()) return;

            // Trích xuất đường dẫn tương đối từ URL
            String relativePath = fileUrl.replace(baseUrl + "/", "");
            Path filePath = Paths.get(relativePath);

            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log warning nhưng không throw — xóa file thất bại không nên block luồng chính
        }
    }
}
