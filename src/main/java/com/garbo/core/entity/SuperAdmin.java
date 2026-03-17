package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "super_admins")
@PrimaryKeyJoinColumn(name = "emp_id")
public class SuperAdmin extends User {
    private String designation;
}
