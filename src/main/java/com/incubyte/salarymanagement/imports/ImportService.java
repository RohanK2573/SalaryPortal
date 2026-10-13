package com.incubyte.salarymanagement.imports;

import com.incubyte.salarymanagement.api.ApiException;
import com.incubyte.salarymanagement.api.ApiFieldError;
import com.incubyte.salarymanagement.api.ApiPage;
import com.incubyte.salarymanagement.api.PaginationParameters;
import com.incubyte.salarymanagement.employees.Employee;
import com.incubyte.salarymanagement.employees.EmployeeRepository;
import com.incubyte.salarymanagement.employees.EmployeeStatus;
import com.incubyte.salarymanagement.salaries.AuditEvent;
import com.incubyte.salarymanagement.salaries.AuditEventRepository;
import com.incubyte.salarymanagement.salaries.SalaryRecord;
import com.incubyte.salarymanagement.salaries.SalaryRecordRepository;
import com.incubyte.salarymanagement.salaries.SalaryRules;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.incubyte.salarymanagement.imports.ImportApiModels.*;

@Service
public class ImportService {
    private static final List<String> HEADERS = List.of("employee_code", "full_name", "country_code", "department",
            "job_title", "status", "annual_base_amount", "currency_code", "effective_date");

    private final ImportBatchRepository batches;
    private final ImportRowRepository importRows;
    private final EmployeeRepository employees;
    private final SalaryRecordRepository salaries;
    private final AuditEventRepository auditEvents;
    private final SalaryRules salaryRules;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ImportService(ImportBatchRepository batches, ImportRowRepository importRows, EmployeeRepository employees,
            SalaryRecordRepository salaries, AuditEventRepository auditEvents, SalaryRules salaryRules,
            ObjectMapper objectMapper, Clock clock) {
        this.batches = batches;
        this.importRows = importRows;
        this.employees = employees;
        this.salaries = salaries;
        this.auditEvents = auditEvents;
        this.salaryRules = salaryRules;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public ImportSummary preview(MultipartFile file, String ownerEmail) {
        if (file == null || file.isEmpty()) throw ApiException.invalid("file", "A non-empty CSV file is required.");
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "CSV file must not exceed 10 MiB.", List.of());
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw ApiException.invalid("file", "CSV file could not be read.");
        }
        List<List<String>> records = CsvFileParser.parse(bytes);
        Map<String, Integer> columnIndex = headerIndex(records.getFirst());
        List<DraftRow> drafts = new ArrayList<>(records.size() - 1);
        for (int i = 1; i < records.size(); i++) {
            drafts.add(validateRow(i + 1, records.get(i), columnIndex));
        }
        markFileDuplicates(drafts);

        List<String> candidateCodes = drafts.stream().filter(draft -> draft.errors.isEmpty())
                .map(draft -> draft.employeeCode).distinct().toList();
        Map<String, Employee> employeeByCode = candidateCodes.isEmpty() ? Map.of()
                : employees.findAllByEmployeeCodeIn(candidateCodes).stream()
                        .collect(Collectors.toMap(Employee::getEmployeeCode, Function.identity()));
        List<UUID> existingIds = drafts.stream().filter(draft -> draft.errors.isEmpty())
                .map(draft -> employeeByCode.get(draft.employeeCode)).filter(java.util.Objects::nonNull)
                .map(Employee::getId).distinct().toList();
        LocalDate today = LocalDate.now(clock);
        Map<UUID, SalaryRecord> latestSalaryByEmployee = existingIds.isEmpty() ? Map.of()
                : salaries.findCurrentForEmployees(existingIds, today).stream()
                        .collect(Collectors.toMap(record -> record.getEmployee().getId(), Function.identity()));

