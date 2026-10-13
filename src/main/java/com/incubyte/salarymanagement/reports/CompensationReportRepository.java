package com.incubyte.salarymanagement.reports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class CompensationReportRepository {
    @PersistenceContext
    private EntityManager entityManager;

    List<CompensationReportResponse.Group> aggregate(ReportGrouping grouping, String countryCode,
            String department, java.time.LocalDate asOf) {
        String groupingColumns = "country_code" + grouping.additionalColumn();
        String where = "";
        if (countryCode != null) where += " AND country_code = :countryCode";
        if (department != null) where += " AND lower(department) = :department";

        String sql = """
                WITH current_salary AS (
                    SELECT DISTINCT ON (s.employee_id)
                        e.country_code, e.department, e.job_title, s.employee_id,
                        s.annual_base_amount, s.currency_code
                    FROM employee e
                    JOIN salary_record s ON s.employee_id = e.id
                    WHERE e.status = 'ACTIVE' AND s.effective_date <= :asOf
                    ORDER BY s.employee_id, s.effective_date DESC
                ), filtered_salary AS (
                    SELECT * FROM current_salary WHERE 1 = 1
                %s
                ), ranked_salary AS (
                    SELECT country_code, department, job_title, currency_code, annual_base_amount,
                        row_number() OVER (PARTITION BY %s, currency_code ORDER BY annual_base_amount) AS salary_rank,
                        count(*) OVER (PARTITION BY %s, currency_code) AS group_size
                    FROM filtered_salary
                )
                SELECT %s, currency_code, count(*), min(annual_base_amount), max(annual_base_amount),
                    avg(annual_base_amount), sum(annual_base_amount),
                    avg(annual_base_amount) FILTER (
                        WHERE salary_rank IN ((group_size + 1) / 2, (group_size + 2) / 2)
                    )
                FROM ranked_salary
                GROUP BY %s, currency_code
                ORDER BY %s, currency_code
                """.formatted(where, groupingColumns, groupingColumns, groupingColumns,
                groupingColumns, groupingColumns);

        Query query = entityManager.createNativeQuery(sql).setParameter("asOf", asOf);
        if (countryCode != null) query.setParameter("countryCode", countryCode);
        if (department != null) query.setParameter("department", department);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<CompensationReportResponse.Group> groups = new ArrayList<>(rows.size());
        for (Object[] row : rows) groups.add(toGroup(row, grouping));
        return groups;
    }

    private CompensationReportResponse.Group toGroup(Object[] row, ReportGrouping grouping) {
        String countryCode = (String) row[0];
        String jobTitle = null;
        String department = null;
        int currencyIndex = 1;
        if (grouping == ReportGrouping.COUNTRY_JOB_TITLE) {
            jobTitle = (String) row[1];
            currencyIndex = 2;
        } else if (grouping == ReportGrouping.COUNTRY_DEPARTMENT) {
            department = (String) row[1];
            currencyIndex = 2;
        }
        int metricsIndex = currencyIndex + 1;
        return new CompensationReportResponse.Group(countryCode, jobTitle, department,
                (String) row[currencyIndex], ((Number) row[metricsIndex]).longValue(),
                decimal(row[metricsIndex + 1]), decimal(row[metricsIndex + 2]),
                decimal(row[metricsIndex + 3]), decimal(row[metricsIndex + 4]),
                decimal(row[metricsIndex + 5]));
    }

    private BigDecimal decimal(Object value) {
        return value == null ? null : (BigDecimal) value;
    }
}
