package com.incubyte.salarymanagement.salaries;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    private UUID id;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @Column(name = "salary_record_id", nullable = false)
    private UUID salaryRecordId;
    @Column(name = "actor_email", nullable = false, length = 254)
    private String actorEmail;
    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;
    @Column(nullable = false, length = 32)
    private String source;
    @Column(length = 500)
    private String reason;
    @Column(name = "before_amount", precision = 19, scale = 4)
    private BigDecimal beforeAmount;
    @Column(name = "before_currency", length = 3)
    private String beforeCurrency;
    @Column(name = "before_effective_date")
    private LocalDate beforeEffectiveDate;
    @Column(name = "after_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal afterAmount;
    @Column(name = "after_currency", nullable = false, length = 3)
    private String afterCurrency;
    @Column(name = "after_effective_date", nullable = false)
    private LocalDate afterEffectiveDate;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {}

    private AuditEvent(UUID employeeId, UUID salaryRecordId, String actorEmail, String eventType,
            String reason, BigDecimal beforeAmount, String beforeCurrency, LocalDate beforeDate,
            BigDecimal afterAmount, String afterCurrency, LocalDate afterDate, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.employeeId = employeeId;
        this.salaryRecordId = salaryRecordId;
        this.actorEmail = actorEmail;
        this.eventType = eventType;
        this.source = "MANUAL";
        this.reason = reason;
        this.beforeAmount = beforeAmount;
        this.beforeCurrency = beforeCurrency;
        this.beforeEffectiveDate = beforeDate;
        this.afterAmount = afterAmount;
        this.afterCurrency = afterCurrency;
        this.afterEffectiveDate = afterDate;
        this.createdAt = createdAt;
    }

    public static AuditEvent salaryCreated(SalaryRecord record, String actorEmail, Instant now) {
        return new AuditEvent(record.getEmployee().getId(), record.getId(), actorEmail, "SALARY_CREATED", null,
                null, null, null, record.getAnnualBaseAmount(), record.getCurrencyCode(), record.getEffectiveDate(), now);
    }

    public static AuditEvent salaryCorrected(SalaryRecord record, String actorEmail, String reason,
            BigDecimal beforeAmount, String beforeCurrency, LocalDate beforeDate, Instant now) {
        return new AuditEvent(record.getEmployee().getId(), record.getId(), actorEmail, "SALARY_CORRECTED", reason,
                beforeAmount, beforeCurrency, beforeDate, record.getAnnualBaseAmount(), record.getCurrencyCode(),
                record.getEffectiveDate(), now);
    }
}
