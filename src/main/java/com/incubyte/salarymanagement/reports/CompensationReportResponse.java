package com.incubyte.salarymanagement.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record CompensationReportResponse(long headcount, List<Group> groups) {
    public CompensationReportResponse {
        groups = List.copyOf(groups);
    }

    public record Group(String countryCode, String jobTitle, String department, String currencyCode,
            long headcount, String minimum, String maximum, String average, String total, String median) {
        public Group(String countryCode, String jobTitle, String department, String currencyCode,
                long headcount, BigDecimal minimum, BigDecimal maximum, BigDecimal average,
                BigDecimal total, BigDecimal median) {
            this(countryCode, jobTitle, department, currencyCode, headcount,
                    amount(minimum, false), amount(maximum, false), amount(average, true),
                    amount(total, false), amount(median, true));
        }

        private static String amount(BigDecimal value, boolean rounded) {
            if (value == null) return null;
            return (rounded ? value.setScale(4, RoundingMode.HALF_UP) : value).toPlainString();
        }
    }
}
