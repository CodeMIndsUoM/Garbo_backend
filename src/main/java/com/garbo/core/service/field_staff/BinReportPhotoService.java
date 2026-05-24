package com.garbo.core.service.field_staff;

import com.garbo.core.repository.BinReportRepository;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class BinReportPhotoService {

    private final CloudinaryUploadService cloudinaryUploadService;
    private final BinReportRepository binReportRepository;

    public BinReportPhotoService(
            CloudinaryUploadService cloudinaryUploadService,
            BinReportRepository binReportRepository) {
        this.cloudinaryUploadService = cloudinaryUploadService;
        this.binReportRepository = binReportRepository;
    }

    public void uploadAndAttachAsync(
            Long reportId,
            Long binId,
            byte[] photoBytes,
            String originalFilename,
            String contentType) {
        CompletableFuture.runAsync(() -> {
            try {
                String photoUrl = cloudinaryUploadService.uploadBinReportPhoto(
                        photoBytes,
                        originalFilename,
                        contentType,
                        binId);
                int updatedRows = binReportRepository.updatePhotoUrl(reportId, photoUrl);
                if (updatedRows == 0) {
                    log.warn("Bin report photo uploaded but report was not found: reportId={}", reportId);
                }
            } catch (Exception ex) {
                log.warn("Bin report photo upload failed for reportId={}, binId={}: {}",
                        reportId,
                        binId,
                        ex.getMessage());
            }
        });
    }
}
