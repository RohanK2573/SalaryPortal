package com.incubyte.salarymanagement.salaries;

import com.incubyte.salarymanagement.api.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SalaryRules {
    private final Clock clock;

    public SalaryRules(Clock clock) {
        this.clock = clock;
    }

    public BigDecimal amount(String raw) {
        if (raw == null || !raw.matches("\\d+(\\.\\d{1,4})?")) {
            throw ApiException.invalid("annualBaseAmount", "Use a positive plain decimal with up to four fractional digits.");
        }
        BigDecimal amount = new BigDecimal(raw);
        int integerDigits = amount.precision() - amount.scale();
        if (amount.signum() <= 0 || amount.scale() > 4 || integerDigits > 15) {
            throw ApiException.invalid("annualBaseAmount", "Amount must be positive and fit NUMERIC(19,4).");
        }
        return amount;
    }

    public String currencyCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (code.length() != 3) throw ApiException.invalid("currencyCode", "Use a valid ISO 4217 currency code.");
        try {
            Currency currency = Currency.getInstance(code);
            if (!currency.getCurrencyCode().equals(code) || code.equals("XXX")) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw ApiException.invalid("currencyCode", "Use a valid ISO 4217 currency code.");
        }
        return code;
    }

    public LocalDate effectiveDate(String raw) {
        try {
            LocalDate date = LocalDate.parse(raw);
            if (date.isAfter(LocalDate.now(clock))) {
                throw ApiException.invalid("effectiveDate", "Effective date cannot be in the future.");
            }
            return date;
        } catch (DateTimeParseException | NullPointerException exception) {
            throw ApiException.invalid("effectiveDate", "Use a valid date in YYYY-MM-DD format.");
        }
    }

    public String countryCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (code.length() != 2 || !Arrays.asList(Locale.getISOCountries()).contains(code)) {
            throw ApiException.invalid("countryCode", "Use a valid ISO 3166-1 alpha-2 country code.");
        }
        return code;
    }
}
