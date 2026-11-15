package com.ecommerce.backend.common.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final Pattern STORED_IMAGE = Pattern.compile(
            "[a-zA-Z0-9_-]+/[a-fA-F0-9]{8}(?:-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}\\.(?i:jpg|jpeg|png|webp)");

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        String filename = file.getOriginalFilename();
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
        String storageKey = folder + "/" + UUID.randomUUID() + extension;
        Path target = resolveImage(storageKey);
        if (target == null) {
            throw new BadRequestException("Invalid image filename or storage location.");
        }

        String fileUrl = uploadUrlPrefix() + storageKey;
        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize();
            Path directory = Files.createDirectories(target.getParent());
            if (!directory.toRealPath().startsWith(root.toRealPath())) {
                throw new IOException("Image directory is outside the storage root.");
            }
            try (InputStream stream = file.getInputStream()) {
                Files.copy(stream, target);
            }
            return fileUrl;
        } catch (IOException ex) {
            deleteFile(fileUrl);
            throw new IllegalStateException("Failed to store the image.", ex);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        String prefix = uploadUrlPrefix();
        if (fileUrl == null || !fileUrl.startsWith(prefix)) {
            return;
        }

        Path target = resolveImage(fileUrl.substring(prefix.length()));
        if (target == null || !Files.isDirectory(target.getParent())) {
            return;
        }

        try {
            Path root = Path.of(uploadDir).toAbsolutePath().normalize().toRealPath();
            if (target.getParent().toRealPath().startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ex) {
            log.warn("Failed to delete stored image {}", fileUrl, ex);
        }
    }

    private String uploadUrlPrefix() {
        return baseUrl.replaceFirst("/+$", "") + "/uploads/";
    }

    private Path resolveImage(String storageKey) {
        if (!STORED_IMAGE.matcher(storageKey).matches()) {
            return null;
        }
        return Path.of(uploadDir).toAbsolutePath().normalize().resolve(storageKey);
    }
}
