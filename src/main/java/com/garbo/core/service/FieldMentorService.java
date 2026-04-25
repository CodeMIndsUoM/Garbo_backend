package com.garbo.core.service;

import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.FieldMentorRepository;
import org.springframework.stereotype.Service;

@Service
public class FieldMentorService {

    final private FieldMentorRepository fieldMentorRepository;

    public FieldMentorService(FieldMentorRepository fieldMentorRepository) {
        this.fieldMentorRepository = fieldMentorRepository;
    }

    public FieldMentor saveFieldMentor(FieldMentor fieldMentor) {
        return this.fieldMentorRepository.save(fieldMentor);
    }

    public java.util.List<FieldMentor> getAll() {
        return this.fieldMentorRepository.findAll();
    }

    public java.util.List<FieldMentor> findByCouncil(String council) {
        return this.fieldMentorRepository.findByAssignedCouncil(council);
    }
}
