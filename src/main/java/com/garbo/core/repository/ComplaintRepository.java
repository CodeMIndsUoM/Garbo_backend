package com.garbo.core.repository;

import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    @Transactional
    @Modifying
    @Query("UPDATE Complaint c SET c.imageUrl = :photoUrl WHERE c.id = :id")
    int updatePhotoUrl(@Param("id") Long id, @Param("photoUrl") String photoUrl);

    // ── Basic finders ─────────────────────────────────────────────────────────

    @Query("""
                SELECT c
                FROM Complaint c
                WHERE c.citizenId = :#{#citizen.empId}
                ORDER BY c.createdAt DESC
            """)
    List<Complaint> findByCitizen(@Param("citizen") User citizen);
    List<Complaint> findByStatus(String status);

    @Query("""
                SELECT c
                FROM Complaint c
                WHERE c.assignedPersonnelId = :#{#assignedTo.empId}
                ORDER BY c.createdAt DESC
            """)
    List<Complaint> findByAssignedTo(@Param("assignedTo") User assignedTo);

    @Query("""
                SELECT c
                FROM Complaint c
                JOIN Citizen ci ON ci.empId = c.citizenId
                WHERE LOWER(ci.council) = LOWER(:council)
                ORDER BY c.createdAt DESC
            """)
    List<Complaint> findByCitizenCouncil(@Param("council") String council);

    @Query("""
                SELECT c
                FROM Complaint c
                JOIN Citizen ci ON ci.empId = c.citizenId
                WHERE c.id = :id AND LOWER(ci.council) = LOWER(:council)
            """)
    Optional<Complaint> findByIdAndCitizenCouncil(@Param("id") Long id, @Param("council") String council);


    // ── Analytics — ALL councils ──────────────────────────────────────────────

    @Query(value = """
        SELECT
            COUNT(*) FILTER (WHERE status = 'PENDING')  AS pending_count,
            COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'APPROVED')) AS accepted_count
        FROM complaints
        WHERE created_at >= CURRENT_DATE
    """, nativeQuery = true)
    List<Object[]> getTodaySummary();

    @Query(value = """
        SELECT
            'Today'                                         AS label,
            COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending_count,
            COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'APPROVED'))    AS accepted_count
        FROM complaints
        WHERE created_at >= CURRENT_DATE
    """, nativeQuery = true)
    List<Object[]> getTodayChart();

    @Query(value = """
        SELECT
            TO_CHAR(created_at, 'Dy')                      AS label,
            COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending_count,
            COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'APPROVED'))    AS accepted_count
        FROM complaints
        WHERE created_at >= :startDate
        GROUP BY TO_CHAR(created_at, 'Dy'), DATE_TRUNC('day', created_at)
        ORDER BY MIN(created_at)
    """, nativeQuery = true)
    List<Object[]> getWeekChart(@Param("startDate") LocalDateTime startDate);

    @Query(value = """
        SELECT
            TO_CHAR(created_at, 'Mon DD')                  AS label,
            COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending_count,
            COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'APPROVED'))    AS accepted_count
        FROM complaints
        WHERE created_at >= :startDate
        GROUP BY TO_CHAR(created_at, 'Mon DD'), DATE_TRUNC('day', created_at)
        ORDER BY MIN(created_at)
    """, nativeQuery = true)
    List<Object[]> getMonthChart(@Param("startDate") LocalDateTime startDate);


    // ── Analytics — filtered by council ──────────────────────────────────────

    @Query(value = """
        SELECT
            COUNT(*) FILTER (WHERE status = 'PENDING')  AS pending_count,
            COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'APPROVED')) AS accepted_count
        FROM complaints
        WHERE created_at >= CURRENT_DATE
          AND LOWER(council) = LOWER(:council)
    """, nativeQuery = true)
    List<Object[]> getTodaySummaryByCouncil(@Param("council") String council);

    @Query(value = """
        SELECT
            'Today'                                         AS label,
            COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending_count,
            COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'APPROVED'))    AS accepted_count
        FROM complaints
        WHERE created_at >= CURRENT_DATE
          AND LOWER(council) = LOWER(:council)
    """, nativeQuery = true)
    List<Object[]> getTodayChartByCouncil(@Param("council") String council);

    @Query(value = """
        SELECT
            TO_CHAR(created_at, 'Dy')                      AS label,
            COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending_count,
            COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'APPROVED'))    AS accepted_count
        FROM complaints
        WHERE created_at >= :startDate
          AND LOWER(council) = LOWER(:council)
        GROUP BY TO_CHAR(created_at, 'Dy'), DATE_TRUNC('day', created_at)
        ORDER BY MIN(created_at)
    """, nativeQuery = true)
    List<Object[]> getWeekChartByCouncil(@Param("startDate") LocalDateTime startDate,
                                          @Param("council")   String council);

    @Query(value = """
        SELECT
            TO_CHAR(created_at, 'Mon DD')                  AS label,
            COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending_count,
            COUNT(*) FILTER (WHERE status IN ('ACCEPTED', 'APPROVED'))    AS accepted_count
        FROM complaints
        WHERE created_at >= :startDate
          AND LOWER(council) = LOWER(:council)
        GROUP BY TO_CHAR(created_at, 'Mon DD'), DATE_TRUNC('day', created_at)
        ORDER BY MIN(created_at)
    """, nativeQuery = true)
    List<Object[]> getMonthChartByCouncil(@Param("startDate") LocalDateTime startDate,
                                           @Param("council")   String council);
}