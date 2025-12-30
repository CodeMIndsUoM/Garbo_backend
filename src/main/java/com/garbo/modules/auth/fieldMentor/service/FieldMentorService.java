package com.garbo.modules.auth.fieldMentor.service;

import com.garbo.modules.auth.fieldMentor.model.FieldMentor;
import com.garbo.modules.auth.fieldMentor.repository.FieldMentorRepository;
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
}
