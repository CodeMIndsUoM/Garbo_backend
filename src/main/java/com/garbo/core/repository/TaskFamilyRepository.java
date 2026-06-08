package com.garbo.core.repository;

import com.garbo.core.entity.TaskFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskFamilyRepository extends JpaRepository<TaskFamily, Long> {
    Optional<TaskFamily> findByCodeIgnoreCase(String code);
}
