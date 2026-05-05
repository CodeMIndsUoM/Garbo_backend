package com.garbo.core.repository;

import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThirdPartyCollectorRepository extends JpaRepository<ThirdPartyCollector, Long> {

    Optional<ThirdPartyCollector> findByEmailIgnoreCase(String email);

    List<ThirdPartyCollector> findByRegistrationStatus(RegistrationStatus status);

    @Query("SELECT DISTINCT a.council FROM AdminNew a WHERE a.council IS NOT NULL AND a.council <> ''")
    List<String> findDistinctCouncils();
}
