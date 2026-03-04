package com.garbo.core.service;

import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BinService {

    private final BinRepository binRepository;

    public BinService(BinRepository binRepository) {
        this.binRepository = binRepository;
    }

    public Bin createBin(Bin bin) {
        if (binRepository.existsByBinCode(bin.getBinCode())) {
            throw new IllegalArgumentException("Bin with code '" + bin.getBinCode() + "' already exists");
        }
        return binRepository.save(bin);
    }

    public Bin updateBin(Long id, Bin updatedBin) {
        Bin existing = binRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bin not found with id: " + id));

        if (updatedBin.getBinCode() != null) existing.setBinCode(updatedBin.getBinCode());
        if (updatedBin.getLocation() != null) existing.setLocation(updatedBin.getLocation());
        if (updatedBin.getLatitude() != null) existing.setLatitude(updatedBin.getLatitude());
        if (updatedBin.getLongitude() != null) existing.setLongitude(updatedBin.getLongitude());
        if (updatedBin.getType() != null) existing.setType(updatedBin.getType());
        if (updatedBin.getFillLevel() != null) existing.setFillLevel(updatedBin.getFillLevel());
        if (updatedBin.getBatteryLevel() != null) existing.setBatteryLevel(updatedBin.getBatteryLevel());
        if (updatedBin.getStatus() != null) existing.setStatus(updatedBin.getStatus());
        if (updatedBin.getCouncil() != null) existing.setCouncil(updatedBin.getCouncil());
        if (updatedBin.getLastCollectionAt() != null) existing.setLastCollectionAt(updatedBin.getLastCollectionAt());

        return binRepository.save(existing);
    }

    public void deleteBin(Long id) {
        Bin existing = binRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bin not found with id: " + id));
        existing.setIsActive(false);
        binRepository.save(existing);
    }

    public List<Bin> getAllBins() {
        return binRepository.findByIsActiveTrue();
    }

    public Optional<Bin> getBinById(Long id) {
        return binRepository.findById(id);
    }

    public List<Bin> getBinsByCouncil(String council) {
        return binRepository.findByCouncilAndIsActiveTrue(council);
    }

    public Bin updateBinStatus(Long id, String status) {
        Bin existing = binRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bin not found with id: " + id));
        existing.setStatus(status);
        return binRepository.save(existing);
    }
}
