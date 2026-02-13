package com.garbo.core.repository;

import com.garbo.core.entity.ThirdPartyCollector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThirdPartyCollectorRepository extends JpaRepository<ThirdPartyCollector, Long> {
}
