package com.garbo.infrastructure.storage;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.garbo.api.exception.CollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryUploadService {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryUploadService.class);
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

    public String uploadBinReportPhoto(
            byte[] fileBytes,
            String originalFilename,
            String contentType,
            Long binId) {
        return uploadImageBytes(
                fileBytes,
                originalFilename,
                contentType,
                "garbo/bin-reports",
                "bin-" + binId + "-",
                "Bin report photo is required");
    }

    public String uploadNicPhoto(MultipartFile file) {
        return uploadImage(
                file,
                "garbo/nic-photos",
                "nic-" + System.currentTimeMillis() + "-",
                "NIC photo is required");
    }

    public String uploadEventImage(MultipartFile file) {
        return uploadImage(
                file,
                "garbo/events",
                "event-" + System.currentTimeMillis() + "-",
                "Event image is required");
    }

    public String uploadBinSuggestionPhoto(MultipartFile file) {
        return uploadImage(
                file,
                "garbo/bin-suggestions",
                "bin-suggestion-" + System.currentTimeMillis() + "-",
                "Bin suggestion photo is required");
    }

    public String uploadBinSuggestionPhoto(
            byte[] fileBytes,
            String originalFilename,
            String contentType) {
        return uploadImageBytes(
                fileBytes,
                originalFilename,
                contentType,
                "garbo/bin-suggestions",
                "bin-suggestion-" + System.currentTimeMillis() + "-",
                "Bin suggestion photo is required");
    }

    public String uploadComplaintPhoto(MultipartFile file) {
        return uploadImage(
                file,
                "garbo/complaints",
                "complaint-" + System.currentTimeMillis() + "-",
                "Complaint photo is required");
    }

    public String uploadComplaintPhoto(
            byte[] fileBytes,
            String originalFilename,
            String contentType) {
        return uploadImageBytes(
                fileBytes,
                originalFilename,
                contentType,
                "garbo/complaints",
                "complaint-" + System.currentTimeMillis() + "-",
                "Complaint photo is required");
    }

    private String uploadImage(
            MultipartFile file,
            String folder,
            String publicIdPrefix,
            String missingPhotoMessage) {
        validateImageFile(file, missingPhotoMessage);

        if (!isConfigured()) {
            return saveLocally(file, folder, publicIdPrefix);
        }

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
            if (secureUrl != null && !secureUrl.toString().isBlank()) {
                return secureUrl.toString();
            }
            log.warn("Cloudinary upload did not return a secure_url, falling back to local storage");
            return saveLocally(file, folder, publicIdPrefix);
        } catch (Exception ex) {
            log.warn("Cloudinary upload failed ({}), falling back to local storage", ex.getMessage());
            return saveLocally(file, folder, publicIdPrefix);
        }
    }

    private String uploadImageBytes(
            byte[] fileBytes,
            String originalFilename,
            String contentType,
            String folder,
            String publicIdPrefix,
            String missingPhotoMessage) {
        validateImageBytes(fileBytes, originalFilename, contentType, missingPhotoMessage);

        if (!isConfigured()) {
            return saveLocallyBytes(fileBytes, originalFilename, folder, publicIdPrefix);
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    fileBytes,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "public_id", publicIdPrefix + System.currentTimeMillis()));

            Object secureUrl = result.get("secure_url");
            if (secureUrl != null && !secureUrl.toString().isBlank()) {
                return secureUrl.toString();
            }
            log.warn("Cloudinary byte upload did not return a secure_url, falling back to local storage");
            return saveLocallyBytes(fileBytes, originalFilename, folder, publicIdPrefix);
        } catch (Exception ex) {
            log.warn("Cloudinary byte upload failed ({}), falling back to local storage", ex.getMessage());
            return saveLocallyBytes(fileBytes, originalFilename, folder, publicIdPrefix);
        }
    }

    private boolean isConfigured() {
        return !isBlank(cloudName) && !isBlank(apiKey) && !isBlank(apiSecret);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String saveLocally(MultipartFile file, String folder, String publicIdPrefix) {
        Path target = null;
        try {
            String subFolder = folder.startsWith("garbo/") ? folder.substring("garbo/".length()) : folder;
            Path uploadDir = Path.of("uploads", subFolder);
            Files.createDirectories(uploadDir);
            String extension = extensionFromFilename(file.getOriginalFilename());
            String fileName = publicIdPrefix + UUID.randomUUID() + extension;
            target = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "uploads/" + subFolder + "/" + fileName;
        } catch (IOException ex) {
            throw new CollectionException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Local image upload failed",
                    "UPLOAD_FAILED");
        }
    }

    private String saveLocallyBytes(
            byte[] fileBytes,
            String originalFilename,
            String folder,
            String publicIdPrefix) {
        Path target = null;
        try {
            String subFolder = folder.startsWith("garbo/") ? folder.substring("garbo/".length()) : folder;
            Path uploadDir = Path.of("uploads", subFolder);
            Files.createDirectories(uploadDir);
            String extension = extensionFromFilename(originalFilename);
            String fileName = publicIdPrefix + UUID.randomUUID() + extension;
            target = uploadDir.resolve(fileName);
            Files.write(target, fileBytes);
            return "uploads/" + subFolder + "/" + fileName;
        } catch (IOException ex) {
            throw new CollectionException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Local image upload failed",
                    "UPLOAD_FAILED");
        }
    }

    private String extensionFromFilename(String originalFilename) {
        if (originalFilename == null) {
            return ".jpg";
        }
        int idx = originalFilename.lastIndexOf('.');
        if (idx > -1) {
            return originalFilename.substring(idx);
        }
        return ".jpg";
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

    private void validateImageBytes(
            byte[] fileBytes,
            String originalFilename,
            String contentType,
            String missingPhotoMessage) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new CollectionException(HttpStatus.BAD_REQUEST, missingPhotoMessage, "VALIDATION_ERROR");
        }

        if (fileBytes.length > MAX_FILE_BYTES) {
            throw new CollectionException(HttpStatus.BAD_REQUEST, "Image file must be 10MB or smaller",
                    "VALIDATION_ERROR");
        }

        if (!isAllowedImage(contentType, originalFilename)) {
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
