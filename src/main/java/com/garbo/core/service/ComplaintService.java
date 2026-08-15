package com.garbo.core.service;

import com.garbo.api.dto.ComplaintCreateRequest;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.User;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.ComplaintRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.service.notification.NotificationPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class ComplaintService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING",
            "APPROVED",
            "ACCEPTED",
            "REJECTED",
            "IN_PROGRESS",
            "ROUTED"
    );

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final CitizenRepository citizenRepository;
    private final CouncilAccessService councilAccessService;
    private final NotificationPublisher notificationPublisher;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            CitizenRepository citizenRepository,
            CouncilAccessService councilAccessService,
            NotificationPublisher notificationPublisher) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.citizenRepository = citizenRepository;
        this.councilAccessService = councilAccessService;
        this.notificationPublisher = notificationPublisher;
    }

    public Complaint createComplaint(ComplaintCreateRequest request, String citizenEmail) {
        User user = UserLookup.requireUser(userRepository, citizenEmail);

        Citizen citizen = UserLookup.requireCitizen(citizenRepository, citizenEmail);

        String council = UserLookup.resolveCitizenCouncil(citizen);
        if (council == null || council.isBlank()) {
            throw new RuntimeException("Citizen council is required before submitting reports");
        }

        Complaint complaint = new Complaint();
        complaint.setCitizenId(user.getEmpId());
        complaint.setCouncil(council);
        complaint.setStatus("PENDING");
        complaint.setTitle(
                request.getTitle() != null && !request.getTitle().isBlank()
                        ? request.getTitle().trim()
                        : (request.getIssueType() != null ? request.getIssueType().trim() : "General Report"));
        complaint.setIssueType(
                request.getIssueType() != null && !request.getIssueType().isBlank()
                        ? request.getIssueType().trim()
                        : "General");
        complaint.setUrgency(
                request.getUrgency() != null && !request.getUrgency().isBlank()
                        ? request.getUrgency().trim()
                        : "Normal");
        complaint.setWasteType(
                request.getWasteType() != null && !request.getWasteType().isBlank()
                        ? request.getWasteType().trim()
                        : null);
        complaint.setDescription(
                request.getDescription() != null && !request.getDescription().isBlank()
                        ? request.getDescription().trim()
                        : "No description provided");
        complaint.setLocation(
                request.getLocation() != null && !request.getLocation().isBlank()
                        ? request.getLocation().trim()
                        : "Unknown Location");
        complaint.setImageUrl(request.getImageUrl());

        Complaint saved = complaintRepository.save(complaint);
        notificationPublisher.complaintSubmitted(saved);
        return saved;
    }

    public List<Complaint> getComplaintsByCitizen(String email) {
        User citizen = UserLookup.requireUser(userRepository, email);
        return complaintRepository.findByCitizen(citizen);
    }

    public List<Complaint> getAllComplaintsForRequester(String requesterEmail) {
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            return complaintRepository.findAll();
        }

        Optional<String> councilOpt = councilAccessService.resolveCouncilForEmail(requesterEmail);
        if (councilOpt.isEmpty()) {
            return List.of();
        }
        return complaintRepository.findByCitizenCouncil(councilOpt.get());
    }

    public Complaint getComplaintById(Long id, String requesterEmail) {
        if (councilAccessService.isSuperAdmin(requesterEmail)) {
            return complaintRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Complaint not found"));
        }

        Optional<String> requesterCouncil = councilAccessService.resolveCouncilForEmail(requesterEmail);
        if (requesterCouncil.isEmpty()) {
            throw new AccessDeniedException("No council assigned for current user");
        }

        if (isCitizen(requesterEmail)) {
            User citizen = UserLookup.requireUser(userRepository, requesterEmail);
            Complaint ownComplaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Complaint not found"));
            if (ownComplaint.getCitizenId() == null
                    || !ownComplaint.getCitizenId().equals(citizen.getEmpId())) {
                throw new AccessDeniedException("Complaint is not visible to this citizen");
            }
            return ownComplaint;
        }

        return complaintRepository.findByIdAndCitizenCouncil(id, requesterCouncil.get())
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    private boolean isCitizen(String requesterEmail) {
        return UserLookup.findUser(userRepository, requesterEmail)
                .map(user -> {
                    String role = user.getRole();
                    if (role == null) {
                        return false;
                    }
                    String normalized = role.trim().toUpperCase();
                    return normalized.equals("CITIZEN") || normalized.equals("ROLE_CITIZEN");
                })
                .orElse(false);
    }

    public Complaint getComplaintByIdUnsafe(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    public Complaint updateStatus(Long id, String status, String requesterEmail) {
        return updateStatus(id, status, null, requesterEmail);
    }

    @Transactional
    public Complaint updateStatus(Long id, String status, String resolutionNotes, String requesterEmail) {
        assertAdminCanModerate(requesterEmail);

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        String normalizedStatus = normalizeStatus(status);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported complaint status: " + status);
        }

        Complaint complaint = getComplaintById(id, requesterEmail);
        complaint.setStatus(normalizedStatus);
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            complaint.setResolutionNotes(resolutionNotes.trim());
        } else if ("REJECTED".equals(normalizedStatus) && complaint.getResolutionNotes() == null) {
            complaint.setResolutionNotes("Rejected by admin");
        }
        Complaint saved = complaintRepository.save(complaint);
        notificationPublisher.complaintStatusUpdated(saved);
        return saved;
    }

    private void assertAdminCanModerate(String requesterEmail) {
        if (councilAccessService.isAdmin(requesterEmail)) {
            return;
        }
        throw new AccessDeniedException("Only admins can approve or reject complaints");
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("APPROVE".equals(normalized)) {
            return "APPROVED";
        }
        if ("REJECT".equals(normalized)) {
            return "REJECTED";
        }
        if ("ACCEPT".equals(normalized)) {
            return "ACCEPTED";
        }
        return normalized;
    }

    public Complaint assignComplaint(Long id, Long personnelId, String requesterEmail) {
        Complaint complaint = getComplaintById(id, requesterEmail);
        userRepository.findById(personnelId)
                .orElseThrow(() -> new RuntimeException("Personnel not found"));
        complaint.setAssignedPersonnelId(personnelId);
        complaint.setStatus("IN_PROGRESS");
        return complaintRepository.save(complaint);
    }

    @Transactional
    public void markComplaintsRouted(java.util.List<Long> complaintIds) {
        if (complaintIds == null || complaintIds.isEmpty()) {
            return;
        }
        for (Long id : complaintIds) {
            if (id == null) {
                continue;
            }
            complaintRepository.findById(id).ifPresent(complaint -> {
                complaint.setStatus("ROUTED");
                complaintRepository.save(complaint);
            });
        }
    }

    public double[] parseComplaintCoordinates(Complaint complaint) {
        if (complaint == null || complaint.getLocation() == null) {
            return null;
        }
        String value = complaint.getLocation().trim();
        if (value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length < 2) {
            return null;
        }
        try {
            return new double[] {
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim())
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
