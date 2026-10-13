package com.incubyte.salarymanagement.salaries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incubyte.salarymanagement.api.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SalaryRulesTest {
    private final SalaryRules rules = new SalaryRules(
            Clock.fixed(Instant.parse("2026-10-13T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void parsesMoneyExactlyWithinNumericPrecision() {
        assertThat(rules.amount("120000.0123")).isEqualByComparingTo("120000.0123");
        assertThatThrownBy(() -> rules.amount("120000.01234"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> rules.amount("1000000000000000"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> rules.amount("1e3"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void normalizesAndChecksIsoCodes() {
        assertThat(rules.countryCode(" us ")).isEqualTo("US");
        assertThat(rules.currencyCode(" usd ")).isEqualTo("USD");
        assertThatThrownBy(() -> rules.countryCode("ZZ")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> rules.currencyCode("XXX")).isInstanceOf(ApiException.class);
    }

    @Test
    void allowsTodayButRejectsFutureDatesUsingTheInjectedUtcClock() {
        assertThat(rules.effectiveDate("2026-10-13")).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThatThrownBy(() -> rules.effectiveDate("2026-10-14"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> rules.effectiveDate("13-10-2026"))
                .isInstanceOf(ApiException.class);
    }
}
