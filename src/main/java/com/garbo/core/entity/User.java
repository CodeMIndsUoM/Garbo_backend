package com.garbo.core.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_id")
    private Long empId;

    @Column(name = "full_name")
    private String empName;

    @Column(name = "email")
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "role")
    private String role;

    @Column(name = "contact_number")
    private String phone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "default_address", length = 500)
    private String defaultAddress;

    @Column(name = "must_change_password")
    private boolean mustChangePassword = false;

    // ================= Security methods =================

    // @Override
    // public Collection<? extends GrantedAuthority> getAuthorities() {
    // return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    // }

    public Long getEmpId() {
        return empId;
    }

    public String getEmpName() {
        return empName;
    }

    public String getEmail() {
        return email; // login using email
    }

    public String getUsername() {
        return email; // login using email
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(String defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }
}
