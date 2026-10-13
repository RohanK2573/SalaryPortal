package com.incubyte.salarymanagement.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record ApiPage<T>(List<T> items, int page, int size, long total) {
    public ApiPage {
        items = List.copyOf(items);
    }

    public static <T> ApiPage<T> from(Page<T> result) {
        return new ApiPage<>(result.getContent(), result.getNumber() + 1, result.getSize(), result.getTotalElements());
    }
}
