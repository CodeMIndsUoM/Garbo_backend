package com.garbo.core.repository;

import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByCitizen(User citizen);
    List<Complaint> findByStatus(String status);
    List<Complaint> findByAssignedTo(User assignedTo);

    @Query("""
                SELECT c
                FROM Complaint c
                JOIN Citizen ci ON ci.empId = c.citizen.empId
                WHERE LOWER(ci.council) = LOWER(:council)
                ORDER BY c.createdAt DESC
            """)
    List<Complaint> findByCitizenCouncil(@Param("council") String council);

    @Query("""
                SELECT c
                FROM Complaint c
                JOIN Citizen ci ON ci.empId = c.citizen.empId
                WHERE c.id = :id AND LOWER(ci.council) = LOWER(:council)
            """)
    Optional<Complaint> findByIdAndCitizenCouncil(@Param("id") Long id, @Param("council") String council);
}
