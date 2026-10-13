package com.incubyte.salarymanagement.imports;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ImportBatch b where b.id = :id and b.ownerEmail = :owner")
    Optional<ImportBatch> findLockedByIdAndOwner(@Param("id") UUID id, @Param("owner") String owner);

    Optional<ImportBatch> findByIdAndOwnerEmail(UUID id, String ownerEmail);

    @Modifying
    @Query(value = "update import_batch set status = 'EXPIRED' where status = 'PREVIEWED' and expires_at <= :now",
            nativeQuery = true)
    int expirePreviews(@Param("now") Instant now);
}
