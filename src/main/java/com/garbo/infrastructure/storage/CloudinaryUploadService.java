package com.garbo.infrastructure.storage;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.garbo.api.exception.CollectionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
public class CloudinaryUploadService {
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryUploadService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public String uploadCompletionPhoto(MultipartFile file, Long offerId) {
        return uploadImage(
                file,
                "garbo/collection-completions",
                "offer-" + offerId + "-",
                "Completion photo is required");
    }

    public String uploadRequestPhoto(MultipartFile file, Long citizenId) {
        return uploadImage(
                file,
                "garbo/request-photos",
                "citizen-" + citizenId + "-",
                "Request photo is required");
    }

    public String uploadProfilePhoto(MultipartFile file, Long userId) {
        return uploadImage(
                file,
                "garbo/profile-photos",
                "user-" + userId + "-",
                "Profile photo is required");
    }

    public String uploadBinReportPhoto(MultipartFile file, Long binId) {
        return uploadImage(
                file,
                "garbo/bin-reports",
                "bin-" + binId + "-",
                "Bin report photo is required");
    }

    private String uploadImage(
            MultipartFile file,
            String folder,
            String publicIdPrefix,
            String missingPhotoMessage) {
        if (!isConfigured()) {
            throw new CollectionException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cloudinary is not configured on backend",
                    "CLOUDINARY_NOT_CONFIGURED");
        }

        validateImageFile(file, missingPhotoMessage);

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "public_id", publicIdPrefix + System.currentTimeMillis()));

            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new CollectionException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Image upload failed: secure URL not returned",
                        "UPLOAD_FAILED");
            }
            return secureUrl.toString();
        } catch (IOException ex) {
            throw new CollectionException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Image upload failed",
                    "UPLOAD_FAILED");
        }
    }

    private boolean isConfigured() {
        return !isBlank(cloudName) && !isBlank(apiKey) && !isBlank(apiSecret);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void validateImageFile(MultipartFile file, String missingPhotoMessage) {
        if (file == null || file.isEmpty()) {
            throw new CollectionException(HttpStatus.BAD_REQUEST, missingPhotoMessage, "VALIDATION_ERROR");
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw new CollectionException(HttpStatus.BAD_REQUEST, "Image file must be 10MB or smaller",
                    "VALIDATION_ERROR");
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        if (!isAllowedImage(contentType, fileName)) {
            throw new CollectionException(HttpStatus.BAD_REQUEST, "Only image files are allowed", "VALIDATION_ERROR");
        }
    }

    private boolean isAllowedImage(String contentType, String fileName) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }

        if (fileName == null) {
            return false;
        }

        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".heic")
                || lower.endsWith(".heif")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif");
    }
}
