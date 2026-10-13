package com.incubyte.salarymanagement.reports;

import com.incubyte.salarymanagement.salaries.SalaryRules;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompensationReportService {
    private final CompensationReportRepository reports;
    private final SalaryRules salaryRules;
    private final Clock clock;

    public CompensationReportService(CompensationReportRepository reports, SalaryRules salaryRules, Clock clock) {
        this.reports = reports;
        this.salaryRules = salaryRules;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CompensationReportResponse compensation(String groupBy, String country, String department) {
        ReportGrouping grouping = ReportGrouping.parse(groupBy);
        String countryFilter = country == null || country.isBlank() ? null : salaryRules.countryCode(country);
        String departmentFilter = ReportGrouping.normalizeDepartmentFilter(department);
        var groups = reports.aggregate(grouping, countryFilter, departmentFilter, LocalDate.now(clock));
        long headcount = groups.stream().mapToLong(CompensationReportResponse.Group::headcount).sum();
        return new CompensationReportResponse(headcount, groups);
    }
}
