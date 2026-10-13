package com.incubyte.salarymanagement.employees;

import com.incubyte.salarymanagement.api.ApiPage;
import com.incubyte.salarymanagement.employees.EmployeeApiModels.CreateEmployeeRequest;
import com.incubyte.salarymanagement.employees.EmployeeApiModels.EmployeeResponse;
import com.incubyte.salarymanagement.employees.EmployeeApiModels.SalaryResponse;
import com.incubyte.salarymanagement.salaries.SalaryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {
    private final EmployeeService employees;
    private final SalaryService salaries;

    public EmployeeController(EmployeeService employees, SalaryService salaries) {
        this.employees = employees;
        this.salaries = salaries;
    }

    @GetMapping
    public ApiPage<EmployeeResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String sort) {
        return employees.search(q, countryCode, department, status, page, size, sort);
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable UUID id) {
        return employees.get(id);
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        EmployeeResponse result = employees.create(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/employees/" + result.id())).body(result);
    }

    @PatchMapping(path = "/{id}", consumes = "application/json")
    public EmployeeResponse patch(@PathVariable UUID id, @RequestBody JsonNode request,
            Authentication authentication) {
        return employees.patch(id, request, authentication.getName());
    }

    @GetMapping("/{id}/salary-records")
    public ApiPage<SalaryResponse> salaryHistory(@PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size) {
        return salaries.history(id, page, size);
    }

    @PostMapping(path = "/{id}/salary-records", consumes = "application/json")
    public ResponseEntity<SalaryResponse> addSalary(@PathVariable UUID id,
            @RequestBody CreateSalaryRequest request, Authentication authentication) {
        SalaryResponse result = salaries.create(id, request.annualBaseAmount(), request.currencyCode(),
                request.effectiveDate(), authentication.getName());
        URI location = URI.create("/api/v1/employees/" + id + "/salary-records/" + result.id());
        return ResponseEntity.created(location).body(result);
    }

    @PatchMapping(path = "/{id}/salary-records/{recordId}", consumes = "application/json")
    public SalaryResponse correctSalary(@PathVariable UUID id, @PathVariable UUID recordId,
            @RequestBody JsonNode request, Authentication authentication) {
        return salaries.correct(id, recordId, request, authentication.getName());
    }

    public record CreateSalaryRequest(String annualBaseAmount, String currencyCode, String effectiveDate) {}
}
