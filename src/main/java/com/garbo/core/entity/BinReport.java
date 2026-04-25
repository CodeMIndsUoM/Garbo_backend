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
@Table(name = "bin_reports")
public class BinReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id", nullable = false)
    private Bin bin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", referencedColumnName = "emp_id", nullable = true)
    private FieldMentor reporter;

    @Column(length = 20)
    private String source = "MANUAL"; // MANUAL, IOT, ANONYMOUS

    @Column(name = "fill_level", nullable = false)
    private Integer fillLevel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Double latitude;
    private Double longitude;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    @PrePersist
    protected void onCreate() {
        if (reportedAt == null) {
            reportedAt = LocalDateTime.now();
        }
    }
}