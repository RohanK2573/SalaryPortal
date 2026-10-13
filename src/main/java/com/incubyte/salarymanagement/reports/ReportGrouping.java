package com.incubyte.salarymanagement.reports;

import com.incubyte.salarymanagement.api.ApiException;
import java.util.Locale;

enum ReportGrouping {
    COUNTRY("country", ""),
    COUNTRY_JOB_TITLE("countryJobTitle", ", job_title"),
    COUNTRY_DEPARTMENT("countryDepartment", ", department");

    private final String apiValue;
    private final String additionalColumn;

    ReportGrouping(String apiValue, String additionalColumn) {
        this.apiValue = apiValue;
        this.additionalColumn = additionalColumn;
    }

    String additionalColumn() {
        return additionalColumn;
    }

    static ReportGrouping parse(String value) {
        String normalized = value == null || value.isBlank() ? "country" : value.trim();
        for (ReportGrouping grouping : values()) {
            if (grouping.apiValue.equals(normalized)) return grouping;
        }
        throw ApiException.invalid("groupBy", "Use country, countryJobTitle, or countryDepartment.");
    }

    static String normalizeDepartmentFilter(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 120) {
            throw ApiException.invalid("department", "Department filter cannot exceed 120 characters.");
        }
        return normalized;
    }
}
