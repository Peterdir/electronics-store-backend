package com.ecommerce.backend.common.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface trừu tượng hóa việc lưu trữ file.
 * Khi tích hợp AWS S3 hoặc Cloudinary, chỉ cần tạo class implement interface này.
 */
public interface FileStorageService {

    /**
     * Upload file và trả về URL public của file đã upload.
     *
     * @param file   file cần upload
     * @param folder thư mục/prefix trên cloud (vd: "brands", "products")
     * @return URL public của file
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * Xóa file trên cloud storage theo URL.
     *
     * @param fileUrl URL của file cần xóa
     */
    void deleteFile(String fileUrl);
}
