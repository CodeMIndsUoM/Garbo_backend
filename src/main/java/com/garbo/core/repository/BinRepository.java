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

    List<Bin> findAllByIdIn(List<Long> ids);

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
}












/*package com.garbo.core.repository;

import com.garbo.core.entity.Bin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BinRepository extends JpaRepository<Bin, Long> {

}
*/

