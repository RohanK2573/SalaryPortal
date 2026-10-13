package com.incubyte.salarymanagement.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_batch")
public class ImportBatch {
    @Id
    private UUID id;
    @Column(name = "owner_email", nullable = false, length = 254)
    private String ownerEmail;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImportStatus status;
    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "committed_at")
    private Instant committedAt;
    @Column(name = "total_row_count", nullable = false)
    private int totalRowCount;
    @Column(name = "create_count", nullable = false)
    private int createCount;
    @Column(name = "update_count", nullable = false)
    private int updateCount;
    @Column(name = "no_op_count", nullable = false)
    private int noOpCount;
    @Column(name = "invalid_count", nullable = false)
    private int invalidCount;
    @Version
    @Column(nullable = false)
    private Long version;

    protected ImportBatch() {}

    public ImportBatch(UUID id, String ownerEmail, String fileSha256, Instant createdAt, Instant expiresAt,
            int totalRowCount, int createCount, int updateCount, int noOpCount, int invalidCount) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.fileSha256 = fileSha256;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.totalRowCount = totalRowCount;
        this.createCount = createCount;
        this.updateCount = updateCount;
        this.noOpCount = noOpCount;
        this.invalidCount = invalidCount;
        this.status = ImportStatus.PREVIEWED;
    }

    public void commit(Instant now) {
        this.status = ImportStatus.COMMITTED;
        this.committedAt = now;
    }

    public void expire() {
        if (status == ImportStatus.PREVIEWED) status = ImportStatus.EXPIRED;
    }

    public UUID getId() { return id; }
    public String getOwnerEmail() { return ownerEmail; }
    public ImportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCommittedAt() { return committedAt; }
    public int getTotalRowCount() { return totalRowCount; }
    public int getCreateCount() { return createCount; }
    public int getUpdateCount() { return updateCount; }
    public int getNoOpCount() { return noOpCount; }
    public int getInvalidCount() { return invalidCount; }
}
