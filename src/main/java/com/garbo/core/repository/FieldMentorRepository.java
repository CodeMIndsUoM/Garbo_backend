package com.garbo.core.repository;

import com.garbo.core.entity.FieldMentor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldMentorRepository extends JpaRepository<FieldMentor, Long> {

}
