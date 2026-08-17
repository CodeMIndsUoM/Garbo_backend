package com.garbo.core.service;

import com.garbo.core.repository.ComplaintRepository;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ComplaintPhotoService {

    private final CloudinaryUploadService cloudinaryUploadService;
    private final ComplaintRepository complaintRepository;

    public ComplaintPhotoService(
            CloudinaryUploadService cloudinaryUploadService,
            ComplaintRepository complaintRepository) {
        this.cloudinaryUploadService = cloudinaryUploadService;
        this.complaintRepository = complaintRepository;
    }

    public void uploadAndAttachAsync(
            Long complaintId,
            byte[] photoBytes,
            String originalFilename,
            String contentType) {
        CompletableFuture.runAsync(() -> {
            try {
                String photoUrl = cloudinaryUploadService.uploadComplaintPhoto(
                        photoBytes,
                        originalFilename,
                        contentType);
                int updatedRows = complaintRepository.updatePhotoUrl(complaintId, photoUrl);
                if (updatedRows == 0) {
                    log.warn("Complaint photo uploaded but complaint was not found: complaintId={}", complaintId);
                    return;
                }
                log.info("Complaint photo successfully attached asynchronously: complaintId={}, photoUrl={}",
                        complaintId, photoUrl);
            } catch (Exception ex) {
                log.error("Complaint photo background upload failed for complaintId={}: {}",
                        complaintId, ex.getMessage(), ex);
            }
        });
    }
}
