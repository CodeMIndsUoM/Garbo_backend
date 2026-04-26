package com.garbo.core.repository;

import com.garbo.core.entity.Bin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BinRepository extends JpaRepository<Bin, Long> {

    interface BinMapRow {
        Long getId();
        Double getLat();
        Double getLng();
        Integer getFillLevel();
        String getPriority();
        String getZone();
    }

    List<Bin> findAllByIdIn(List<Long> ids);

    @Query(value = """
            SELECT
                CAST(id AS BIGINT) AS id,
                lat AS lat,
                lng AS lng,
                fill_level AS fillLevel,
                priority AS priority,
                zone AS zone
            FROM bins
            """, nativeQuery = true)
    List<BinMapRow> findAllForMap();

    // bins.id is varchar in current DB, while app sends numeric ids.
    // Cast column to bigint for compatibility during transition.
    @Query(value = "SELECT * FROM bins b WHERE CAST(b.id AS BIGINT) IN (:ids)", nativeQuery = true)
    List<Bin> findAllByIdWithCast(@Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM bins WHERE id = CAST(:id AS VARCHAR)", nativeQuery = true)
    void deleteByIdNative(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE bins SET priority = :priority WHERE id = CAST(:id AS VARCHAR)", nativeQuery = true)
    void updatePriorityNative(@Param("id") Long id, @Param("priority") String priority);

    @Modifying
    @Transactional
    @Query(value = "UPDATE bins SET zone = :zone WHERE id = CAST(:id AS VARCHAR)", nativeQuery = true)
    void updateZoneNative(@Param("id") Long id, @Param("zone") String zone);

    @Query("SELECT DISTINCT b.zone FROM Bin b")
    List<String> findDistinctZones();

    List<Bin> findByZone(String zone);
    @Query("SELECT b FROM Bin b WHERE b.zone IN ('A','B','C','D','E')")
    List<Bin> findAllValidBins();
}













