package com.garbo.core.repository;

import com.garbo.core.entity.Bin;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BinRepository extends JpaRepository<Bin, Long> {

    @Query(
	    value = """
		    SELECT *
		    FROM bins
		    WHERE id ~ '^[0-9]+$'
		      AND CAST(id AS BIGINT) = :id
		    """,
	    nativeQuery = true
    )
    Bin findByIdCastToBigInt(@Param("id") Long id);

    @Query(
	    value = """
		    SELECT *
		    FROM bins
		    WHERE id ~ '^[0-9]+$'
		      AND CAST(id AS BIGINT) IN (:ids)
		    """,
	    nativeQuery = true
    )
    List<Bin> findAllByTextIdsCastToBigInt(@Param("ids") List<Long> ids);

}

