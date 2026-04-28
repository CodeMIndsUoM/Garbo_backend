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

    // Mobile APIs use numeric path IDs, while the bins table stores id as text.
    // Cast the numeric input to text so lookup works without schema changes.
    @Query(value = "SELECT * FROM bins WHERE id = CAST(:id AS TEXT)", nativeQuery = true)
    Optional<Bin> findByNumericId(@Param("id") Long id);

    @Query("SELECT b FROM Bin b")
    List<Bin> findAllValidBins();

    List<Bin> findByAssignedToEmpId(Long empId);

    @Query("SELECT b FROM Bin b")
    Collection<Bin> findAllForMap();

    @Transactional
    @Modifying
    @Query("DELETE FROM Bin b WHERE b.id = :id")
    void deleteByIdNative(@Param("id") Long id);

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
    // Avoid JPA id-type mismatch on update by using a native query with text cast.
    @Query(value = "UPDATE bins SET status = :status, fill_level = :fillLevel, last_checked = NOW() WHERE id = CAST(:binId AS TEXT)", nativeQuery = true)
    int updateStatusForReport(@Param("binId") Long binId, @Param("status") String status, @Param("fillLevel") Integer fillLevel);

    @Transactional
    @Modifying
    // Undo returns the bin to default "notChecked" state for field-staff flow.
    @Query(value = "UPDATE bins SET status = 'notChecked', fill_level = 0, last_checked = NULL WHERE id = CAST(:binId AS TEXT)", nativeQuery = true)
    int resetStatusForUndo(@Param("binId") Long binId);

    @Query("SELECT b FROM Bin b WHERE b.id IN :selected")
    List<Bin> findAllByIdWithCast(@Param("selected") List<Long> selected);

}

