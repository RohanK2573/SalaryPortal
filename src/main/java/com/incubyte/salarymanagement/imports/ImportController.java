package com.incubyte.salarymanagement.imports;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.incubyte.salarymanagement.imports.ImportApiModels.*;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {
    private static final String TEMPLATE = "employee_code,full_name,country_code,department,job_title,status,"
            + "annual_base_amount,currency_code,effective_date\r\n";

    private final ImportService imports;

    public ImportController(ImportService imports) {
        this.imports = imports;
    }

    @GetMapping(value = "/template", produces = "text/csv")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee-salary-template.csv")
                .body(TEMPLATE.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportSummary> preview(@RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        ImportSummary result = imports.preview(file, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/imports/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    public ImportDetailsResponse get(@PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication authentication) {
        return imports.get(id, authentication.getName(), page, size);
    }

    @PostMapping("/{id}/commit")
    public ImportSummary commit(@PathVariable UUID id, Authentication authentication) {
        return imports.commit(id, authentication.getName());
    }
}
