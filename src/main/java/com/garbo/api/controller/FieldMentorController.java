package com.garbo.api.controller;

import com.garbo.core.entity.FieldMentor;
import com.garbo.core.service.FieldMentorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fieldmentors")
public class FieldMentorController {

    final private FieldMentorService fieldMentorService;

    public FieldMentorController(FieldMentorService fieldMentorService) {
        this.fieldMentorService = fieldMentorService;
    }

    @PostMapping
    public void createFieldMentor(@RequestBody FieldMentor fieldMentor) {
        System.out.println("admin saved successfully");
        fieldMentorService.saveFieldMentor(fieldMentor);
    }
}
