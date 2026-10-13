package com.incubyte.salarymanagement.salaries;

import com.incubyte.salarymanagement.api.ApiException;
import com.incubyte.salarymanagement.api.ApiPage;
import com.incubyte.salarymanagement.api.PaginationParameters;
import com.incubyte.salarymanagement.employees.Employee;
import com.incubyte.salarymanagement.employees.EmployeeRepository;
import com.incubyte.salarymanagement.employees.EmployeeService;
import com.incubyte.salarymanagement.employees.EmployeeApiModels.SalaryResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class SalaryService {
    private static final List<String> PATCHABLE_FIELDS = List.of("annualBaseAmount", "currencyCode", "effectiveDate");

    private final EmployeeRepository employees;
    private final SalaryRecordRepository records;
    private final AuditEventRepository auditEvents;
    private final SalaryRules rules;
    private final Clock clock;

    public SalaryService(EmployeeRepository employees, SalaryRecordRepository records,
            AuditEventRepository auditEvents, SalaryRules rules, Clock clock) {
        this.employees = employees;
        this.records = records;
        this.auditEvents = auditEvents;
        this.rules = rules;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ApiPage<SalaryResponse> history(UUID employeeId, int page, int size) {
        new PaginationParameters(page, size);
        if (!employees.existsById(employeeId)) throw ApiException.notFound("Employee was not found.");
        Page<SalaryRecord> result = records.findByEmployee(employeeId,
                PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("effectiveDate"), Sort.Order.asc("id"))));
        return ApiPage.from(result.map(EmployeeService::salaryResponse));
    }

    @Transactional
    public SalaryResponse create(UUID employeeId, String rawAmount, String rawCurrency, String rawDate,
            String actorEmail) {
        Employee employee = employees.findLockedById(employeeId)
                .orElseThrow(() -> ApiException.notFound("Employee was not found."));
        var amount = rules.amount(rawAmount);
        String currency = rules.currencyCode(rawCurrency);
        LocalDate effectiveDate = rules.effectiveDate(rawDate);
        Instant now = clock.instant();

        SalaryRecord record = records.saveAndFlush(new SalaryRecord(UUID.randomUUID(), employee,
                amount, currency, effectiveDate, now));
        auditEvents.save(AuditEvent.salaryCreated(record, actorEmail, now));
        return EmployeeService.salaryResponse(record);
    }

    @Transactional
    public SalaryResponse correct(UUID employeeId, UUID recordId, JsonNode patch, String actorEmail) {
        if (patch == null || !patch.isObject()) throw ApiException.invalid("body", "Request body must be a JSON object.");
        for (String field : patch.propertyNames()) {
            if (!field.equals("version") && !field.equals("reason") && !PATCHABLE_FIELDS.contains(field)) {
                throw ApiException.invalid(field, "This field cannot be changed.");
            }
        }
        JsonNode versionNode = patch.get("version");
        if (versionNode == null || !versionNode.isIntegralNumber() || versionNode.longValue() < 0) {
            throw ApiException.invalid("version", "A nonnegative salary record version is required.");
        }
        String reason = requiredText("reason", patch.get("reason"), 500);
        if (patch.size() < 3) throw ApiException.invalid("body", "At least one salary field is required.");

        employees.findLockedById(employeeId)
                .orElseThrow(() -> ApiException.notFound("Employee was not found."));
        SalaryRecord record = records.findForEmployee(employeeId, recordId)
                .orElseThrow(() -> ApiException.notFound("Salary record was not found for this employee."));
        if (record.getVersion() != versionNode.longValue()) {
            throw ApiException.conflict("The salary record changed since it was loaded. Refresh and retry.");
        }
        var amount = patch.has("annualBaseAmount")
                ? rules.amount(requiredText("annualBaseAmount", patch.get("annualBaseAmount"), 40))
                : record.getAnnualBaseAmount();
        String currency = patch.has("currencyCode")
                ? rules.currencyCode(requiredText("currencyCode", patch.get("currencyCode"), 3))
                : record.getCurrencyCode();
        LocalDate date = patch.has("effectiveDate")
                ? rules.effectiveDate(requiredText("effectiveDate", patch.get("effectiveDate"), 10))
                : record.getEffectiveDate();

        boolean changed = amount.compareTo(record.getAnnualBaseAmount()) != 0
                || !currency.equals(record.getCurrencyCode()) || !date.equals(record.getEffectiveDate());
        if (!changed) throw ApiException.invalid("body", "Correction must change at least one salary value.");
        if (!date.equals(record.getEffectiveDate())
                && records.existsForOtherRecordOnDate(employeeId, date, recordId)) {
            throw ApiException.conflict("Another salary record already uses this effective date.");
        }

        var beforeAmount = record.getAnnualBaseAmount();
        String beforeCurrency = record.getCurrencyCode();
        LocalDate beforeDate = record.getEffectiveDate();
        Instant now = clock.instant();
        record.correct(amount, currency, date, now);
        records.saveAndFlush(record);
        auditEvents.save(AuditEvent.salaryCorrected(record, actorEmail, reason,
                beforeAmount, beforeCurrency, beforeDate, now));
        return EmployeeService.salaryResponse(record);
    }

    private String requiredText(String field, JsonNode node, int maxLength) {
        if (node == null || !node.isString()) throw ApiException.invalid(field, "Value must be a non-null string.");
        String value = node.stringValue().trim();
        if (value.isBlank() || value.length() > maxLength) {
            throw ApiException.invalid(field, "Value is required and must be at most " + maxLength + " characters.");
        }
        return value;
    }
}
