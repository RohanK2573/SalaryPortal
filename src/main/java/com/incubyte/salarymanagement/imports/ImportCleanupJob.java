package com.incubyte.salarymanagement.imports;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImportCleanupJob {
    private final ImportService imports;

    public ImportCleanupJob(ImportService imports) {
        this.imports = imports;
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanExpiredImports() {
        imports.cleanup();
    }
}
