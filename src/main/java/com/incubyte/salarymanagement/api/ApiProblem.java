package com.incubyte.salarymanagement.api;

import java.util.List;

public record ApiProblem(String type, String title, int status, String detail, List<ApiFieldError> errors) {
    public ApiProblem {
        errors = errors == null || errors.isEmpty() ? null : List.copyOf(errors);
    }
}
