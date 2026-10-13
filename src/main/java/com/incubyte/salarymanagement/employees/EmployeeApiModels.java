package com.incubyte.salarymanagement.employees;

import java.time.LocalDate;
import java.util.UUID;

public final class EmployeeApiModels {
    private EmployeeApiModels() {}

    public record CreateEmployeeRequest(String employeeCode, String fullName, String countryCode,
            String department, String jobTitle, String status, String annualBaseAmount,
            String currencyCode, String effectiveDate) {}

    public record EmployeeResponse(UUID id, String employeeCode, String fullName, String countryCode,
            String department, String jobTitle, EmployeeStatus status, long version, SalaryResponse currentSalary) {}

    public record SalaryResponse(UUID id, String annualBaseAmount, String currencyCode,
            LocalDate effectiveDate, long version) {}
}
