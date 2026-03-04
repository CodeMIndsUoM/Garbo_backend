/*
package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "admins")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_id")
    private Long empId;
    private String email;
    private String password;
    private String council;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    public String getEmail() {
        return email;
    }
}

 */

package com.garbo.core.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "admins_v2")
public class Admin {
    @Id
    @Column(name = "emp_id")
    private Long empId;
    private String email;
    private String password;
    private String council;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    @Column(name = "can_create_users")
    private Boolean can_create_users = false;
    
    @Column(name = "can_manage_garbage")
    private Boolean can_manage_garbage = false;
    
    @Column(name = "can_view_reports")
    private Boolean can_view_reports = false;
    
    @Column(name = "can_edit_users")
    private Boolean can_edit_users = false;
    
    @Column(name = "can_delete_users")
    private Boolean can_delete_users = false;
    
    @Column(name = "can_assign_tasks")
    private Boolean can_assign_tasks = false;
    
    @Column(name = "role")
    private String role;
    
    @Column(name = "is_active")
    private Boolean is_active = true;
    public String getEmail() {
        return email;
    }
}
