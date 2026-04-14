package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "field_mentors")
@PrimaryKeyJoinColumn(name = "emp_id")
public class FieldMentor extends User {
    private String assignedZone;
    private String workShift;
    private boolean onDuty;
    private double rewardPoints;
}
