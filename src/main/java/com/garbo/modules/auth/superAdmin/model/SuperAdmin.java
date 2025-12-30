package com.garbo.modules.auth.superAdmin.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "SuperAdmin")
public class SuperAdmin {
    @Id
    private Long empId;
    private String email;
    private String password;
    public String getEmail() {
        return email;
    }
}
