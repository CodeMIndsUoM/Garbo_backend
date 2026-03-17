package com.garbo.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bins")
public class Bin {
    @Id
    @Column(length = 20)
    private String id;

    private String location;

    @Column(name = "type", length = 20)
    private String category; // public/park/commercial/medical/education

    @Column(length = 20)
    private String status = "notChecked"; // notChecked/full/half/empty

    @Column(name = "fill_level")
    private Integer fillLevel;

    @Column(length = 50)
    private String zone;

    @Column(name = "lat")
    private Double latitude;

    @Column(name = "lng")
    private Double longitude;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    private String priority;

    @ManyToOne
    @JoinColumn(name = "assigned_to", referencedColumnName = "emp_id", nullable = true)
    private FieldMentor assignedTo;
}
