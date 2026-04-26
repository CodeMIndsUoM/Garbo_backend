package com.garbo.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.garbo.core.entity.WasteBin;
import com.garbo.core.repository.WasteBinRepository;

@Service
public class WasteBinService {

    private final WasteBinRepository wasteBinRepository;

    public WasteBinService(WasteBinRepository wasteBinRepository) {
        this.wasteBinRepository = wasteBinRepository;
    }

    public List<WasteBin> getAll() {
        List<WasteBin> bins = wasteBinRepository.findAll();
        bins.forEach(this::hydrateClientFields);
        return bins;
    }

    public WasteBin create(WasteBin payload) {
        String binCode = payload.getBinCode();
        if ((binCode == null || binCode.isBlank()) && payload.getId() != null) {
            binCode = payload.getId();
        }
        if (binCode == null || binCode.isBlank()) {
            throw new IllegalArgumentException("Bin code is required");
        }
        if (payload.getLocation() == null || payload.getLocation().isBlank()) {
            throw new IllegalArgumentException("Location is required");
        }
        final String normalizedCode = binCode.trim();
        wasteBinRepository.findByIdIgnoreCase(normalizedCode)
                .ifPresent(b -> {
                    throw new IllegalArgumentException("Bin code already exists");
                });
        LocalDateTime now = LocalDateTime.now();
        payload.setId(normalizedCode);
        payload.setCreatedAt(now);
        payload.setUpdatedAt(now);
        normalize(payload);
        WasteBin saved = wasteBinRepository.save(payload);
        hydrateClientFields(saved);
        return saved;
    }

    public void delete(String id) {
        if (!wasteBinRepository.existsById(id)) {
            throw new NoSuchElementException("Bin not found");
        }
        wasteBinRepository.deleteById(id);
    }

    private void normalize(WasteBin bin) {
        if (bin.getStatus() == null || bin.getStatus().isBlank()) {
            bin.setStatus("normal");
        } else {
            bin.setStatus(bin.getStatus().trim().toLowerCase(Locale.ROOT));
        }
        if (bin.getFillLevel() == null) {
            bin.setFillLevel(0);
        } else {
            bin.setFillLevel(Math.max(0, Math.min(bin.getFillLevel(), 100)));
        }
        if (bin.getCoordinates() != null && (bin.getLatitude() == null || bin.getLongitude() == null)) {
            String[] parts = bin.getCoordinates().split(",");
            if (parts.length == 2) {
                try {
                    Double parsedLat = Double.parseDouble(parts[0].trim());
                    Double parsedLng = Double.parseDouble(parts[1].trim());
                    bin.setLatitude(parsedLat);
                    bin.setLongitude(parsedLng);
                    if (bin.getLat() == null) {
                        bin.setLat(parsedLat);
                    }
                    if (bin.getLng() == null) {
                        bin.setLng(parsedLng);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (bin.getCoordinates() == null && bin.getLatitude() != null && bin.getLongitude() != null) {
            bin.setCoordinates(bin.getLatitude() + "," + bin.getLongitude());
        }
        if (bin.getLat() == null) {
            if (bin.getLatitude() != null) {
                bin.setLat(bin.getLatitude());
            } else {
                bin.setLat(0.0);
            }
        }
        if (bin.getLng() == null) {
            if (bin.getLongitude() != null) {
                bin.setLng(bin.getLongitude());
            } else {
                bin.setLng(0.0);
            }
        }
    }

    private void hydrateClientFields(WasteBin bin) {
        bin.setBinCode(bin.getId());
        if (bin.getLat() == null && bin.getLatitude() != null) {
            bin.setLat(bin.getLatitude());
        }
        if (bin.getLng() == null && bin.getLongitude() != null) {
            bin.setLng(bin.getLongitude());
        }
        if (bin.getLatitude() == null && bin.getLat() != null) {
            bin.setLatitude(bin.getLat());
        }
        if (bin.getLongitude() == null && bin.getLng() != null) {
            bin.setLongitude(bin.getLng());
        }
    }
}
