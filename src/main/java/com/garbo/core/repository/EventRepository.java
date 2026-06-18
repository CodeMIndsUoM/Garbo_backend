package com.garbo.core.repository;

import com.garbo.core.entity.Event;
import com.garbo.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCouncilIgnoreCaseAndStatusInOrderByEventDateAsc(String council, List<String> statuses);

    Optional<Event> findByIdAndCouncilIgnoreCase(Long id, String council);

    List<Event> findByOrganizerCitizenOrderByCreatedAtDesc(User organizerCitizen);

    List<Event> findByCouncilIgnoreCaseAndStatusOrderByCreatedAtDesc(String council, String status);

    List<Event> findByCouncilIgnoreCaseOrderByEventDateAsc(String council);
}
