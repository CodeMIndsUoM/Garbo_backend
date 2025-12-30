package com.garbo.modules.auth.fieldMentor.controller;

import com.garbo.modules.auth.admin.model.Admin;
import com.garbo.modules.auth.admin.service.AdminService;
import com.garbo.modules.auth.fieldMentor.model.FieldMentor;
import com.garbo.modules.auth.fieldMentor.service.FieldMentorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fieldMentor")
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
