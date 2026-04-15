package com.garbo.core.service;

import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import com.garbo.core.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinReport;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinReportRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.FieldMentorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    private final BinReportRepository binReportRepository;
    private final FieldMentorRepository fieldMentorRepository;

    public BinService(BinRepository binRepository, BinReportRepository binReportRepository, FieldMentorRepository fieldMentorRepository) {
        this.binRepository = binRepository;
        this.binReportRepository = binReportRepository;
        this.fieldMentorRepository = fieldMentorRepository;
    }

    public List<Bin> getAssignedBins(Long empId) {
        return binRepository.findByAssignedToEmpId(empId);
    }

    @Transactional
    public Bin reportBinStatus(String binId, Long reporterId, BinReportRequest request) {
        Bin bin = binRepository.findById(binId)
                .orElseThrow(() -> new EntityNotFoundException("Bin not found with ID: " + binId));

        FieldMentor reporter = null;
        if (reporterId != null) {
            reporter = fieldMentorRepository.findById(reporterId)
                    .orElseThrow(() -> new EntityNotFoundException("Field Mentor not found with ID: " + reporterId));
        }

        // Create Report
        BinReport report = new BinReport();
        report.setBin(bin);
        report.setReporter(reporter);
        report.setFillLevel(request.getFillLevel());
        report.setStatus(request.getStatus());
        report.setNotes(request.getNotes());
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setPhotoUrl(request.getPhotoUrl());
        
        // Determine source
        if (reporter != null) {
            report.setSource("FIELD_STAFF");
        } else {
            report.setSource("ANONYMOUS"); // Or from request if we add source to DTO
        }
        
        binReportRepository.save(report);

        // Update Bin
        bin.setStatus(request.getStatus());
        bin.setFillLevel(request.getFillLevel());
        bin.setLastChecked(LocalDateTime.now());
        
        return binRepository.save(bin);
    }

    public Bin createBin(Bin bin) {
        if (binRepository.existsById(bin.getId())) {
            throw new IllegalArgumentException("Bin with ID " + bin.getId() + " already exists.");
        }
        return binRepository.save(bin);
    }
}
