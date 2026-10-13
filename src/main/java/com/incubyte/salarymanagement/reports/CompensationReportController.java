package com.incubyte.salarymanagement.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class CompensationReportController {
    private final CompensationReportService reports;

    public CompensationReportController(CompensationReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/compensation")
    public CompensationReportResponse compensation(
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department) {
        return reports.compensation(groupBy, countryCode, department);
    }
}
