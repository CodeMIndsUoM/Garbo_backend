package com.garbo.core.service;

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
