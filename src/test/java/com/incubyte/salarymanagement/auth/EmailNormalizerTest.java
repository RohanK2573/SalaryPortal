package com.incubyte.salarymanagement.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {
    @Test
    void trimsAndLowercasesEmailUsingLocaleIndependentRules() {
        assertThat(EmailNormalizer.normalize("  HR.User@Example.COM ")).isEqualTo("hr.user@example.com");
    }
}
