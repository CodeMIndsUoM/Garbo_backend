package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "fieldMentors")
public class FieldMentor {
    @Id
    private Long empId;
    private String email;
    private String password;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
