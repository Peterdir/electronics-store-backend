package com.ecommerce.backend.common.utils;

import com.ecommerce.backend.common.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class FileValidator {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String INVALID_FORMAT = "Invalid image format. Allowed formats: JPG, JPEG, PNG, WebP.";
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    private FileValidator() {
    }

    public static void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Image size exceeds the maximum allowed size of 5MB.");
        }

        String filename = file.getOriginalFilename();
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
        String expectedMime = switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            default -> throw new BadRequestException(INVALID_FORMAT);
        };
        if (!expectedMime.equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException(INVALID_FORMAT);
        }

        try (InputStream stream = file.getInputStream()) {
            byte[] header = stream.readNBytes(16);
            boolean matches = switch (expectedMime) {
                case "image/jpeg" -> header.length >= 3 && header[0] == (byte) 0xFF
                        && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF;
                case "image/png" -> header.length >= 8
                        && Arrays.equals(header, 0, 8, PNG_SIGNATURE, 0, 8);
                case "image/webp" -> header.length >= 16
                        && hasTag(header, 0, "RIFF") && hasTag(header, 8, "WEBP")
                        && (hasTag(header, 12, "VP8 ") || hasTag(header, 12, "VP8L")
                            || hasTag(header, 12, "VP8X"));
                default -> false;
            };
            if (!matches) {
                throw new BadRequestException(INVALID_FORMAT);
            }
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the image file.");
        }
    }

    private static boolean hasTag(byte[] header, int offset, String tag) {
        return tag.equals(new String(header, offset, 4, StandardCharsets.US_ASCII));
    }
}
