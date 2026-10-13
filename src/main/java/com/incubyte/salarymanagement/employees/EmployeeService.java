package com.incubyte.salarymanagement.employees;

import com.incubyte.salarymanagement.api.ApiException;
import com.incubyte.salarymanagement.api.ApiPage;
import com.incubyte.salarymanagement.api.PaginationParameters;
import com.incubyte.salarymanagement.salaries.AuditEvent;
import com.incubyte.salarymanagement.salaries.AuditEventRepository;
import com.incubyte.salarymanagement.salaries.SalaryRecord;
import com.incubyte.salarymanagement.salaries.SalaryRecordRepository;
import com.incubyte.salarymanagement.salaries.SalaryRules;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.criteria.Predicate;

import static com.incubyte.salarymanagement.employees.EmployeeApiModels.CreateEmployeeRequest;
import static com.incubyte.salarymanagement.employees.EmployeeApiModels.EmployeeResponse;
import static com.incubyte.salarymanagement.employees.EmployeeApiModels.SalaryResponse;

@Service
public class EmployeeService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "employeeCode", "employeeCode", "fullName", "fullName", "countryCode", "countryCode",
            "department", "department", "jobTitle", "jobTitle", "status", "status");

    private final EmployeeRepository employees;
    private final SalaryRecordRepository salaryRecords;
    private final AuditEventRepository auditEvents;
    private final SalaryRules salaryRules;
    private final Clock clock;

    public EmployeeService(EmployeeRepository employees, SalaryRecordRepository salaryRecords,
            AuditEventRepository auditEvents, SalaryRules salaryRules, Clock clock) {
        this.employees = employees;
        this.salaryRecords = salaryRecords;
        this.auditEvents = auditEvents;
        this.salaryRules = salaryRules;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ApiPage<EmployeeResponse> search(String query, String country, String department, String status,
            int page, int size, String sort) {
        new PaginationParameters(page, size);
        Specification<Employee> specification = searchSpecification(query, country, department, status);
        Page<Employee> result = employees.findAll(specification,
                PageRequest.of(page - 1, size, parseSort(sort)));
        List<UUID> ids = result.getContent().stream().map(Employee::getId).toList();
        Map<UUID, SalaryRecord> currentByEmployee = ids.isEmpty() ? Map.of()
                : salaryRecords.findCurrentForEmployees(ids, LocalDate.now(clock)).stream()
                        .collect(Collectors.toMap(record -> record.getEmployee().getId(), Function.identity()));
        return ApiPage.from(result.map(employee -> response(employee, currentByEmployee.get(employee.getId()))));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(UUID id) {
        Employee employee = findEmployee(id);
        return response(employee, currentSalary(id));
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request, String actorEmail) {
        String code = employeeCode(request.employeeCode());
        String name = requiredText("fullName", request.fullName(), 200);
        String country = salaryRules.countryCode(request.countryCode());
        String department = requiredText("department", request.department(), 120);
        String jobTitle = requiredText("jobTitle", request.jobTitle(), 120);
        EmployeeStatus status = status(request.status());
        var amount = salaryRules.amount(request.annualBaseAmount());
        String currency = salaryRules.currencyCode(request.currencyCode());
        LocalDate effectiveDate = salaryRules.effectiveDate(request.effectiveDate());

        if (employees.existsByEmployeeCode(code)) {
            throw ApiException.conflict("An employee with this employee code already exists.");
        }
        Instant now = clock.instant();
        Employee employee = employees.saveAndFlush(new Employee(UUID.randomUUID(), code, name, country,
                department, jobTitle, status, now));
        SalaryRecord salary = salaryRecords.saveAndFlush(new SalaryRecord(UUID.randomUUID(), employee,
                amount, currency, effectiveDate, now));
        auditEvents.save(AuditEvent.salaryCreated(salary, actorEmail, now));
        return response(employee, salary);
    }

    @Transactional
    public EmployeeResponse patch(UUID id, JsonNode patch, String actorEmail) {
        if (patch == null || !patch.isObject()) throw ApiException.invalid("body", "Request body must be a JSON object.");
        for (String field : patch.propertyNames()) {
            if (!field.equals("version") && !List.of("fullName", "countryCode", "department", "jobTitle", "status").contains(field)) {
                throw ApiException.invalid(field, "This field cannot be changed.");
            }
        }
        JsonNode versionNode = patch.get("version");
        if (versionNode == null || !versionNode.isIntegralNumber() || versionNode.longValue() < 0) {
            throw ApiException.invalid("version", "A nonnegative employee version is required.");
        }
        if (patch.size() < 2) throw ApiException.invalid("body", "At least one profile field is required.");

        Employee employee = employees.findLockedById(id)
                .orElseThrow(() -> ApiException.notFound("Employee was not found."));
        if (employee.getVersion() != versionNode.longValue()) {
            throw ApiException.conflict("The employee changed since it was loaded. Refresh and retry.");
        }
        String name = patch.has("fullName") ? requiredText("fullName", patch.get("fullName"), 200) : employee.getFullName();
        String country = patch.has("countryCode") ? salaryRules.countryCode(requiredText("countryCode", patch.get("countryCode"), 2)) : employee.getCountryCode();
        String department = patch.has("department") ? requiredText("department", patch.get("department"), 120) : employee.getDepartment();
        String jobTitle = patch.has("jobTitle") ? requiredText("jobTitle", patch.get("jobTitle"), 120) : employee.getJobTitle();
        EmployeeStatus nextStatus = patch.has("status") ? status(requiredText("status", patch.get("status"), 16)) : employee.getStatus();

        boolean changed = !name.equals(employee.getFullName()) || !country.equals(employee.getCountryCode())
                || !department.equals(employee.getDepartment()) || !jobTitle.equals(employee.getJobTitle())
                || nextStatus != employee.getStatus();
        if (changed) {
            employee.updateProfile(name, country, department, jobTitle, nextStatus, clock.instant());
            employees.saveAndFlush(employee);
        }
        return response(employee, currentSalary(id));
    }

    private Specification<Employee> searchSpecification(String query, String country, String department, String status) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.length() > 100) throw ApiException.invalid("q", "Search text cannot exceed 100 characters.");
        String countryFilter = country == null || country.isBlank() ? null : salaryRules.countryCode(country);
        String departmentFilter = department == null || department.isBlank() ? null : department.trim().toLowerCase(Locale.ROOT);
        if (departmentFilter != null && departmentFilter.length() > 120) {
            throw ApiException.invalid("department", "Department filter cannot exceed 120 characters.");
        }
        EmployeeStatus statusFilter = status == null || status.isBlank() ? null : status(status);
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!normalizedQuery.isBlank()) {
                String escaped = normalizedQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("employeeCode")), pattern, '\\'),
                        builder.like(builder.lower(root.get("fullName")), pattern, '\\')));
            }
            if (countryFilter != null) predicates.add(builder.equal(root.get("countryCode"), countryFilter));
            if (departmentFilter != null) predicates.add(builder.equal(builder.lower(root.get("department")), departmentFilter));
            if (statusFilter != null) predicates.add(builder.equal(root.get("status"), statusFilter));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Sort parseSort(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) return Sort.by(Sort.Order.asc("employeeCode"));
        String[] parts = rawSort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.containsKey(parts[0])) {
            throw ApiException.invalid("sort", "Use an allowed sort field and direction, for example fullName,desc.");
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw ApiException.invalid("sort", "Sort direction must be asc or desc.");
        }
        Sort sort = Sort.by(direction, SORT_FIELDS.get(parts[0]));
        if (!parts[0].equals("employeeCode")) sort = sort.and(Sort.by(Sort.Order.asc("employeeCode")));
        return sort;
    }

    private Employee findEmployee(UUID id) {
        return employees.findById(id).orElseThrow(() -> ApiException.notFound("Employee was not found."));
    }

    private SalaryRecord currentSalary(UUID employeeId) {
        return salaryRecords.findCurrentForEmployee(employeeId, LocalDate.now(clock)).orElse(null);
    }

    static EmployeeResponse response(Employee employee, SalaryRecord currentSalary) {
        return new EmployeeResponse(employee.getId(), employee.getEmployeeCode(), employee.getFullName(),
                employee.getCountryCode(), employee.getDepartment(), employee.getJobTitle(), employee.getStatus(),
                employee.getVersion(), currentSalary == null ? null : salaryResponse(currentSalary));
    }

    public static SalaryResponse salaryResponse(SalaryRecord record) {
        return new SalaryResponse(record.getId(), record.getAnnualBaseAmount().toPlainString(),
                record.getCurrencyCode(), record.getEffectiveDate(), record.getVersion());
    }

    private String employeeCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (code.isBlank() || code.length() > 64) {
            throw ApiException.invalid("employeeCode", "Employee code is required and must be at most 64 characters.");
        }
        return code;
    }

    private String requiredText(String field, String raw, int maxLength) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank() || value.length() > maxLength) {
            throw ApiException.invalid(field, "Value is required and must be at most " + maxLength + " characters.");
        }
        return value;
    }

    private String requiredText(String field, JsonNode node, int maxLength) {
        if (node == null || !node.isString()) throw ApiException.invalid(field, "Value must be a non-null string.");
        return requiredText(field, node.stringValue(), maxLength);
    }

    private EmployeeStatus status(String raw) {
        try {
            return EmployeeStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw ApiException.invalid("status", "Status must be ACTIVE or INACTIVE.");
        }
    }
}
