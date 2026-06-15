package com.garbo.core.repository;

import com.garbo.core.entity.Bin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BinRepository extends JpaRepository<Bin, Long> {

    @Query("SELECT b FROM Bin b WHERE b.id = :id")
    Bin findByIdCastToBigInt(@Param("id") Long id);

    @Query("SELECT b FROM Bin b WHERE b.id IN :ids")
    List<Bin> findAllByTextIdsCastToBigInt(@Param("ids") List<Long> ids);

    @Query(value = "SELECT * FROM bins WHERE id = :id", nativeQuery = true)
    Optional<Bin> findByNumericId(@Param("id") Long id);

    @Query("SELECT b FROM Bin b")
    List<Bin> findAllValidBins();

    @Query("SELECT b FROM Bin b LEFT JOIN FETCH b.assignedTo WHERE b.assignedTo.empId = :empId")
    List<Bin> findByAssignedToEmpId(@Param("empId") Long empId);
    List<Bin> findByCouncilIgnoreCase(String council);

    @Query("SELECT b FROM Bin b")
    Collection<Bin> findAllForMap();

    @Transactional
    @Modifying
    @Query("DELETE FROM Bin b WHERE b.id = :id")
    void deleteByIdNative(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("DELETE FROM Bin b WHERE b.id IN :ids")
    void deleteAllByIds(@Param("ids") List<Long> ids);

    @Transactional
    @Modifying
    @Query("UPDATE Bin b SET b.priority = :priority WHERE b.id = :id")
    void updatePriorityNative(@Param("id") Long id, @Param("priority") String priority);

    @Transactional
    @Modifying
    @Query("UPDATE Bin b SET b.zone = :safeZone WHERE b.id = :id")
    void updateZoneNative(@Param("id") Long id, @Param("safeZone") String safeZone);

    @Transactional
    @Modifying
    @Query(value = "UPDATE bins SET status = :status, fill_level = :fillLevel, last_checked = NOW() WHERE id = :binId", nativeQuery = true)
    int updateStatusForReport(@Param("binId") Long binId, @Param("status") String status, @Param("fillLevel") Integer fillLevel);

    @Transactional
    @Modifying
    @Query(value = "UPDATE bins SET status = 'notChecked', fill_level = 0, last_checked = NULL WHERE id = :binId", nativeQuery = true)
    int resetStatusForUndo(@Param("binId") Long binId);

    @Query("SELECT b FROM Bin b WHERE b.id IN :selected")
    List<Bin> findAllByIdWithCast(@Param("selected") List<Long> selected);

    @Transactional
    @Modifying
    @Query("UPDATE Bin b SET b.isAssigned = :isAssigned WHERE b.id = :id")
    void updateAssignedStatus(@Param("id") Long id, @Param("isAssigned") Boolean isAssigned);

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE bins
            SET status = 'empty', fill_level = 0, is_assigned = false, last_checked = NOW()
            WHERE id = :binId
            """, nativeQuery = true)
    int resetAfterCollection(@Param("binId") Long binId);

    // ── Analytics — council filtered ──────────────────────────────────────────

    @Query("SELECT b FROM Bin b WHERE LOWER(b.council) = LOWER(:council)")
    List<Bin> findAllByCouncil(@Param("council") String council);

    @Query("SELECT COUNT(b) FROM Bin b WHERE LOWER(b.status) = 'full'")
    long countFullBins();

    @Query("SELECT COUNT(b) FROM Bin b WHERE LOWER(b.status) = 'full' AND LOWER(b.council) = LOWER(:council)")
    long countFullBinsByCouncil(@Param("council") String council);

    @Query("SELECT DISTINCT b.zone FROM Bin b WHERE b.zone IS NOT NULL")
    List<String> findDistinctZones();

    @Query("SELECT DISTINCT b.zone FROM Bin b WHERE b.zone IS NOT NULL AND LOWER(b.council) = LOWER(:council)")
    List<String> findDistinctZonesByCouncil(@Param("council") String council);
}