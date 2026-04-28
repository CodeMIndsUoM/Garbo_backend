package com.garbo.core.service;

import com.garbo.api.dto.BinDTO;
import com.garbo.core.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinReport;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinReportRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.service.event.BinChangedEvent;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BinService {

    // ── HEAD dependencies ─────────────────────────────────────────────────────

    private final BinReportRepository binReportRepository;
    private final FieldMentorRepository fieldMentorRepository;

    // ── kevin-RWS dependencies ────────────────────────────────────────────────

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public BinService(BinRepository binRepository,
                      BinReportRepository binReportRepository,
                      FieldMentorRepository fieldMentorRepository) {
        this.binRepository = binRepository;
        this.binReportRepository = binReportRepository;
        this.fieldMentorRepository = fieldMentorRepository;
    }


    // ── Methods from HEAD ─────────────────────────────────────────────────────

    public List<Bin> getAssignedBins(Long empId) {
        return binRepository.findByAssignedToEmpId(empId);
    }

    @Transactional
    public Bin reportBinStatus(Long binId, Long reporterId, BinReportRequest request) {
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
            report.setSource("ANONYMOUS");
        }

        binReportRepository.save(report);

        // Update Bin
        bin.setStatus(request.getStatus());
        bin.setFillLevel(request.getFillLevel());
        bin.setLastChecked(LocalDateTime.now());

        return binRepository.save(bin);
    }

    public Bin createBin(Bin bin) {
        if (binRepository.existsById((Long) bin.getId())) {
            throw new IllegalArgumentException("Bin with ID " + bin.getId() + " already exists.");
        }
        return binRepository.save(bin);
    }


    // ── Methods from kevin-RWS ────────────────────────────────────────────────

    // Add new bin
    public Bin addBin(BinDTO dto) {
        Bin bin = new Bin();
        bin.setLatitude(dto.getLat());
        bin.setLng(dto.getLng());
        bin.setFillLevel(dto.getFillLevel());
        bin.setPriority(dto.getPriority());
        String zone = dto.getZone() == null || dto.getZone().isBlank() ? "unassigned" : dto.getZone();
        bin.setZone(zone);
        Bin saved = binRepository.save(bin);
        eventPublisher.publishEvent(new BinChangedEvent("CREATED", saved.getId()));
        return saved;
    }

    // Remove bin
    public void deleteBin(Long id) {
        binRepository.deleteByIdNative(id);
        eventPublisher.publishEvent(new BinChangedEvent("DELETED", id));
    }

    // Get all bins (for map)
    public List<Bin> getAllBins() {
        return binRepository.findAllForMap()
                .stream()
                .map(row -> {
                    Bin bin = new Bin();
                    bin.setId(row.getId());
                    bin.setLatitude(row.getLat());
                    bin.setLng(row.getLng());
                    bin.setFillLevel(row.getFillLevel());
                    bin.setPriority(row.getPriority());
                    bin.setZone(row.getZone());
                    return bin;
                })
                .toList();
    }

    // Get bin details
    public Bin getBinById(Long id) {
        return binRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bin not found"));
    }

    // Update bin priority
    public void updatePriority(Long id, String priority) {
        binRepository.updatePriorityNative(id, priority);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
    }

    // Update bin zone
    public void updateZone(Long id, String zone) {
        String safeZone = zone == null || zone.isBlank() ? "unassigned" : zone;
        binRepository.updateZoneNative(id, safeZone);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
    }
}