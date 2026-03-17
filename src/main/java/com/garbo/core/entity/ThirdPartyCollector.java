package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "third_party_collectors")
@PrimaryKeyJoinColumn(name = "emp_id")
public class ThirdPartyCollector extends User {
    private String NIC;
    private String company;
    private String contractId;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private int completedRequests;
}
