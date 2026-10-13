package com.incubyte.salarymanagement.imports;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportRowRepository extends JpaRepository<ImportRow, UUID> {
    Page<ImportRow> findAllByBatchIdOrderByRowNumberAsc(UUID batchId, Pageable pageable);
    List<ImportRow> findAllByBatchIdOrderByRowNumberAsc(UUID batchId);

    @Modifying
    @Query(value = "delete from import_row r using import_batch b " +
            "where r.batch_id = b.id and b.created_at <= :cutoff", nativeQuery = true)
    int deleteStagedRowsBefore(@Param("cutoff") Instant cutoff);
}
