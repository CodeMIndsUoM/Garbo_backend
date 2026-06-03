package com.garbo.core.service.field_staff;

import com.garbo.api.dto.BinDTO;
import com.garbo.core.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinReport;
import com.garbo.core.entity.CouncilBoundary;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinReportRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.CouncilBoundaryRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.service.CouncilAccessService;
import com.garbo.core.service.event.BinChangedEvent;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class BinService {

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final BinReportRepository       binReportRepository;
    private final FieldMentorRepository     fieldMentorRepository;
    private final CouncilAccessService      councilAccessService;
    private final CouncilBoundaryRepository councilBoundaryRepository;

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public BinService(BinRepository binRepository,
                      BinReportRepository binReportRepository,
                      FieldMentorRepository fieldMentorRepository,
                      CouncilAccessService councilAccessService,
                      CouncilBoundaryRepository councilBoundaryRepository) {
        this.binRepository            = binRepository;
        this.binReportRepository      = binReportRepository;
        this.fieldMentorRepository    = fieldMentorRepository;
        this.councilAccessService     = councilAccessService;
        this.councilBoundaryRepository = councilBoundaryRepository;
    }

    // ── Field-staff methods ───────────────────────────────────────────────────

    public List<Bin> getAssignedBins(Long empId) {
        return binRepository.findByAssignedToEmpId(empId);
    }

    @Transactional
    public Bin reportBinStatus(Long binId, Long reporterId, BinReportRequest request) {
        Bin bin = binRepository.findByNumericId(binId)
                .orElseThrow(() -> new EntityNotFoundException("Bin not found with ID: " + binId));

        FieldMentor reporter = null;
        if (reporterId != null) {
            reporter = fieldMentorRepository.findById(reporterId)
                    .orElseThrow(() -> new EntityNotFoundException("Field Mentor not found with ID: " + reporterId));
        }

        BinReport report = new BinReport();
        report.setBin(bin);
        report.setReporter(reporter);
        report.setFillLevel(request.getFillLevel());
        report.setStatus(request.getStatus());
        report.setNotes(request.getNotes());
        report.setLatitude(request.getLatitude());
        report.setLongitude(request.getLongitude());
        report.setPhotoUrl(request.getPhotoUrl());
        report.setSource(reporter != null ? "FIELD_STAFF" : "ANONYMOUS");

        binReportRepository.save(report);

        Integer effectiveFillLevel =
                "full".equalsIgnoreCase(request.getStatus()) ? 100 :
                "half".equalsIgnoreCase(request.getStatus()) ? 50  : 0;

        int updatedRows = binRepository.updateStatusForReport(binId, request.getStatus(), effectiveFillLevel);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("Bin not found with ID: " + binId);
        }

        eventPublisher.publishEvent(new BinChangedEvent("STATUS_REPORTED", binId));

        Bin updated = new Bin();
        updated.setId(binId);
        updated.setStatus(request.getStatus());
        updated.setFillLevel(effectiveFillLevel);
        updated.setLastChecked(LocalDateTime.now());
        return updated;
    }

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

        eventPublisher.publishEvent(new BinChangedEvent("STATUS_UNDONE", binId));

        Bin updated = new Bin();
        updated.setId(binId);
        updated.setStatus("notChecked");
        updated.setFillLevel(0);
        updated.setLastChecked(null);
        return updated;
    }

    // ── Admin bin management methods ──────────────────────────────────────────

    public Bin createBin(Bin bin) {
        if (bin.getId() != null && binRepository.existsById(bin.getId())) {
            throw new IllegalArgumentException("Bin with ID " + bin.getId() + " already exists.");
        }
        bin.setId(null);
        return binRepository.save(bin);
    }

    public List<Bin> getBins(String council) {
        List<Bin> bins = (council == null || council.isBlank())
                ? binRepository.findAll()
                : binRepository.findByCouncilIgnoreCase(council.trim());
        bins.forEach(this::normalizeReadModel);
        return bins;
    }

    /**
     * Creates a bin for the currently logged-in admin.
     * Council is resolved automatically from the admin's email via CouncilAccessService.
     * Coordinates are validated against the council boundary stored in DB.
     */
    public Bin createBinForCurrentUser(Bin payload) {
        String email   = currentEmail();
        String council = councilAccessService.resolveCouncilForEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Your account has no assigned council"));

        double[] latLng = resolveIncomingCoordinates(payload);
        validateCoordinatesInCouncil(council, latLng[0], latLng[1]);

        String generatedCode = generateNextBinCode(council);
        payload.setBinCode(generatedCode);
        payload.setCouncil(council);
        payload.setLocation(latLng[0] + "," + latLng[1]);
        payload.setCoordinates(latLng[0] + "," + latLng[1]);
        payload.setLatitude(latLng[0]);
        payload.setLongitude(latLng[1]);
        payload.setLastChecked(LocalDateTime.now());
        normalizeCreateModel(payload);

        Bin saved = binRepository.save(payload);
        normalizeReadModel(saved);
        return saved;
    }

    @Transactional
    public void deleteBinForCurrentUser(Long id) {
        Bin bin = getBinWithCouncilAccess(id);
        binReportRepository.deleteByBinId(bin.getId());
        binRepository.deleteByIdNative(bin.getId());
        eventPublisher.publishEvent(new BinChangedEvent("DELETED", id));
    }

    public Bin updatePriorityForCurrentUser(Long id, String priority) {
        Bin bin = getBinWithCouncilAccess(id);
        String normalized = (priority == null || priority.isBlank())
                ? "medium" : priority.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("low") && !normalized.equals("medium") && !normalized.equals("high")) {
            throw new IllegalArgumentException("Priority must be low, medium, or high");
        }
        bin.setPriority(normalized);
        binRepository.updatePriorityNative(id, normalized);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
        normalizeReadModel(bin);
        return bin;
    }

    public Bin updateZoneForCurrentUser(Long id, String zone) {
        Bin bin = getBinWithCouncilAccess(id);
        String safeZone = (zone == null || zone.isBlank()) ? "unassigned" : zone.trim();
        bin.setZone(safeZone);
        binRepository.updateZoneNative(id, safeZone);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
        normalizeReadModel(bin);
        return bin;
    }

    // ── Legacy kevin-RWS methods ──────────────────────────────────────────────

    public Bin addBin(BinDTO dto) {
        Bin bin = new Bin();
        bin.setLatitude(dto.getLat());
        bin.setLng(dto.getLng());
        bin.setFillLevel(dto.getFillLevel());
        bin.setPriority(dto.getPriority());
        String zone = (dto.getZone() == null || dto.getZone().isBlank()) ? "unassigned" : dto.getZone();
        bin.setZone(zone);
        Bin saved = binRepository.save(bin);
        eventPublisher.publishEvent(new BinChangedEvent("CREATED", saved.getId()));
        return saved;
    }

    @Transactional
    public void deleteBin(Long id) {
        binReportRepository.deleteByBinId(id);
        binRepository.deleteByIdNative(id);
        eventPublisher.publishEvent(new BinChangedEvent("DELETED", id));
    }

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

    public Bin getBinById(Long id) {
        return binRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bin not found"));
    }

    public void updatePriority(Long id, String priority) {
        binRepository.updatePriorityNative(id, priority);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
    }

    public void updateZone(Long id, String zone) {
        String safeZone = (zone == null || zone.isBlank()) ? "unassigned" : zone;
        binRepository.updateZoneNative(id, safeZone);
        eventPublisher.publishEvent(new BinChangedEvent("UPDATED", id));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void normalizeCreateModel(Bin bin) {
        if (bin.getStatus() == null || bin.getStatus().isBlank()) {
            bin.setStatus("empty");
        } else {
            bin.setStatus(bin.getStatus().trim().toLowerCase(Locale.ROOT));
        }

        if ("full".equalsIgnoreCase(bin.getStatus())) {
            bin.setFillLevel(100);
        } else if ("half".equalsIgnoreCase(bin.getStatus())) {
            bin.setFillLevel(50);
        } else {
            bin.setFillLevel(0);
        }

        if (bin.getPriority() == null || bin.getPriority().isBlank()) {
            bin.setPriority("medium");
        } else {
            bin.setPriority(bin.getPriority().trim().toLowerCase(Locale.ROOT));
        }

        if (bin.getZone() == null || bin.getZone().isBlank()) {
            bin.setZone("unassigned");
        }
    }

    private void normalizeReadModel(Bin bin) {
        if ((bin.getCoordinates() == null || bin.getCoordinates().isBlank())
                && bin.getLatitude() != null && bin.getLongitude() != null) {
            bin.setCoordinates(bin.getLatitude() + "," + bin.getLongitude());
        }
        if (bin.getLocation() == null || bin.getLocation().isBlank()) {
            bin.setLocation(bin.getCoordinates());
        }
    }

    private String currentEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return auth.getName();
    }

    private Bin getBinWithCouncilAccess(Long id) {
        Bin existing = binRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bin not found"));

        String email      = currentEmail();
        boolean superAdmin = councilAccessService.isSuperAdmin(email);

        if (!superAdmin) {
            String requesterCouncil = councilAccessService.resolveCouncilForEmail(email)
                    .orElse("");
            if (requesterCouncil.isBlank()
                    || existing.getCouncil() == null
                    || !existing.getCouncil().equalsIgnoreCase(requesterCouncil)) {
                throw new AccessDeniedException("You can only manage bins from your council");
            }
        }
        return existing;
    }

    private String generateNextBinCode(String council) {
        List<Bin> councilBins = binRepository.findByCouncilIgnoreCase(council.trim());
        String prefix = council.trim() + "-";
        int nextNumber = councilBins.stream()
                .map(Bin::getBinCode)
                .filter(code -> code != null
                        && code.regionMatches(true, 0, prefix, 0, prefix.length()))
                .map(code -> code.substring(prefix.length()).trim())
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0) + 1;
        return prefix + nextNumber;
    }

    private double[] parseLatLng(String rawLocation) {
        String[] parts = rawLocation.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Location must be in lat,lng format");
        }
        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lng = Double.parseDouble(parts[1].trim());
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                throw new IllegalArgumentException("Latitude/longitude out of range");
            }
            return new double[]{ lat, lng };
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Location must contain valid latitude and longitude");
        }
    }

    private double[] resolveIncomingCoordinates(Bin payload) {
        if (payload.getLocation() != null && !payload.getLocation().isBlank()) {
            return parseLatLng(payload.getLocation());
        }
        if (payload.getLatitude() != null && payload.getLongitude() != null) {
            double lat = payload.getLatitude();
            double lng = payload.getLongitude();
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                throw new IllegalArgumentException("Latitude/longitude out of range");
            }
            return new double[]{ lat, lng };
        }
        throw new IllegalArgumentException("Location is required as lat,lng");
    }

    /**
     * Validates that the given coordinates fall within the council boundary
     * stored in the council_boundaries table using ray-casting algorithm.
     */
    private void validateCoordinatesInCouncil(String council, double lat, double lng) {
        List<CouncilBoundary> points =
            councilBoundaryRepository.findByCouncilIgnoreCaseOrderByPointOrderAsc(council);

        if (points.isEmpty()) {
            throw new IllegalArgumentException(
                "No boundary configured for council: " + council);
        }

        // Ray-casting point-in-polygon
        boolean inside = false;
        int n = points.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = points.get(i).getLat(), yi = points.get(i).getLng();
            double xj = points.get(j).getLat(), yj = points.get(j).getLng();
            boolean intersect = ((yi > lng) != (yj > lng))
                && (lat < (xj - xi) * (lng - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }

        if (!inside) {
            throw new IllegalArgumentException(
                "Coordinates are outside the council boundary");
        }
    }
}