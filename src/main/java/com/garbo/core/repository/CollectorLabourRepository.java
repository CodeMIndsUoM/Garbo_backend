package com.garbo.core.repository;

import com.garbo.core.entity.CollectorLabour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CollectorLabourRepository extends JpaRepository<CollectorLabour, Long> {
    List<CollectorLabour> findByCouncilIgnoreCase(String council);
}
