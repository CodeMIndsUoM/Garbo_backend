package com.garbo.core.repository;

import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByCitizen(User citizen);
    List<Complaint> findByStatus(String status);
    List<Complaint> findByAssignedTo(User assignedTo);
}
