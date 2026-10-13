package com.incubyte.salarymanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

class PaginationParametersTest {
    @Test
    void defaultsToFirstPageAndTwentyFiveItems() {
        var parameters = PaginationParameters.defaults();
        assertThat(parameters.page()).isEqualTo(1);
        assertThat(parameters.size()).isEqualTo(25);
        assertThat(parameters.toPageable(Sort.by("employeeCode")).getPageNumber()).isZero();
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> new PaginationParameters(0, 25)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> new PaginationParameters(1, 101)).isInstanceOf(ResponseStatusException.class);
    }
}
