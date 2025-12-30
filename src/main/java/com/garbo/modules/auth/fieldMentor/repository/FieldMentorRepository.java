package com.garbo.modules.auth.fieldMentor.repository;

import com.garbo.modules.auth.fieldMentor.model.FieldMentor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldMentorRepository extends JpaRepository<FieldMentor, Long> {

}
