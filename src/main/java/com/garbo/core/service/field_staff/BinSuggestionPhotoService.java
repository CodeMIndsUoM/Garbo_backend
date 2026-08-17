package com.garbo.core.service.field_staff;

import com.garbo.core.repository.BinSuggestionRepository;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class BinSuggestionPhotoService {

    private final CloudinaryUploadService cloudinaryUploadService;
    private final BinSuggestionRepository binSuggestionRepository;

    public BinSuggestionPhotoService(
            CloudinaryUploadService cloudinaryUploadService,
            BinSuggestionRepository binSuggestionRepository) {
        this.cloudinaryUploadService = cloudinaryUploadService;
        this.binSuggestionRepository = binSuggestionRepository;
    }

    public void uploadAndAttachAsync(
            Long suggestionId,
            byte[] photoBytes,
            String originalFilename,
            String contentType) {
        CompletableFuture.runAsync(() -> {
            try {
                String photoUrl = cloudinaryUploadService.uploadBinSuggestionPhoto(
                        photoBytes,
                        originalFilename,
                        contentType);
                int updatedRows = binSuggestionRepository.updatePhotoUrl(suggestionId, photoUrl);
                if (updatedRows == 0) {
                    log.warn("Bin suggestion photo uploaded but suggestion was not found: suggestionId={}", suggestionId);
                    return;
                }
                log.info("Bin suggestion photo successfully attached asynchronously: suggestionId={}, photoUrl={}",
                        suggestionId, photoUrl);
            } catch (Exception ex) {
                log.error("Bin suggestion photo background upload failed for suggestionId={}: {}",
                        suggestionId, ex.getMessage(), ex);
            }
        });
    }
}
