package com.incubyte.salarymanagement.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.incubyte.salarymanagement.api.ApiException;
import com.incubyte.salarymanagement.salaries.SalaryRules;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompensationReportServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    private final StubReports reports = new StubReports();

    private CompensationReportService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);
        service = new CompensationReportService(reports, new SalaryRules(clock), clock);
        reports.result = List.of();
    }

    @Test
    void returnsCountryAndJobTitleGroupsWithCurrencyAndRoundedAverage() {
        var group = new CompensationReportResponse.Group("IN", "Engineer", null, "INR", 2,
                new BigDecimal("40000.0000"), new BigDecimal("50000.0000"),
                new BigDecimal("45000.00005"), new BigDecimal("90000.0000"),
                new BigDecimal("45000.00005"));
        reports.result = List.of(group);

        CompensationReportResponse response = service.compensation("countryJobTitle", " in ", null);

        assertEquals(2, response.headcount());
        assertEquals(1, response.groups().size());
        assertEquals("IN", response.groups().getFirst().countryCode());
        assertEquals("Engineer", response.groups().getFirst().jobTitle());
        assertEquals("INR", response.groups().getFirst().currencyCode());
        assertEquals("45000.0001", response.groups().getFirst().average());
        assertEquals("45000.0001", response.groups().getFirst().median());
        assertEquals(ReportGrouping.COUNTRY_JOB_TITLE, reports.grouping);
        assertEquals("IN", reports.countryCode);
        assertEquals(TODAY, reports.asOf);
    }

    @Test
    void normalizesDepartmentFilterAndDefaultsGroupingToCountry() {
        CompensationReportResponse response = service.compensation(null, null, " Engineering ");

        assertEquals(0, response.headcount());
        assertEquals(List.of(), response.groups());
        assertEquals(ReportGrouping.COUNTRY, reports.grouping);
        assertEquals("engineering", reports.department);
    }

    @Test
    void rejectsUnsupportedGroupingAndInvalidCountryFilter() {
        assertThrows(ApiException.class, () -> service.compensation("salary", null, null));
        assertThrows(ApiException.class, () -> service.compensation("country", "XX", null));
    }

    private static final class StubReports extends CompensationReportRepository {
        private List<CompensationReportResponse.Group> result = List.of();
        private ReportGrouping grouping;
        private String countryCode;
        private String department;
        private LocalDate asOf;

        @Override
        List<CompensationReportResponse.Group> aggregate(ReportGrouping grouping, String countryCode,
                String department, LocalDate asOf) {
            this.grouping = grouping;
            this.countryCode = countryCode;
            this.department = department;
            this.asOf = asOf;
            return result;
        }
    }
}
