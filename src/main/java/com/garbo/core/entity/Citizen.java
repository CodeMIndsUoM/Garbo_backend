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
@Table(name = "citizens")
@PrimaryKeyJoinColumn(name = "emp_id")
public class Citizen extends User {
    private String address;
    private String area;
    private String council;
    private int reportCount;
}
