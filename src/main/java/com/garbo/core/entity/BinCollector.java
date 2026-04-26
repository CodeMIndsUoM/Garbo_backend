package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "bin_collectors")
@PrimaryKeyJoinColumn(name = "emp_id")
public class BinCollector extends User {
    private String assignedCouncil;
    private String team;
    private String workShift;
    private boolean onDuty;
    private int completedCollections;
    private int missedCollections;
    private double rewardPoints;
}