        int creates = 0;
        int updates = 0;
        int noOps = 0;
        int invalid = 0;
        for (DraftRow draft : drafts) {
            Employee employee = draft.errors.isEmpty() ? employeeByCode.get(draft.employeeCode) : null;
            SalaryRecord latest = employee == null ? null : latestSalaryByEmployee.get(employee.getId());
            if (draft.errors.isEmpty()) classify(draft, employee, latest);
            if (draft.action == ImportAction.CREATE) creates++;
            else if (draft.action == ImportAction.UPDATE) updates++;
            else if (draft.action == ImportAction.NO_OP) noOps++;
            else invalid++;
        }
        UUID batchId = UUID.randomUUID();
        Instant now = clock.instant();
        ImportBatch batch = new ImportBatch(batchId, ownerEmail, sha256(bytes), now, now.plusSeconds(24 * 60 * 60),
                drafts.size(), creates, updates, noOps, invalid);
        List<ImportRow> stagedRows = new ArrayList<>(drafts.size());
        for (DraftRow draft : drafts) {
            Employee employee = draft.errors.isEmpty() ? employeeByCode.get(draft.employeeCode) : null;
            SalaryRecord latest = employee == null ? null : latestSalaryByEmployee.get(employee.getId());
            stagedRows.add(new ImportRow(UUID.randomUUID(), batch, draft.rowNumber, employee,
                    employee == null ? null : employee.getVersion(), latest,
                    latest == null ? null : latest.getVersion(), draft.employeeCode, draft.fullName,
                    draft.countryCode, draft.department, draft.jobTitle, draft.status,
                    draft.amount == null ? draft.rawAmount : draft.amount.toPlainString(), draft.currencyCode,
                    draft.effectiveDate == null ? draft.rawEffectiveDate : draft.effectiveDate.toString(),
                    draft.action, serializeErrors(draft.errors), draft.profileChanged,
                    draft.salaryRecordProposed));
        }
        batches.saveAndFlush(batch);
        importRows.saveAll(stagedRows);
        return summary(batch);
    }

    @Transactional(readOnly = true)
    public ImportDetailsResponse get(UUID id, String ownerEmail, int page, int size) {
        new PaginationParameters(page, size);
        ImportBatch batch = batches.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> ApiException.notFound("Import batch was not found."));
        if (batch.getStatus() == ImportStatus.EXPIRED
                || (batch.getStatus() == ImportStatus.PREVIEWED && !batch.getExpiresAt().isAfter(clock.instant()))) {
            throw new ApiException(HttpStatus.GONE, "This import preview has expired. Upload a fresh CSV.", List.of());
        }
        Page<ImportRow> rowPage = importRows.findAllByBatchIdOrderByRowNumberAsc(id,
                PageRequest.of(page - 1, size));
        return new ImportDetailsResponse(summary(batch), ApiPage.from(rowPage.map(this::rowResponse)));
    }

    @Transactional
    public ImportSummary commit(UUID id, String ownerEmail) {
        ImportBatch batch = batches.findLockedByIdAndOwner(id, ownerEmail)
                .orElseThrow(() -> ApiException.notFound("Import batch was not found."));
        if (batch.getStatus() == ImportStatus.COMMITTED) return summary(batch);
        if (batch.getStatus() == ImportStatus.EXPIRED || !batch.getExpiresAt().isAfter(clock.instant())) {
            throw new ApiException(HttpStatus.GONE, "This import preview has expired. Upload a fresh CSV.", List.of());
        }
        if (batch.getInvalidCount() > 0) {
            throw ApiException.invalid("batch", "Correct the invalid rows and upload the CSV again before committing.");
        }

        List<ImportRow> rows = importRows.findAllByBatchIdOrderByRowNumberAsc(id);
        List<ImportRow> existingRows = rows.stream().filter(row -> row.getEmployee() != null)
                .sorted(Comparator.comparing(ImportRow::getEmployeeCode)).toList();
        Map<UUID, Employee> lockedEmployees = new LinkedHashMap<>();
        for (ImportRow row : existingRows) {
            UUID employeeId = row.getEmployee().getId();
            if (!lockedEmployees.containsKey(employeeId)) {
                Employee locked = employees.findLockedById(employeeId)
                        .orElseThrow(() -> ApiException.conflict("An employee changed since preview. Upload a fresh CSV."));
                lockedEmployees.put(employeeId, locked);
            }
        }

        LocalDate today = LocalDate.now(clock);
        for (ImportRow row : rows) {
            if (row.getAction() == ImportAction.CREATE) {
                if (employees.existsByEmployeeCode(row.getEmployeeCode())) {
                    throw ApiException.conflict("An employee code was created after preview. Upload a fresh CSV.");
                }
            } else {
                Employee employee = lockedEmployees.get(row.getEmployee().getId());
                if (!Objects.equals(employee.getVersion(), row.getEmployeeVersion())) {
                    throw ApiException.conflict("An employee changed since preview. Upload a fresh CSV.");
                }
                SalaryRecord latest = salaries.findCurrentForEmployee(employee.getId(), today).orElse(null);
                if (latest == null || row.getLatestSalaryRecord() == null
                        || !latest.getId().equals(row.getLatestSalaryRecord().getId())
                        || !latest.getVersion().equals(row.getLatestSalaryVersion())) {
                    throw ApiException.conflict("An employee's salary changed since preview. Upload a fresh CSV.");
                }
            }
        }

        Instant now = clock.instant();
        for (ImportRow row : rows) {
            if (row.getAction() == ImportAction.NO_OP) continue;
            Employee employee;
            if (row.getAction() == ImportAction.CREATE) {
                employee = new Employee(UUID.randomUUID(), row.getEmployeeCode(), row.getFullName(),
                        row.getCountryCode(), row.getDepartment(), row.getJobTitle(),
                        EmployeeStatus.valueOf(row.getEmployeeStatus()), now);
                employee = employees.saveAndFlush(employee);
            } else {
                employee = lockedEmployees.get(row.getEmployee().getId());
                if (row.isProfileChanged()) {
                    employee.updateProfile(row.getFullName(), row.getCountryCode(), row.getDepartment(),
                            row.getJobTitle(), EmployeeStatus.valueOf(row.getEmployeeStatus()), now);
                    employees.saveAndFlush(employee);
                }
            }
            if (row.isSalaryRecordProposed()) {
                SalaryRecord salary = salaries.saveAndFlush(new SalaryRecord(UUID.randomUUID(), employee,
                        new java.math.BigDecimal(row.getAnnualBaseAmount()), row.getCurrencyCode(),
                        LocalDate.parse(row.getEffectiveDate()), now));
                auditEvents.save(AuditEvent.csvSalaryCreated(salary, ownerEmail, batch.getId(), now));
            }
        }
        batch.commit(now);
        batches.saveAndFlush(batch);
        return summary(batch);
    }

    @Transactional
    public void cleanup() {
        Instant now = clock.instant();
        batches.expirePreviews(now);
        importRows.deleteStagedRowsBefore(now.minusSeconds(24 * 60 * 60));
    }

    private Map<String, Integer> headerIndex(List<String> header) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String name = header.get(i).trim();
            if (!HEADERS.contains(name) || columns.putIfAbsent(name, i) != null) {
                throw ApiException.invalid("file", "CSV header must contain each required column exactly once and no other columns.");
            }
        }
        if (!columns.keySet().containsAll(HEADERS)) {
            throw ApiException.invalid("file", "CSV header is missing one or more required columns.");
        }
        return columns;
    }

    private DraftRow validateRow(int number, List<String> cells, Map<String, Integer> columns) {
        DraftRow row = new DraftRow(number);
        if (cells.size() != HEADERS.size()) row.error("row", "Expected exactly nine columns in this CSV record.");
        row.rawEmployeeCode = cell(cells, columns, "employee_code").trim();
        row.rawAmount = cell(cells, columns, "annual_base_amount").trim();
        row.rawEffectiveDate = cell(cells, columns, "effective_date").trim();
        row.employeeCode = validate(row, "employee_code", () -> employeeCode(row.rawEmployeeCode));
        row.fullName = validate(row, "full_name", () -> required("full_name", cell(cells, columns, "full_name"), 200));
        row.countryCode = validate(row, "country_code", () -> salaryRules.countryCode(cell(cells, columns, "country_code")));
        row.department = validate(row, "department", () -> required("department", cell(cells, columns, "department"), 120));
        row.jobTitle = validate(row, "job_title", () -> required("job_title", cell(cells, columns, "job_title"), 120));
        row.status = validate(row, "status", () -> status(cell(cells, columns, "status")));
        row.amount = validate(row, "annual_base_amount", () -> salaryRules.amount(row.rawAmount));
        row.currencyCode = validate(row, "currency_code", () -> salaryRules.currencyCode(cell(cells, columns, "currency_code")));
        LocalDate date = validate(row, "effective_date", () -> salaryRules.effectiveDate(row.rawEffectiveDate));
        row.effectiveDate = date;
        if (row.employeeCode == null) row.employeeCode = row.rawEmployeeCode.toUpperCase(Locale.ROOT);
        if (row.fullName == null) row.fullName = cell(cells, columns, "full_name").trim();
        if (row.countryCode == null) row.countryCode = cell(cells, columns, "country_code").trim();
        if (row.department == null) row.department = cell(cells, columns, "department").trim();
        if (row.jobTitle == null) row.jobTitle = cell(cells, columns, "job_title").trim();
        if (row.status == null) row.status = cell(cells, columns, "status").trim().toUpperCase(Locale.ROOT);
        if (row.currencyCode == null) row.currencyCode = cell(cells, columns, "currency_code").trim().toUpperCase(Locale.ROOT);
        return row;
    }

    private void classify(DraftRow draft, Employee employee, SalaryRecord latest) {
        if (employee == null) {
            draft.action = ImportAction.CREATE;
            draft.salaryRecordProposed = true;
            return;
        }
        if (latest == null) {
            draft.error("annual_base_amount", "Employee has no current salary record to compare.");
            draft.action = ImportAction.INVALID;
            return;
        }
        draft.profileChanged = !employee.getFullName().equals(draft.fullName)
                || !employee.getCountryCode().equals(draft.countryCode)
                || !employee.getDepartment().equals(draft.department)
                || !employee.getJobTitle().equals(draft.jobTitle)
                || employee.getStatus() != EmployeeStatus.valueOf(draft.status);
        int dateOrder = draft.effectiveDate.compareTo(latest.getEffectiveDate());
        boolean amountOrCurrencyChanged = draft.amount.compareTo(latest.getAnnualBaseAmount()) != 0
                || !draft.currencyCode.equals(latest.getCurrencyCode());
        if (dateOrder < 0) {
            draft.error("effective_date", "For an existing employee, use the latest salary date or a later date.");
        } else if (dateOrder == 0 && amountOrCurrencyChanged) {
            draft.error("effective_date", "Same-date salary changes must use the salary correction API.");
        } else if (dateOrder > 0) {
            draft.salaryRecordProposed = true;
        }
        if (!draft.errors.isEmpty()) draft.action = ImportAction.INVALID;
        else if (draft.profileChanged || draft.salaryRecordProposed) draft.action = ImportAction.UPDATE;
        else draft.action = ImportAction.NO_OP;
    }

    private void markFileDuplicates(List<DraftRow> drafts) {
        Map<String, List<DraftRow>> byCode = drafts.stream().filter(row -> row.employeeCode != null)
                .collect(Collectors.groupingBy(row -> row.employeeCode, LinkedHashMap::new, Collectors.toList()));
        byCode.values().stream().filter(group -> group.size() > 1)
                .forEach(group -> group.forEach(row -> row.error("employee_code", "Employee code appears more than once in this file.")));
        drafts.stream().filter(row -> !row.errors.isEmpty()).forEach(row -> row.action = ImportAction.INVALID);
    }

    private <T> T validate(DraftRow row, String field, SupplierWithValidation<T> validation) {
        try {
            return validation.get();
        } catch (ApiException exception) {
            String message = exception.errors().stream().findFirst().map(ApiFieldError::message)
                    .orElse("Value is invalid.");
            row.error(field, message);
            return null;
        }
    }

    private String employeeCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (code.isBlank() || code.length() > 64) throw ApiException.invalid("employeeCode", "Employee code is required and must be at most 64 characters.");
        return code;
    }

    private String required(String field, String raw, int maxLength) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || value.length() > maxLength) {
            throw ApiException.invalid(field, "Value is required and must be at most " + maxLength + " characters.");
        }
        return value;
    }

    private String status(String raw) {
        try {
            return EmployeeStatus.valueOf(raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw ApiException.invalid("status", "Status must be ACTIVE or INACTIVE.");
        }
    }

    private String cell(List<String> cells, Map<String, Integer> columns, String name) {
        int index = columns.get(name);
        return index < cells.size() ? cells.get(index) : "";
    }

    private ImportSummary summary(ImportBatch batch) {
        return new ImportSummary(batch.getId(), batch.getStatus(), batch.getCreatedAt(), batch.getExpiresAt(),
                batch.getCommittedAt(), batch.getTotalRowCount(), batch.getCreateCount(), batch.getUpdateCount(),
                batch.getNoOpCount(), batch.getInvalidCount());
    }

    private ImportRowResponse rowResponse(ImportRow row) {
        try {
            JsonNode errors = objectMapper.readTree(row.getErrorsJson());
            List<ImportError> errorList = new ArrayList<>();
            for (JsonNode error : errors) {
                errorList.add(new ImportError(error.path("field").stringValue(), error.path("message").stringValue()));
            }
            return new ImportRowResponse(row.getRowNumber(), row.getEmployeeCode(), row.getAction(), row.getFullName(),
                    row.getCountryCode(), row.getDepartment(), row.getJobTitle(), row.getEmployeeStatus(),
                    row.getAnnualBaseAmount(), row.getCurrencyCode(), row.getEffectiveDate(), errorList);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored import row errors could not be read.", exception);
        }
    }

    private String serializeErrors(List<ImportError> errors) {
        try {
            return objectMapper.writeValueAsString(errors);
        } catch (Exception exception) {
            throw new IllegalStateException("Import row errors could not be serialized.", exception);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    @FunctionalInterface
    private interface SupplierWithValidation<T> { T get(); }

    private static final class DraftRow {
        private final int rowNumber;
        private final List<ImportError> errors = new ArrayList<>();
        private String rawEmployeeCode;
        private String rawAmount;
        private String rawEffectiveDate;
        private String employeeCode;
        private String fullName;
        private String countryCode;
        private String department;
        private String jobTitle;
        private String status;
        private java.math.BigDecimal amount;
        private String currencyCode;
        private LocalDate effectiveDate;
        private ImportAction action;
        private boolean profileChanged;
        private boolean salaryRecordProposed;

        private DraftRow(int rowNumber) { this.rowNumber = rowNumber; }
        private void error(String field, String message) { errors.add(new ImportError(field, message)); }
    }
}
