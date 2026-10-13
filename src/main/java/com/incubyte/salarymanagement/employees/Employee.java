package com.incubyte.salarymanagement.employees;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    private UUID id;
    @Column(name = "employee_code", nullable = false, unique = true, length = 64)
    private String employeeCode;
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;
    @Column(nullable = false, length = 120)
    private String department;
    @Column(name = "job_title", nullable = false, length = 120)
    private String jobTitle;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmployeeStatus status;
    @Version
    @Column(nullable = false)
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Employee() {}

    public Employee(UUID id, String employeeCode, String fullName, String countryCode, String department,
            String jobTitle, EmployeeStatus status, Instant now) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.fullName = fullName;
        this.countryCode = countryCode;
        this.department = department;
        this.jobTitle = jobTitle;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateProfile(String fullName, String countryCode, String department, String jobTitle,
            EmployeeStatus status, Instant now) {
        this.fullName = fullName;
        this.countryCode = countryCode;
        this.department = department;
        this.jobTitle = jobTitle;
        this.status = status;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getFullName() { return fullName; }
    public String getCountryCode() { return countryCode; }
    public String getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public EmployeeStatus getStatus() { return status; }
    public Long getVersion() { return version; }
}
