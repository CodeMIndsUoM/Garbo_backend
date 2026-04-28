package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "admins")
@PrimaryKeyJoinColumn(name = "emp_id")
public class Admin extends User {
    private String designation;
    private String department;
    private String workShift;
    private boolean canCreateUsers;
    private boolean canDeleteUsers;
}
