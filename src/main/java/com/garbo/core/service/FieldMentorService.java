package com.garbo.core.service;

import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.FieldMentorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FieldMentorService {

    final private FieldMentorRepository fieldMentorRepository;
    final private PasswordEncoder passwordEncoder;


    public FieldMentorService(FieldMentorRepository fieldMentorRepository, PasswordEncoder passwordEncoder) {
        this.fieldMentorRepository = fieldMentorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public FieldMentor saveFieldMentor(FieldMentor fieldMentor) {
        fieldMentor.setPassword(passwordEncoder.encode(fieldMentor.getPassword()));
        return this.fieldMentorRepository.save(fieldMentor);
    }

    public FieldMentor getFieldMentor(Long empId) {
        return this.fieldMentorRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("Field Mentor not found with id: " + empId));
    }

    public List<FieldMentor> getAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }

    public List<FieldMentor> findByCouncil(String council) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByCouncil'");
    }
}
