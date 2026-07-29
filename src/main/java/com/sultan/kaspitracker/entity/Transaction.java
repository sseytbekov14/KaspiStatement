package com.sultan.kaspitracker.entity;

import com.sultan.kaspitracker.parser.OperationType;
import com.sultan.kaspitracker.parser.SignType;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id", nullable = false)
    private Statement statement;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SignType sign;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 50)
    private OperationType operationType;

    @Column(name = "merchant_details", nullable = false, columnDefinition = "TEXT")
    private String merchantDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // nullable, assigned during categorization in Milestone 5

    protected Transaction() {
        // JPA required no-args constructor
    }

    public Transaction(Statement statement, LocalDate date, SignType sign, BigDecimal amount, OperationType operationType, String merchantDetails) {
        this.statement = statement;
        this.date = date;
        this.sign = sign;
        this.amount = amount;
        this.operationType = operationType;
        this.merchantDetails = merchantDetails;
    }

    public Long getId() {
        return id;
    }

    public Statement getStatement() {
        return statement;
    }

    public LocalDate getDate() {
        return date;
    }

    public SignType getSign() {
        return sign;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public String getMerchantDetails() {
        return merchantDetails;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
