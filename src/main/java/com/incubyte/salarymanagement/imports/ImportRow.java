package com.incubyte.salarymanagement.imports;

import com.incubyte.salarymanagement.employees.Employee;
import com.incubyte.salarymanagement.salaries.SalaryRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "import_row", uniqueConstraints = @UniqueConstraint(name = "uk_import_row_batch_number",
        columnNames = {"batch_id", "row_number"}))
public class ImportRow {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private ImportBatch batch;
    @Column(name = "row_number", nullable = false)
    private int rowNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;
    @Column(name = "employee_version")
    private Long employeeVersion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_salary_record_id")
    private SalaryRecord latestSalaryRecord;
    @Column(name = "latest_salary_version")
    private Long latestSalaryVersion;
    @Column(name = "employee_code", nullable = false, columnDefinition = "text")
    private String employeeCode;
    @Column(name = "full_name", columnDefinition = "text")
    private String fullName;
    @Column(name = "country_code", columnDefinition = "text")
    private String countryCode;
    @Column(columnDefinition = "text")
    private String department;
    @Column(name = "job_title", columnDefinition = "text")
    private String jobTitle;
    @Column(name = "employee_status", columnDefinition = "text")
    private String employeeStatus;
    @Column(name = "annual_base_amount", columnDefinition = "text")
    private String annualBaseAmount;
    @Column(name = "currency_code", columnDefinition = "text")
    private String currencyCode;
    @Column(name = "effective_date", columnDefinition = "text")
    private String effectiveDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImportAction action;
    @Column(name = "errors_json", nullable = false, columnDefinition = "text")
    private String errorsJson;
    @Column(name = "profile_changed", nullable = false)
    private boolean profileChanged;
    @Column(name = "salary_record_proposed", nullable = false)
    private boolean salaryRecordProposed;

    protected ImportRow() {}

    public ImportRow(UUID id, ImportBatch batch, int rowNumber, Employee employee, Long employeeVersion,
            SalaryRecord latestSalaryRecord, Long latestSalaryVersion, String employeeCode, String fullName,
            String countryCode, String department, String jobTitle, String employeeStatus,
            String annualBaseAmount, String currencyCode, String effectiveDate, ImportAction action,
            String errorsJson, boolean profileChanged, boolean salaryRecordProposed) {
        this.id = id;
        this.batch = batch;
        this.rowNumber = rowNumber;
        this.employee = employee;
        this.employeeVersion = employeeVersion;
        this.latestSalaryRecord = latestSalaryRecord;
        this.latestSalaryVersion = latestSalaryVersion;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.countryCode = countryCode;
        this.department = department;
        this.jobTitle = jobTitle;
        this.employeeStatus = employeeStatus;
        this.annualBaseAmount = annualBaseAmount;
        this.currencyCode = currencyCode;
        this.effectiveDate = effectiveDate;
        this.action = action;
        this.errorsJson = errorsJson;
        this.profileChanged = profileChanged;
        this.salaryRecordProposed = salaryRecordProposed;
    }

    public UUID getId() { return id; }
    public ImportBatch getBatch() { return batch; }
    public int getRowNumber() { return rowNumber; }
    public Employee getEmployee() { return employee; }
    public Long getEmployeeVersion() { return employeeVersion; }
    public SalaryRecord getLatestSalaryRecord() { return latestSalaryRecord; }
    public Long getLatestSalaryVersion() { return latestSalaryVersion; }
    public String getEmployeeCode() { return employeeCode; }
    public String getFullName() { return fullName; }
    public String getCountryCode() { return countryCode; }
    public String getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public String getEmployeeStatus() { return employeeStatus; }
    public String getAnnualBaseAmount() { return annualBaseAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public String getEffectiveDate() { return effectiveDate; }
    public ImportAction getAction() { return action; }
    public String getErrorsJson() { return errorsJson; }
    public boolean isProfileChanged() { return profileChanged; }
    public boolean isSalaryRecordProposed() { return salaryRecordProposed; }
}
