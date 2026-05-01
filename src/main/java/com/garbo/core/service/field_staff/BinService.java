package com.garbo.core.service.field_staff;

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

// Bin service includes multiple feature areas.
// In this scoped refactor pass, mobile-critical methods are:
//   - getAssignedBins
//   - reportBinStatus
//   - undoBinReport
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

    // Shared report operation used by both anonymous JSON report and field-staff multipart report.
    @Transactional
    public Bin reportBinStatus(Long binId, Long reporterId, BinReportRequest request) {
        Bin bin = binRepository.findByNumericId(binId)
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

        // Update the bin via native query because bins.id is stored as text in DB.
        int updatedRows = binRepository.updateStatusForReport(binId, request.getStatus(), request.getFillLevel());
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Bin not found with ID: " + binId);
        }

        // Trigger realtime websocket push for dashboards listening to bin-status changes.
        eventPublisher.publishEvent(new BinChangedEvent("STATUS_REPORTED", binId));

        Bin updated = new Bin();
        updated.setId(binId);
        updated.setStatus(request.getStatus());
        updated.setFillLevel(request.getFillLevel());
        updated.setLastChecked(LocalDateTime.now());
        return updated;
    }

    // Field-staff undo operation used by dedicated mobile undo endpoint.
    @Transactional
    public Bin undoBinReport(Long binId, Long reporterId) {
        if (reporterId != null) {
            fieldMentorRepository.findById(reporterId)
                    .orElseThrow(() -> new EntityNotFoundException("Field Mentor not found with ID: " + reporterId));
        }

        int updatedRows = binRepository.resetStatusForUndo(binId);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Bin not found with ID: " + binId);
        }

        // Trigger realtime websocket push for dashboards listening to bin-status changes.
        eventPublisher.publishEvent(new BinChangedEvent("STATUS_UNDONE", binId));

        // Return a lightweight response object with final state expected by mobile.
        Bin updated = new Bin();
        updated.setId(binId);
        updated.setStatus("notChecked");
        updated.setFillLevel(0);
        updated.setLastChecked(null);
        return updated;
    }

    public Bin createBin(Bin bin) {
        if (bin.getId() != null && binRepository.existsById(bin.getId())) {
            throw new IllegalArgumentException("Bin with ID " + bin.getId() + " already exists.");
        }
        bin.setId(null);
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
                    bin.setFillLevel(row.getFillLevel() != null ? row.getFillLevel() : 0);
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