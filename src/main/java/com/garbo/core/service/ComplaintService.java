package com.garbo.core.service;

import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.User;
import com.garbo.core.repository.ComplaintRepository;
import com.garbo.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    public Complaint createComplaint(Complaint complaint, String citizenEmail) {
        User citizen = userRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));
        complaint.setCitizen(citizen);
        complaint.setStatus("PENDING");
        return complaintRepository.save(complaint);
    }

    public List<Complaint> getComplaintsByCitizen(String email) {
        User citizen = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));
        return complaintRepository.findByCitizen(citizen);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    public Complaint updateStatus(Long id, String status) {
        Complaint complaint = getComplaintById(id);
        complaint.setStatus(status);
        return complaintRepository.save(complaint);
    }

    public Complaint assignComplaint(Long id, Long personnelId) {
        Complaint complaint = getComplaintById(id);
        User personnel = userRepository.findById(personnelId)
                .orElseThrow(() -> new RuntimeException("Personnel not found"));
        complaint.setAssignedTo(personnel);
        complaint.setStatus("IN_PROGRESS");
        return complaintRepository.save(complaint);
    }
}
