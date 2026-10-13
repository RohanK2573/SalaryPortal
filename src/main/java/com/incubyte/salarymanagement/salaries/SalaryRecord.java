package com.incubyte.salarymanagement.salaries;

import com.incubyte.salarymanagement.employees.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salary_record", uniqueConstraints = @UniqueConstraint(name = "uk_salary_employee_effective_date", columnNames = {"employee_id", "effective_date"}))
public class SalaryRecord {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @Column(name = "annual_base_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal annualBaseAmount;
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    @Version
    @Column(nullable = false)
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SalaryRecord() {}

    public SalaryRecord(UUID id, Employee employee, BigDecimal annualBaseAmount, String currencyCode,
            LocalDate effectiveDate, Instant now) {
        this.id = id;
        this.employee = employee;
        this.annualBaseAmount = annualBaseAmount;
        this.currencyCode = currencyCode;
        this.effectiveDate = effectiveDate;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void correct(BigDecimal amount, String currency, LocalDate date, Instant now) {
        this.annualBaseAmount = amount;
        this.currencyCode = currency;
        this.effectiveDate = date;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public Employee getEmployee() { return employee; }
    public BigDecimal getAnnualBaseAmount() { return annualBaseAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public Long getVersion() { return version; }
}
