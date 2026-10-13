package com.incubyte.salarymanagement.seed;

import com.incubyte.salarymanagement.employees.Employee;
import com.incubyte.salarymanagement.employees.EmployeeRepository;
import com.incubyte.salarymanagement.salaries.SalaryRecordRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataSeedService {
    private static final int EMPLOYEE_COUNT = 10_000;
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2020, 1, 1);
    private static final Instant CREATED_AT = Instant.parse("2020-01-01T00:00:00Z");
    private static final String[] COUNTRIES = {"IN", "US", "GB", "DE", "CA"};
    private static final String[] CURRENCIES = {"INR", "USD", "GBP", "EUR", "CAD"};
    private static final BigDecimal[] BASE_SALARIES = {
            new BigDecimal("900000"), new BigDecimal("85000"), new BigDecimal("65000"),
            new BigDecimal("70000"), new BigDecimal("80000")
    };
    private static final String[] DEPARTMENTS = {"Engineering", "People", "Finance", "Operations", "Sales"};
    private static final String[] JOB_TITLES = {
            "Software Engineer", "People Partner", "Financial Analyst", "Operations Specialist", "Account Executive"
    };
    private static final String[] FIRST_NAMES = {
            "Alex", "Avery", "Casey", "Jordan", "Morgan", "Quinn", "Riley", "Taylor", "Cameron", "Drew"
    };
    private static final String[] LAST_NAMES = {
            "Anderson", "Brown", "Chen", "Garcia", "Khan", "Miller", "Patel", "Singh", "Smith", "Wilson"
    };

    private final EmployeeRepository employees;
    private final SalaryRecordRepository salaries;
    private final JdbcTemplate jdbc;

    public DemoDataSeedService(EmployeeRepository employees, SalaryRecordRepository salaries, JdbcTemplate jdbc) {
        this.employees = employees;
        this.salaries = salaries;
        this.jdbc = jdbc;
    }

    /**
     * Inserts the deterministic demo dataset, returns zero when it already exists, and never overwrites data.
     */
    @Transactional
    public int seed() {
        List<SeedEmployee> dataset = dataset();
        List<String> codes = dataset.stream().map(SeedEmployee::employeeCode).toList();
        List<UUID> employeeIds = dataset.stream().map(SeedEmployee::employeeId).toList();
        List<UUID> salaryIds = dataset.stream().map(SeedEmployee::salaryId).toList();

        Map<String, Employee> byCode = new HashMap<>();
        employees.findAllByEmployeeCodeIn(codes).forEach(employee -> byCode.put(employee.getEmployeeCode(), employee));
        Map<UUID, Employee> byId = new HashMap<>();
        employees.findAllById(employeeIds).forEach(employee -> byId.put(employee.getId(), employee));
        var existingSalaries = salaries.findAllById(salaryIds);

        if (byCode.isEmpty() && byId.isEmpty() && existingSalaries.isEmpty()) {
            insert(dataset);
            return EMPLOYEE_COUNT;
        }

        if (byCode.size() == EMPLOYEE_COUNT && byId.size() == EMPLOYEE_COUNT
                && existingSalaries.size() == EMPLOYEE_COUNT && completeAndConsistent(dataset, byCode, byId, existingSalaries)) {
            return 0;
        }

        throw new IllegalStateException("Demo seed conflicts with existing employee data; no records were changed.");
    }

    private boolean completeAndConsistent(List<SeedEmployee> dataset, Map<String, Employee> byCode,
            Map<UUID, Employee> byId, List<com.incubyte.salarymanagement.salaries.SalaryRecord> existingSalaries) {
        Map<UUID, com.incubyte.salarymanagement.salaries.SalaryRecord> salaryById = new HashMap<>();
        existingSalaries.forEach(salary -> salaryById.put(salary.getId(), salary));

        for (SeedEmployee seed : dataset) {
            Employee codeMatch = byCode.get(seed.employeeCode());
            Employee idMatch = byId.get(seed.employeeId());
            var salary = salaryById.get(seed.salaryId());
            if (codeMatch == null || idMatch == null || !codeMatch.getId().equals(seed.employeeId())
                    || !seed.employeeCode().equals(idMatch.getEmployeeCode()) || salary == null
                    || !salary.getEmployee().getId().equals(seed.employeeId())) {
                return false;
            }
        }
        return true;
    }

    private void insert(List<SeedEmployee> dataset) {
        jdbc.batchUpdate("""
                INSERT INTO employee (id, employee_code, full_name, country_code, department, job_title,
                    status, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 0, ?, ?)
                """, dataset, 500, (PreparedStatement statement, SeedEmployee employee) -> {
            statement.setObject(1, employee.employeeId());
            statement.setString(2, employee.employeeCode());
            statement.setString(3, employee.fullName());
            statement.setString(4, employee.countryCode());
            statement.setString(5, employee.department());
            statement.setString(6, employee.jobTitle());
            statement.setTimestamp(7, Timestamp.from(CREATED_AT));
            statement.setTimestamp(8, Timestamp.from(CREATED_AT));
        });

        jdbc.batchUpdate("""
                INSERT INTO salary_record (id, employee_id, annual_base_amount, currency_code,
                    effective_date, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, ?)
                """, dataset, 500, (PreparedStatement statement, SeedEmployee employee) -> {
            statement.setObject(1, employee.salaryId());
            statement.setObject(2, employee.employeeId());
            statement.setBigDecimal(3, employee.salary());
            statement.setString(4, employee.currencyCode());
            statement.setDate(5, Date.valueOf(EFFECTIVE_DATE));
            statement.setTimestamp(6, Timestamp.from(CREATED_AT));
            statement.setTimestamp(7, Timestamp.from(CREATED_AT));
        });
    }

    private List<SeedEmployee> dataset() {
        List<SeedEmployee> result = new ArrayList<>(EMPLOYEE_COUNT);
        for (int index = 0; index < EMPLOYEE_COUNT; index++) {
            int countryIndex = index % COUNTRIES.length;
            int departmentIndex = (index / COUNTRIES.length) % DEPARTMENTS.length;
            String code = "DEMO-%05d".formatted(index + 1);
            BigDecimal salary = BASE_SALARIES[countryIndex]
                    .add(BASE_SALARIES[countryIndex].multiply(BigDecimal.valueOf((index % 20) * 5L))
                            .divide(BigDecimal.valueOf(100)));
            result.add(new SeedEmployee(
                    stableId("employee", code), code,
                    FIRST_NAMES[(index / 7) % FIRST_NAMES.length] + " " + LAST_NAMES[(index / 11) % LAST_NAMES.length],
                    COUNTRIES[countryIndex], DEPARTMENTS[departmentIndex], JOB_TITLES[departmentIndex],
                    stableId("salary", code), salary, CURRENCIES[countryIndex]));
        }
        return result;
    }

    private UUID stableId(String kind, String employeeCode) {
        return UUID.nameUUIDFromBytes(("salary-management:demo:" + kind + ":" + employeeCode)
                .getBytes(StandardCharsets.UTF_8));
    }

    private record SeedEmployee(UUID employeeId, String employeeCode, String fullName, String countryCode,
            String department, String jobTitle, UUID salaryId, BigDecimal salary, String currencyCode) {}
}
