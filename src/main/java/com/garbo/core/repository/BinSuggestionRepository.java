package com.garbo.core.repository;

import com.garbo.core.entity.BinSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BinSuggestionRepository extends JpaRepository<BinSuggestion, Long> {

    List<BinSuggestion> findByMentorIdOrderByCreatedAtDesc(Long mentorId);

    List<BinSuggestion> findByCouncilIgnoreCaseOrderByCreatedAtDesc(String council);

    Optional<BinSuggestion> findByIdAndCouncilIgnoreCase(Long id, String council);
}
