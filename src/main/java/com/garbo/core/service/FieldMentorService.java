package com.garbo.core.service;

import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.FieldMentorRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FieldMentorService {
    @Autowired
    private FieldMentorRepository fieldMentorRepository;

    public FieldMentor saveFieldMentor(FieldMentor fieldMentor) {
        return fieldMentorRepository.save(fieldMentor);
    }
}
