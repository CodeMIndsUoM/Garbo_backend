package com.garbo.core.repository;

import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.User;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // ── Basic finders (HEAD) ──────────────────────────────────────────────────

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


    // ── Dashboard analytics (kevin-RWS) ──────────────────────────────────────

    // 1. TODAY summary — KPI cards (always fixed to today)
    // Row: [newCount, inProgressCount, resolvedCount]
    @Query(value = """
        SELECT
            COUNT(*) FILTER (WHERE status = 'new')         AS new_count,
            COUNT(*) FILTER (WHERE status = 'inprogress')  AS in_progress,
            COUNT(*) FILTER (WHERE status = 'completed')   AS resolved
        FROM complaints
        WHERE created_at >= CURRENT_DATE
    """, nativeQuery = true)
    List<Object[]> getTodaySummary();

    // 2. TODAY chart — single bar: new / inprogress / completed today
    @Query(value = """
        SELECT
            'Today'                                        AS label,
            COUNT(*) FILTER (WHERE status = 'new')         AS new_count,
            COUNT(*) FILTER (WHERE status = 'inprogress')  AS in_progress,
            COUNT(*) FILTER (WHERE status = 'completed')   AS resolved
        FROM complaints
        WHERE created_at >= CURRENT_DATE
    """, nativeQuery = true)
    List<Object[]> getTodayChart();

    // 3. LAST 7 DAYS — grouped by day name (Mon, Tue …)
    @Query(value = """
        SELECT
            TO_CHAR(created_at, 'Dy')                      AS label,
            COUNT(*) FILTER (WHERE status = 'new')         AS new_count,
            COUNT(*) FILTER (WHERE status = 'inprogress')  AS in_progress,
            COUNT(*) FILTER (WHERE status = 'completed')   AS resolved
        FROM complaints
        WHERE created_at >= :startDate
        GROUP BY TO_CHAR(created_at, 'Dy'), DATE_TRUNC('day', created_at)
        ORDER BY MIN(created_at)
    """, nativeQuery = true)
    List<Object[]> getWeekChart(@Param("startDate") LocalDateTime startDate);

    // 4. LAST 30 DAYS — grouped by date label (Mar 01, Mar 05 …)
    @Query(value = """
        SELECT
            TO_CHAR(created_at, 'Mon DD')                  AS label,
            COUNT(*) FILTER (WHERE status = 'new')         AS new_count,
            COUNT(*) FILTER (WHERE status = 'inprogress')  AS in_progress,
            COUNT(*) FILTER (WHERE status = 'completed')   AS resolved
        FROM complaints
        WHERE created_at >= :startDate
        GROUP BY TO_CHAR(created_at, 'Mon DD'), DATE_TRUNC('day', created_at)
        ORDER BY MIN(created_at)
    """, nativeQuery = true)
    List<Object[]> getMonthChart(@Param("startDate") LocalDateTime startDate);
}