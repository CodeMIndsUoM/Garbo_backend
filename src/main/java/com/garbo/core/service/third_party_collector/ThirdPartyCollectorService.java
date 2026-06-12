package com.garbo.core.service.third_party_collector;

import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class ThirdPartyCollectorService {

    private final ThirdPartyCollectorRepository repository;
    private final CloudinaryUploadService cloudinaryUploadService;

    public ThirdPartyCollectorService(
            ThirdPartyCollectorRepository repository,
            CloudinaryUploadService cloudinaryUploadService) {
        this.repository = repository;
        this.cloudinaryUploadService = cloudinaryUploadService;
    }

    public Optional<ThirdPartyCollector> getProfile(Long id) {
        return repository.findById(id);
    }

    public ThirdPartyCollector updateProfile(Long id, ThirdPartyCollector updatedDetails) {
        return repository.findById(id).map(collector -> {
            if (updatedDetails.getEmpName() != null) collector.setEmpName(updatedDetails.getEmpName());
            if (updatedDetails.getEmail() != null) collector.setEmail(updatedDetails.getEmail());
            if (updatedDetails.getPhone() != null) collector.setPhone(updatedDetails.getPhone());
            if (updatedDetails.getDefaultAddress() != null) collector.setDefaultAddress(updatedDetails.getDefaultAddress());
            if (updatedDetails.getAvatarUrl() != null) collector.setAvatarUrl(updatedDetails.getAvatarUrl());
            if (updatedDetails.getNIC() != null) collector.setNIC(updatedDetails.getNIC());
            if (updatedDetails.getCompany() != null) collector.setCompany(updatedDetails.getCompany());
            return repository.save(collector);
        }).orElseThrow(() -> new RuntimeException("ThirdPartyCollector not found with id: " + id));
    }

    public ThirdPartyCollector uploadAvatar(Long id, MultipartFile photo) {
        ThirdPartyCollector collector = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ThirdPartyCollector not found with id: " + id));
        String url = cloudinaryUploadService.uploadProfilePhoto(photo, id);
        collector.setAvatarUrl(url);
        return repository.save(collector);
    }

    public ThirdPartyCollector removeAvatar(Long id) {
        ThirdPartyCollector collector = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ThirdPartyCollector not found with id: " + id));
        collector.setAvatarUrl(null);
        return repository.save(collector);
    }
}
