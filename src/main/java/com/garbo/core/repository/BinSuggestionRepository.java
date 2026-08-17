package com.garbo.core.repository;

import com.garbo.core.entity.BinSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BinSuggestionRepository extends JpaRepository<BinSuggestion, Long> {

    @Transactional
    @Modifying
    @Query("UPDATE BinSuggestion s SET s.imageUrl = :photoUrl WHERE s.id = :id")
    int updatePhotoUrl(@Param("id") Long id, @Param("photoUrl") String photoUrl);

    List<BinSuggestion> findByMentorIdOrderByCreatedAtDesc(Long mentorId);

    List<BinSuggestion> findByCouncilIgnoreCaseOrderByCreatedAtDesc(String council);

    Optional<BinSuggestion> findByIdAndCouncilIgnoreCase(Long id, String council);
}
