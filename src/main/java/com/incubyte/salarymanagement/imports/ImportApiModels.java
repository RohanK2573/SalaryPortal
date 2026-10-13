package com.incubyte.salarymanagement.imports;

import com.incubyte.salarymanagement.api.ApiPage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ImportApiModels {
    private ImportApiModels() {}

    public record ImportSummary(UUID id, ImportStatus status, Instant createdAt, Instant expiresAt,
            Instant committedAt, int totalRowCount, int createCount, int updateCount,
            int noOpCount, int invalidCount) {}

    public record ImportError(String field, String message) {}

    public record ImportRowResponse(int rowNumber, String employeeCode, ImportAction action,
            String fullName, String countryCode, String department, String jobTitle,
            String status, String annualBaseAmount, String currencyCode, String effectiveDate,
            List<ImportError> errors) {
        public ImportRowResponse { errors = List.copyOf(errors); }
    }

    public record ImportDetailsResponse(ImportSummary batch, ApiPage<ImportRowResponse> rows) {}
}
