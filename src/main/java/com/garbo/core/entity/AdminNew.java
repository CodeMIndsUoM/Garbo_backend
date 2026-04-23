package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "admins_new")
@PrimaryKeyJoinColumn(name = "emp_id")
public class AdminNew extends User {
    // New admin-specific field for the superadmin-created admin flow
    private String council;

}
