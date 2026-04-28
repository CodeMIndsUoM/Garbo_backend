package com.garbo.core.service;

import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.User;
import com.garbo.core.repository.ComplaintRepository;
import com.garbo.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouncilAccessService councilAccessService;

    public Complaint createComplaint(Complaint complaint, String citizenEmail) {
        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));
        complaint.setCitizen(citizen);
        complaint.setStatus("PENDING");
        if (complaint.getCategory() == null || ((String) complaint.getCategory()).isBlank()) {
            complaint.setCategory(complaint.getTitle() == null ? "General" : complaint.getTitle());
        }
        if (complaint.getDescription() == null || complaint.getDescription().isBlank()) {
            complaint.setDescription("No description provided");
        }
        if (complaint.getLocation() == null || complaint.getLocation().isBlank()) {
            String location = complaint.getLocationAddress();
            complaint.setLocation((location == null || location.isBlank()) ? "Unknown Location" : location);
        }
        return complaintRepository.save(complaint);
    }

    public List<Complaint> getComplaintsByCitizen(String email) {
        User citizen = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));
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
            User citizen = userRepository.findByEmail(requesterEmail)
                    .orElseThrow(() -> new RuntimeException("Citizen not found"));
            Complaint ownComplaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Complaint not found"));
            if (!ownComplaint.getCitizen().getEmpId().equals(citizen.getEmpId())) {
                throw new AccessDeniedException("Complaint is not visible to this citizen");
            }
            return ownComplaint;
        }

        return complaintRepository.findByIdAndCitizenCouncil(id, requesterCouncil.get())
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    private boolean isCitizen(String requesterEmail) {
        return userRepository.findByEmail(requesterEmail)
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
        Complaint complaint = getComplaintById(id, requesterEmail);
        complaint.setStatus(status);
        return complaintRepository.save(complaint);
    }

    public Complaint updateStatus(Long id, String status, String resolutionNotes, String requesterEmail) {
        Complaint complaint = getComplaintById(id, requesterEmail);
        complaint.setStatus(status);
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
            complaint.setResolutionNotes(resolutionNotes);
        }
        return complaintRepository.save(complaint);
    }

    public Complaint assignComplaint(Long id, Long personnelId, String requesterEmail) {
        Complaint complaint = getComplaintById(id, requesterEmail);
        User personnel = userRepository.findById(personnelId)
                .orElseThrow(() -> new RuntimeException("Personnel not found"));
        complaint.setAssignedTo(personnel);
        complaint.setStatus("IN_PROGRESS");
        return complaintRepository.save(complaint);
    }
}
