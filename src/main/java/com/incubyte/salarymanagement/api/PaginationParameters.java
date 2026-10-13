package com.incubyte.salarymanagement.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record PaginationParameters(int page, int size) {
    public static final int DEFAULT_SIZE = 25;
    public static final int MAX_SIZE = 100;

    public PaginationParameters {
        if (page < 1) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "page must be at least 1.");
        if (size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "size must be between 1 and 100.");
        }
    }

    public static PaginationParameters defaults() {
        return new PaginationParameters(1, DEFAULT_SIZE);
    }

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page - 1, size, sort);
    }
}
