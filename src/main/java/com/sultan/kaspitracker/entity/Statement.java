package com.sultan.kaspitracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "statements")
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_hash", nullable = false, unique = true)
    private String fileHash;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected Statement() {
        // JPA required no-args constructor
    }

    public Statement(String fileHash, LocalDate periodStart, LocalDate periodEnd, Instant uploadedAt) {
        this.fileHash = fileHash;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public String getFileHash() {
        return fileHash;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
