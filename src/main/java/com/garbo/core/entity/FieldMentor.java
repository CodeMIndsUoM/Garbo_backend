package com.garbo.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "field_mentors")
@PrimaryKeyJoinColumn(name = "emp_id")
public class FieldMentor extends User {
    private String assignedCouncil;
    private String workShift;
    private boolean onDuty;
    private double rewardPoints;

    @Column(name = "admin_hidden")
    private Boolean adminHidden = false;
    public void setAssignedZone(String string) {
        this.assignedCouncil = string;
    }
}
