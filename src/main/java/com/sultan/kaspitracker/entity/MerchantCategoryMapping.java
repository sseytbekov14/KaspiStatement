package com.sultan.kaspitracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "merchant_category_mappings")
public class MerchantCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_pattern", nullable = false, unique = true)
    private String merchantPattern;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MappingSource source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MerchantCategoryMapping() {
        // JPA required no-args constructor
    }

    public MerchantCategoryMapping(String merchantPattern, Category category, MappingSource source, Instant createdAt) {
        this.merchantPattern = merchantPattern;
        this.category = category;
        this.source = source;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getMerchantPattern() {
        return merchantPattern;
    }

    public Category getCategory() {
        return category;
    }

    public MappingSource getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
