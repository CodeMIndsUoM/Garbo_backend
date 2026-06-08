package com.garbo.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
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
    private String council;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "council_id")
    private Council councilEntity;

}
