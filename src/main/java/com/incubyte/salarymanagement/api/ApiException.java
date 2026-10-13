package com.incubyte.salarymanagement.api;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final List<ApiFieldError> errors;

    public ApiException(HttpStatus status, String detail, List<ApiFieldError> errors) {
        super(detail);
        this.status = status;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public HttpStatus status() { return status; }
    public List<ApiFieldError> errors() { return errors; }

    public static ApiException invalid(String field, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "The request contains invalid values.",
                List.of(new ApiFieldError(field, message)));
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, detail, List.of());
    }

    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, detail, List.of());
    }
}
