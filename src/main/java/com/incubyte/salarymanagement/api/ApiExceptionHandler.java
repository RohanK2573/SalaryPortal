package com.incubyte.salarymanagement.api;

import java.util.List;
import com.incubyte.salarymanagement.api.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class})
    ResponseEntity<ApiProblem> validationFailure(Exception exception) {
        List<ApiFieldError> errors = exception instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream()
                    .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage())).toList()
                : List.of();
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "The request contains invalid values.", errors);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiProblem> knownHttpFailure(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String detail = status == HttpStatus.UNPROCESSABLE_ENTITY
                ? "The request contains invalid values."
                : "The request could not be completed.";
        return problem(status, detail, List.of());
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiProblem> apiFailure(ApiException exception) {
        return problem(exception.status(), exception.getMessage(), exception.errors());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ApiProblem> persistenceConflict(Exception exception) {
        return problem(HttpStatus.CONFLICT, "The data changed or conflicts with an existing record. Retry after refreshing.", List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ApiProblem> malformedRequest(Exception exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "The request contains invalid values.", List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiProblem> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the 10 MiB limit.", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> unexpectedFailure(Exception exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", List.of());
    }

    private ResponseEntity<ApiProblem> problem(HttpStatus status, String detail, List<ApiFieldError> errors) {
        var body = new ApiProblem("about:blank", status.getReasonPhrase(), status.value(), detail, errors);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
